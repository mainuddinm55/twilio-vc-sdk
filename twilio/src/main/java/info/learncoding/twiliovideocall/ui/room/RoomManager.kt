package info.learncoding.twiliovideocall.ui.room

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import com.twilio.video.*
import com.twilio.video.ktx.createBandwidthProfileOptions
import com.twilio.video.ktx.createConnectOptions
import info.learncoding.twiliovideocall.data.model.CallOptions
import info.learncoding.twiliovideocall.data.network.ApiResponse
import info.learncoding.twiliovideocall.data.repository.VideoCallRepository
import info.learncoding.twiliovideocall.service.VideoCallService.Companion.stopService
import info.learncoding.twiliovideocall.ui.participant.*
import info.learncoding.twiliovideocall.TwilioSdk
import info.learncoding.twiliovideocall.data.model.UserType
import info.learncoding.twiliovideocall.utils.serializeToMap
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.*
import kotlin.collections.HashMap

const val MICROPHONE_TRACK_NAME = "microphone"
const val CAMERA_TRACK_NAME = "camera"
private const val TAG = "RoomManager"

class RoomManager constructor(
    private val context: Context,
    private val repository: VideoCallRepository,
    private val audioSwitch: AudioSwitch,
    private val participantManager: ParticipantManager,
    private var initialViewState: RoomViewState = RoomViewState(
        participantManager.primaryParticipant
    ),
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private val _isOngoingCall = MutableLiveData(false)
        val isOngoingCall: LiveData<Boolean> = _isOngoingCall

        private val _duration = MutableLiveData(0L)
        val duration: LiveData<Long> = _duration
    }

    private val roomListener = RoomListener()

    @VisibleForTesting(otherwise = PRIVATE)
    internal var roomScope = CoroutineScope(coroutineDispatcher)

    private val _callState: MutableLiveData<CallState> = MutableLiveData(CallState.Lobby)
    val callState: LiveData<CallState> = _callState

    private val _viewState = MutableLiveData(initialViewState)
    val viewState: LiveData<RoomViewState> = _viewState

    private val _isUiOnService = MutableLiveData<Boolean>()
    val isUiOnService: LiveData<Boolean> = _isUiOnService

    @VisibleForTesting(otherwise = PRIVATE)
    internal var localParticipantManager = LocalParticipantManager(context, this)
    var room: Room? = null
    private var callOptions: CallOptions? = null

    //For call duration
    private val timeInterval = 1000L
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val runnable: Runnable = object : Runnable {
        override fun run() {
            val currentTime = duration.value ?: 0L
            _duration.postValue(currentTime + 1)
            handler.postDelayed(this, timeInterval)
        }
    }

    private fun startCountDownTimer() {
        handler.post(runnable)
    }

    private fun stopCountDownTimer() {
        handler.removeCallbacks(runnable)
    }

    init {
        // start audio switch, it will silently error if it has already been started
        audioSwitch.start { audioDevices, selectedAudioDevice ->
            updateRoomViewState(
                initialViewState.copy(
                    primaryParticipant = participantManager.primaryParticipant,
                    selectedDevice = selectedAudioDevice,
                    availableAudioDevices = audioDevices
                )
            )
        }
    }

    fun disconnect(isReject: Boolean = false, isMissedCall: Boolean = false) {
        room?.disconnect()
        releaseLocalTrack()
        participantManager.clearRemoteParticipants()
        updateParticipantViewState()
        sendCallState(CallState.Disconnected(isReject))
        stopCountDownTimer()
        Log.d(TAG, "disconnect: ")
        val type = if (isReject && !isMissedCall) {
            TwilioSdk.TYPE_REJECT
        } else if (isMissedCall && !isReject) {
            TwilioSdk.TYPE_MISSED_CALL
        } else {
            TwilioSdk.TYPE_END
        }
        broadcastCallback(type)
        _duration.postValue(0L)

        if (isReject && !isMissedCall) {
            broadcastCallingData(
                TwilioSdk.KEY_VIDEO_CALL_BUTTON_STATE,
                hashMapOf(Pair("action", "reject call"))
            )
        } else if (!isReject && isMissedCall) {
            broadcastCallingData(
                TwilioSdk.KEY_VIDEO_CALL_MISSED_CALL,
                hashMapOf(Pair("action", "missed call"))
            )
        } else {
            broadcastCallingData(
                TwilioSdk.KEY_VIDEO_CALL_BUTTON_STATE,
                hashMapOf(Pair("action", "end call clicked"))
            )
        }
    }

    suspend fun connect(callOptions: CallOptions) {
        this.callOptions = callOptions
        sendCallState(CallState.Connecting(callOptions))
        broadcastCallback(TwilioSdk.TYPE_CONNECTING)
        connectToRoom(callOptions.tokenUrl, callOptions.roomName)
        _isOngoingCall.postValue(true)

        if (callOptions.userType == UserType.RECEIVER) {
            broadcastCallingData(
                TwilioSdk.KEY_VIDEO_CALL_BUTTON_STATE,
                hashMapOf(Pair("action", "answered call"))
            )
        }
    }

    private suspend fun connectToRoom(url: String, roomName: String) {
        roomScope.launch {
            when (val token = repository.fetchToken(url)) {
                is ApiResponse.Success -> {
                    val connectOptions = buildConnectionOption(token = token.data, roomName)
                    try {
                        Video.connect(context, connectOptions, roomListener)
                    } catch (e: Exception) {
                        sendCallState(CallState.ConnectionFailed(e.message))
                        broadcastCallback(TwilioSdk.TYPE_FAILED, e.message)
                    }
                }
                is ApiResponse.Error -> {
                    sendCallState(CallState.ConnectionFailed(token.msg))
                    broadcastCallback(TwilioSdk.TYPE_FAILED, token.msg)
                }
            }
        }
    }

    fun updateLocalVideoTrack(videoTrack: LocalVideoTrack?, isFrontCamera: Boolean) {
        participantManager.updateLocalParticipantVideoTrack(
            videoTrack?.let { VideoTrackViewState(it) }, isFrontCamera
        )
        updateParticipantViewState()
        updateRoomViewState(initialViewState.copy(isVideoOff = videoTrack == null))
    }

    fun updateLocalAudioTrack(mute: Boolean) {
        updateRoomViewState(initialViewState.copy(isAudioMuted = mute))
    }

    fun updateParticipantVideoTrack(sid: String, videoTrackViewState: VideoTrackViewState?) {
        participantManager.updateParticipantVideoTrack(sid, videoTrackViewState)
        updateParticipantViewState()
    }

    fun updateParticipantNetworkQualityChanged(
        sid: String,
        networkQualityLevel: NetworkQualityLevel
    ) {
        participantManager.updateNetworkQuality(
            sid, networkQualityLevel
        )
        updateParticipantViewState()
        broadcastNetworkLevel(networkQualityLevel)
    }

    fun updateParticipantAudioTrack(sid: String, mute: Boolean) {
        participantManager.muteParticipant(sid, mute)
        updateParticipantViewState()
    }

    private fun sendCallState(callState: CallState) {
        Log.d(TAG, "sendCallState: $callState")
        _callState.postValue(callState)
    }

    private fun updateRoomViewState(viewState: RoomViewState) {
        this.initialViewState = viewState
        _viewState.postValue(viewState)
    }

    fun setupLocalTrack(permissionGranted: Boolean) {
        if (permissionGranted) {
            localParticipantManager.setupLocalTrack()
        }
        updateRoomViewState(
            initialViewState.copy(
                isMicEnabled = permissionGranted,
                isCameraEnabled = permissionGranted
            )
        )
    }

    private fun releaseLocalTrack() {
        localParticipantManager.releaseLocalTrack()
    }

    fun toggleLocalVideo() {
        localParticipantManager.toggleLocalVideo()
        broadcastCallingData(
            TwilioSdk.KEY_VIDEO_CALL_BUTTON_STATE,
            hashMapOf(Pair("action", "click on switch video"))
        )
    }

    fun toggleLocalAudio() {
        localParticipantManager.toggleLocalAudio()
        broadcastCallingData(
            TwilioSdk.KEY_VIDEO_CALL_BUTTON_STATE,
            hashMapOf(Pair("action", "click on switch audio"))
        )
    }

    fun switchCamera() {
        localParticipantManager.switchCamera()
        broadcastCallingData(
            TwilioSdk.KEY_VIDEO_CALL_BUTTON_STATE,
            hashMapOf(Pair("action", "click on switch camera"))
        )
    }

    fun audioActivate() {
        audioSwitch.activate()
    }

    fun audioDeactivate() {
        audioSwitch.deactivate()
    }

    fun selectDevice(device: AudioDevice) {
        audioSwitch.selectDevice(device)
    }

    private fun buildConnectionOption(token: String, roomName: String): ConnectOptions {
        val bandwidthProfileOptions = createBandwidthProfileOptions {
            mode(BandwidthProfileMode.COLLABORATION)
            maxSubscriptionBitrate(2400)
            dominantSpeakerPriority(TrackPriority.STANDARD)
            trackSwitchOffMode(TrackSwitchOffMode.DETECTED)
            ClientTrackSwitchOffControl.AUTO
            VideoContentPreferencesMode.AUTO
        }
        val preferedVideoCodec = Vp8Codec()

        val preferredAudioCodec = OpusCodec()

        return createConnectOptions(token) {
            roomName(roomName)
            enableInsights(true)
            enableAutomaticSubscription(true)
            enableDominantSpeaker(true)
            enableNetworkQuality(true)
            networkQualityConfiguration(
                NetworkQualityConfiguration(
                    NetworkQualityVerbosity.NETWORK_QUALITY_VERBOSITY_MINIMAL,
                    NetworkQualityVerbosity.NETWORK_QUALITY_VERBOSITY_MINIMAL
                )
            )
            bandwidthProfile(bandwidthProfileOptions)
            encodingParameters(EncodingParameters(16, 0))
            preferVideoCodecs(listOf(preferedVideoCodec))
            preferAudioCodecs(listOf(preferredAudioCodec))
        }
    }

    fun setRinging() {
        sendCallState(CallState.Ringing(callOptions))
        broadcastCallback(TwilioSdk.TYPE_RINGING)
    }

    fun setIncoming(callOptions: CallOptions) {
        this.callOptions = callOptions
        sendCallState(CallState.Incoming(callOptions))
        _isOngoingCall.postValue(true)
        broadcastCallback(TwilioSdk.TYPE_INCOMING)
    }

    inner class RoomListener : Room.Listener {
        override fun onConnected(room: Room) {
            Log.i(TAG, "onConnected -> room sid: " + room.sid)
            setupParticipants(room)
            this@RoomManager.room = room

            hashMapOf<String, String>().apply {
                put("room", room.name)
                if (callOptions?.userType == UserType.RECEIVER) {
                    put("receiving_time", System.currentTimeMillis().toString())
                    broadcastCallingData(TwilioSdk.KEY_VIDEO_CALL_RECEIVING_TIME, this)
                } else if (callOptions?.userType == UserType.CALLER) {
                    put("calling_time", System.currentTimeMillis().toString())
                    broadcastCallingData(TwilioSdk.KEY_VIDEO_CALL_CALLING_TIME, this)
                }
            }
        }

        override fun onDisconnected(room: Room, twilioException: TwilioException?) {
            Log.i(
                TAG, "Disconnected from room -> sid: ${room.sid}, state: ${room.state}"
            )

            stopService(context)

            sendCallState(CallState.Disconnected(false))

            localParticipantManager.localParticipant = null
            if (twilioException != null) {
                val exceptionInfo = hashMapOf<String, String>()
                exceptionInfo["code"] = twilioException.code.toString()
                exceptionInfo["message"] = twilioException.explanation.toString()
                exceptionInfo["method"] = "onDisconnected"
                broadcastCallingData(TwilioSdk.KEY_VIDEO_CALL_TWILIO_EXCEPTION, exceptionInfo)
            }
        }

        override fun onConnectFailure(room: Room, twilioException: TwilioException) {
            Log.e(
                TAG,
                "Failed to connect to room -> sid: ${room.sid}, state: ${room.state}, code: ${twilioException.code}, error: ${twilioException.message}"
            )
            sendCallState(
                CallState.ConnectionFailed(
                    twilioException.explanation ?: "Something went wrong"
                )
            )
            broadcastCallback(TwilioSdk.TYPE_FAILED, twilioException.explanation)
            val exceptionInfo = hashMapOf<String, String>()
            exceptionInfo["code"] = twilioException.code.toString()
            exceptionInfo["message"] = twilioException.explanation.toString()
            exceptionInfo["method"] = "onConnectFailure"
            broadcastCallingData(TwilioSdk.KEY_VIDEO_CALL_TWILIO_EXCEPTION, exceptionInfo)
        }

        override fun onParticipantConnected(room: Room, remoteParticipant: RemoteParticipant) {
            Log.i(
                TAG,
                "RemoteParticipant connected -> room sid: ${room.sid}, remoteParticipant: ${remoteParticipant.sid}"
            )
            setupParticipants(room)
        }

        override fun onParticipantDisconnected(room: Room, remoteParticipant: RemoteParticipant) {
            Log.i(
                TAG,
                "RemoteParticipant disconnected -> room sid: ${room.sid}, remoteParticipant: ${remoteParticipant.sid}"
            )

            participantManager.removeParticipant(remoteParticipant.sid)
            updateParticipantViewState()
            if (room.remoteParticipants.isEmpty()) {
                disconnect()
            }
        }

        override fun onDominantSpeakerChanged(room: Room, remoteParticipant: RemoteParticipant?) {
            Log.i(
                TAG,
                "DominantSpeakerChanged -> room sid: ${room.sid}, remoteParticipant: ${remoteParticipant?.sid}"
            )
            hashMapOf<String, String>().apply {
                put("speaker", remoteParticipant?.identity ?: "")
                put("room", room.name)
            }.also {
                broadcastCallingData(TwilioSdk.KEY_VIDEO_CALL_DOMINANT_SPEAKER, it)
            }
        }

        override fun onRecordingStarted(room: Room) {

        }

        override fun onRecordingStopped(room: Room) {

        }

        override fun onReconnected(room: Room) {
            Log.i(TAG, "onReconnected: " + room.name)
            sendCallState(CallState.Connected(room, callOptions))
            broadcastCallback(TwilioSdk.TYPE_RECONNECTED)
        }

        override fun onReconnecting(room: Room, twilioException: TwilioException) {
            Log.i(TAG, "onReconnecting: " + room.name)
            sendCallState(CallState.Reconnecting(callOptions))
            broadcastCallback(TwilioSdk.TYPE_RECONNECTING)
            val dropInfo = hashMapOf<String, String>()
            dropInfo["is_drop"] = "true"
            dropInfo["message"] = twilioException.explanation.toString()
            dropInfo["code"] = twilioException.getCode().toString()
            broadcastCallingData(TwilioSdk.KEY_VIDEO_CALL_DROP, dropInfo)

            val exceptionInfo = hashMapOf<String, String>()
            exceptionInfo["code"] = twilioException.code.toString()
            exceptionInfo["message"] = twilioException.explanation.toString()
            exceptionInfo["method"] = "onReconnecting"
            broadcastCallingData(TwilioSdk.KEY_VIDEO_CALL_TWILIO_EXCEPTION, exceptionInfo)
        }

        private fun setupParticipants(room: Room) {
            room.localParticipant?.let { localParticipant ->
                localParticipantManager.localParticipant = localParticipant
                val participants = mutableListOf<Participant>()
                participants.add(localParticipant)
                localParticipant.setListener(LocalParticipantListener(this@RoomManager))

                room.remoteParticipants.forEach {
                    it.setListener(RemoteParticipantListener(this@RoomManager))
                    participants.add(it)
                }
                if (room.remoteParticipants.isNotEmpty()) {
                    sendCallState(CallState.Connected(room, callOptions))
                    startCountDownTimer()
                    broadcastCallback(TwilioSdk.TYPE_CONNECTED)
                }
                checkParticipant(participants)
                localParticipantManager.publishLocalTracks()
            }
        }

        private fun checkParticipant(participants: List<Participant>) {
            Log.d(TAG, "checkParticipant: $callOptions")
            for ((index, participant) in participants.withIndex()) {
                if (index == 0) { // local participant
                    participantManager.updateLocalParticipantSid(participant.sid)
                } else {
                    participantManager.addParticipant(
                        buildParticipantViewState(
                            participant,
                            callOptions?.remoteIdentity
                        )
                    )
                }
            }
            updateParticipantViewState()
        }
    }

    private fun updateParticipantViewState() {
        updateRoomViewState(
            initialViewState.copy(
                participantThumbnails = participantManager.participantThumbnails,
                primaryParticipant = participantManager.primaryParticipant
            )
        )

    }

    fun updateServiceUiState(isShow: Boolean) {
        _isUiOnService.postValue(isShow)
    }

    private fun broadcastCallback(type: String, msg: String? = null) {
        Intent(TwilioSdk.ACTION_CALLBACK).apply {
            putExtra(TwilioSdk.EXTRA_TYPE, type)
            putExtra(TwilioSdk.EXTRA_MSG, msg)
            putExtra(TwilioSdk.EXTRA_CALL_OPTIONS, Gson().toJson(callOptions))
        }.also {
            context.sendBroadcast(it)
        }
    }

    private fun broadcastNetworkLevel(networkQualityLevel: NetworkQualityLevel) {
        val networkQuality: Int
        val networkLevel: String
        when (networkQualityLevel) {
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_ZERO -> {
                networkLevel = "failed"
                networkQuality = 0
            }
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_ONE -> {
                networkLevel = "Very Bad"
                networkQuality = 1
            }
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_TWO -> {
                networkLevel = "Bad"
                networkQuality = 2
            }
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_THREE -> {
                networkLevel = "Good"
                networkQuality = 3
            }
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_FOUR -> {
                networkLevel = "Very Good"
                networkQuality = 4
            }
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_FIVE -> {
                networkLevel = "Excellent"
                networkQuality = 5
            }
            else -> {
                networkLevel = "Unknown"
                networkQuality = -1
            }
        }
        val qualityInfo = hashMapOf<String, String>()
        qualityInfo["quality"] = networkQuality.toString()
        qualityInfo["label"] = networkLevel
        qualityInfo["network"] = TwilioSdk.networkType(context)
        broadcastCallingData(TwilioSdk.KEY_VIDEO_CALL_NETWORK_QUALITY, qualityInfo)

    }

    private fun broadcastCallingData(key: String, data: HashMap<String, String>) {
        try {
            data["timestamp"] = System.currentTimeMillis().toString()
            data["data"] = Date().toString()
            data["calling_state"] = callState.value?.javaClass?.simpleName?.toString() ?: ""
            Log.d(TAG, "broadcastCallingData: $data")
            Intent(TwilioSdk.ACTION_CALL_DATA).apply {
                putExtra(TwilioSdk.EXTRA_CALL_DATA_KEY, key)
                data.entries.forEach { entry ->
                    putExtra(entry.key, entry.value)
                }
                callOptions?.serializeToMap()?.forEach {
                    putExtra(it.key, it.value.toString())
                }
                initialViewState.serializeToMap().forEach {
                    putExtra(it.key, it.value.toString())
                }
                putExtra(TwilioSdk.EXTRA_CALL_OPTIONS, Gson().toJson(callOptions))
            }.also {
                context.sendBroadcast(it)
            }
        } catch (e: Exception) {
        }
    }
}
