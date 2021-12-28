package info.learncoding.twiliovideocall.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import dagger.hilt.EntryPoints
import dagger.hilt.android.AndroidEntryPoint
import info.learncoding.twiliovideocall.R
import info.learncoding.twiliovideocall.TwilioSdk
import info.learncoding.twiliovideocall.TwilioSdk.EXTRA_CALL_OPTIONS
import info.learncoding.twiliovideocall.data.model.CallOptions
import info.learncoding.twiliovideocall.data.model.UserType
import info.learncoding.twiliovideocall.data.repository.VideoCallRepository
import info.learncoding.twiliovideocall.databinding.TwilioVideoCallFloatingViewBinding
import info.learncoding.twiliovideocall.di.RoomEntryPoint
import info.learncoding.twiliovideocall.di.TwilioSDK
import info.learncoding.twiliovideocall.receiver.VideoCallReceiver
import info.learncoding.twiliovideocall.ui.call.OnGoingCallActivity
import info.learncoding.twiliovideocall.ui.participant.ParticipantManager
import info.learncoding.twiliovideocall.ui.participant.ParticipantViewState
import info.learncoding.twiliovideocall.ui.participant.PrimaryParticipantController
import info.learncoding.twiliovideocall.ui.room.*
import info.learncoding.twiliovideocall.utils.NotificationHelper.buildCallNotification
import info.learncoding.twiliovideocall.utils.NotificationHelper.buildOngoingCallNotification
import info.learncoding.twiliovideocall.utils.NotificationHelper.isAppForeground
import info.learncoding.twiliovideocall.utils.NotificationHelper.showNotification
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NOTIFICATION_ID = 1200
private const val TAG = "VideoCallService"

@AndroidEntryPoint
class VideoCallService : LifecycleService() {
    private var wakeLock: WakeLock? = null

    private var roomManager: RoomManager? = null

    @Inject
    lateinit var roomManagerProvider: RoomManagerProvider

    @Inject
    @TwilioSDK
    lateinit var repository: VideoCallRepository

    private val participantManager = ParticipantManager()
    private val audioSwitch by lazy {
        AudioSwitch(
            this,
            loggingEnabled = true,
            preferredDeviceList = listOf(
                AudioDevice.BluetoothHeadset::class.java,
                AudioDevice.WiredHeadset::class.java,
                AudioDevice.Speakerphone::class.java,
                AudioDevice.Earpiece::class.java
            )
        )
    }

    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var videoCallFloatingViewBinding: TwilioVideoCallFloatingViewBinding
    private lateinit var participantController: PrimaryParticipantController
    private var isAttachView = false
    private var showDurationNotification = false

    private var mediaPlayer: MediaPlayer? = null
    private var countDownTimer: CountDownTimer? = null
    private var ringingTimestamp: Long = 0
    private var currentAudioLevel = 0

    private var callOptions: CallOptions? = null

    companion object {
        private var callbackReceiver: BroadcastReceiver? = null
        private val videoCallReceiver = VideoCallReceiver()
        private var registeredCallbackReceiver = false
        private var registeredVideoCallReceiver = false

        fun startService(context: Context, callOptions: CallOptions, callback: BroadcastReceiver) {
            callbackReceiver = callback
            Intent(context, VideoCallService::class.java).also {
                it.putExtra(EXTRA_CALL_OPTIONS, Gson().toJson(callOptions))
                ContextCompat.startForegroundService(context, it)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (isAppForeground(context)) {
                    Intent(context, OnGoingCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        context.startActivity(this)
                    }
                }
            } else {
                Intent(context, OnGoingCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    context.startActivity(this)
                }
            }
        }

        fun stopService(context: Context) {
            Intent(context, VideoCallService::class.java).also {
                context.stopService(it)
            }
        }
    }

    private val uiStateObserver = Observer<CallState> { state ->
        Log.d(TAG, "UiStateObserver: $state")
        when (state) {
            is CallState.Connected -> {
                clearResource()
                showConnectedViewState()
                if (!showDurationNotification) {
                    showNotification(
                        this,
                        NOTIFICATION_ID,
                        buildOngoingCallNotification(this, true)
                    )
                    showDurationNotification = true
                }
                toggleAudioDevice(true)
            }
            is CallState.Connecting -> {
                if (callOptions?.userType == UserType.CALLER) {
                    startCallingTimer()
                } else if (callOptions?.userType == UserType.RECEIVER) {
                    clearResource()
                }
                showConnectingViewState()
            }
            is CallState.ConnectionFailed -> {
                toggleAudioDevice(false)
                stopSelf()
            }
            is CallState.Disconnected -> {
                toggleAudioDevice(false)
                stopSelf()
            }
            is CallState.Incoming -> {
                showIncomingViewState()
                if (callOptions?.userType == UserType.RECEIVER) {
                    startCallingTimer()
                }
            }
            CallState.Lobby -> {
                toggleAudioDevice(false)
            }
            is CallState.Reconnecting -> showReconnectingViewState()
            is CallState.Ringing -> showRingingViewState()
        }
    }

    private val viewStateObserver = Observer<RoomViewState> { viewState ->
        Log.d(TAG, "viewStateObserver: $viewState")
        if (roomManager?.isUiOnService?.value == true) {
            renderPrimaryView(viewState.primaryParticipant)
        }
    }

    private val isUiOnServiceObserver = Observer<Boolean> {
        Log.d(TAG, "isUiOnServiceObserver: $it")
        if (it) {
            var permissionDenied = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionDenied = !Settings.canDrawOverlays(this@VideoCallService)
            }

            if (!permissionDenied && !isAttachView) {

                windowManager.addView(
                    videoCallFloatingViewBinding.participantBackground,
                    params
                )
                isAttachView = true
                roomManager?.setupLocalTrack(isPermissionsGranted())
            } else {
                //TODO request for permission
            }
        } else {
            if (isAttachView) {
                windowManager.removeView(videoCallFloatingViewBinding.participantBackground)
                isAttachView = false
            }
        }
    }

    private fun isPermissionsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreate() {
        super.onCreate()
        roomManagerProvider.createRoomScope(
            RoomManager(
                this,
                repository,
                audioSwitch,
                participantManager
            )
        )
        roomManagerProvider.roomComponent?.let { component ->
            EntryPoints.get(component, RoomEntryPoint::class.java).getRoomManager()
                .also { roomManager = it }
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        videoCallFloatingViewBinding = TwilioVideoCallFloatingViewBinding.inflate(
            LayoutInflater.from(this)
        )
        participantController = PrimaryParticipantController(
            videoCallFloatingViewBinding.primaryVideo
        )
        setLayoutParams()
        drawMovementOfWidget()
        widgetClickListener()
        registerCallbackReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        manageWakeLock()
        val callOptionJson = intent?.getStringExtra(EXTRA_CALL_OPTIONS)
        callOptions = Gson().fromJson(callOptionJson, CallOptions::class.java)
        callOptions?.let { callOptions ->
            startForeground(
                NOTIFICATION_ID, buildCallNotification(
                    this, callOptions, callOptions.userType != UserType.RECEIVER,
                    showTimer = false
                )
            )
            when (callOptions.userType) {
                UserType.CALLER -> {
                    mediaPlayer = MediaPlayer.create(this, R.raw.twilio_outgoing_ringtone).apply {
                        isLooping = true
                    }
                    lifecycleScope.launch {
                        roomManager?.connect(callOptions)
                    }
                }
                UserType.RECEIVER -> {
                    try {
                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        currentAudioLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val maxIndex = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxIndex, 0)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    mediaPlayer = MediaPlayer.create(this, R.raw.twilio_incoming_ringtone).apply {
                        isLooping = true
                        setVolume(1f, 1f)
                    }
                    roomManager?.setIncoming(callOptions)
                }
            }
        } ?: kotlin.run {
            stopSelf()
        }
        Log.d(TAG, "onStartCommand: $roomManager")
        roomManager?.isUiOnService?.observe(this, isUiOnServiceObserver)
        roomManager?.callState?.observe(this, uiStateObserver)
        roomManager?.viewState?.observe(this, viewStateObserver)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setLayoutParams() {
        //setting the layout parameters
        params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }
        //params.gravity = Gravity.END or Gravity.TOP
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun drawMovementOfWidget() {
        videoCallFloatingViewBinding.participantBackground.setOnTouchListener(object :
            View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                //Timber.d("OnViewTouch: event $event")
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        //when the drag is ended switching the state of the widget
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        //this code is helping the widget to move around the screen with fingers
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        //Timber.d("MovingX: ${params.x}")
                        //Timber.d("MovingY: ${params.y}")
                        windowManager.updateViewLayout(
                            videoCallFloatingViewBinding.participantBackground,
                            params
                        )
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun widgetClickListener() {
        videoCallFloatingViewBinding.expandImageView.setOnClickListener {
            expandFullScreen()
        }
        videoCallFloatingViewBinding.closeImageView.setOnClickListener {
            if (isAttachView) {
                windowManager.removeView(videoCallFloatingViewBinding.participantBackground)
                isAttachView = false
            }
        }
        videoCallFloatingViewBinding.answerCallImageView.setOnClickListener {
            callOptions ?: return@setOnClickListener
            lifecycleScope.launch {
                roomManager?.connect(callOptions!!)
            }
        }
        videoCallFloatingViewBinding.endCallImageView.setOnClickListener {
            roomManager?.disconnect()
            clearResource()
        }
    }

    private fun expandFullScreen() {
        if (isAttachView) {
            windowManager.removeView(videoCallFloatingViewBinding.participantBackground)
            isAttachView = false
            startActivity(Intent(this, OnGoingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }
    }

    private fun startCallingTimer() {
        if (countDownTimer == null) {
            ringingTimestamp = 55000
            countDownTimer = object : CountDownTimer(ringingTimestamp, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    ringingTimestamp = millisUntilFinished
                }

                override fun onFinish() {
                    //Timber.d("Timer finish")
                    roomManager?.disconnect()
                }
            }.start()
            Log.d(TAG, "startCallingTimer: before start")
            mediaPlayer?.start()
        }

    }

    private fun toggleAudioDevice(activate: Boolean) {
        if (activate) {
            roomManager?.audioActivate()
        } else {
            roomManager?.audioDeactivate()
        }
    }

    private fun showConnectedViewState() {
        videoCallFloatingViewBinding.statusTextView.text =
            getString(R.string.twilio_call_state_connected)
        videoCallFloatingViewBinding.endCallImageView.visibility = View.VISIBLE
        videoCallFloatingViewBinding.answerCallImageView.visibility = View.GONE
    }

    private fun showIncomingViewState() {
        videoCallFloatingViewBinding.statusTextView.text =
            getString(R.string.twilio_call_state_incoming)
        videoCallFloatingViewBinding.answerCallImageView.visibility = View.VISIBLE
        videoCallFloatingViewBinding.endCallImageView.visibility = View.VISIBLE
    }

    private fun showConnectingViewState() {
        videoCallFloatingViewBinding.statusTextView.text =
            getString(R.string.twilio_call_state_connecting)
        videoCallFloatingViewBinding.endCallImageView.visibility = View.GONE
        videoCallFloatingViewBinding.answerCallImageView.visibility = View.GONE
    }

    private fun showReconnectingViewState() {
        videoCallFloatingViewBinding.statusTextView.text =
            getString(R.string.twilio_call_state_reconnecting)
        videoCallFloatingViewBinding.endCallImageView.visibility = View.VISIBLE
        videoCallFloatingViewBinding.answerCallImageView.visibility = View.GONE
    }

    private fun showRingingViewState() {
        videoCallFloatingViewBinding.statusTextView.text =
            getString(R.string.twilio_call_state_ringing)
        videoCallFloatingViewBinding.endCallImageView.visibility = View.VISIBLE
        videoCallFloatingViewBinding.answerCallImageView.visibility = View.GONE
    }

    private fun renderPrimaryView(primaryParticipant: ParticipantViewState) {
        primaryParticipant.run {
            participantController.renderAsPrimary(
                if (isLocalParticipant) getString(R.string.twilio_you) else identity,
                videoTrack,
                isMuted,
                isMirrored,
                networkQualityLevel
            )
        }
    }

    private fun clearResource() {
        Log.d(TAG, "clearResource: ")
        mediaPlayer?.stop()
        mediaPlayer?.release()
        resetAudioLevel()
        countDownTimer?.cancel()
        mediaPlayer = null
        countDownTimer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        Log.d(TAG, "onDestroy: $roomManager")
        roomManager?.callState?.removeObserver(uiStateObserver)
        roomManager?.viewState?.removeObserver(viewStateObserver)
        roomManager?.isUiOnService?.removeObserver(isUiOnServiceObserver)
        roomManager?.room?.disconnect()
        clearResource()
        if (isAttachView) {
            windowManager.removeView(videoCallFloatingViewBinding.participantBackground)
        }
        roomManagerProvider.destroyScope()
        //wakeLock?.release()
        unregisterCallbackReceiver()
    }

    private fun registerCallbackReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            callbackReceiver?.let {
                registerReceiver(it, IntentFilter(TwilioSdk.ACTION_CALLBACK))
                registeredCallbackReceiver = true
            }

            registerReceiver(videoCallReceiver, IntentFilter(TwilioSdk.ACTION_CALL))
            registeredVideoCallReceiver = true
        }
    }

    private fun unregisterCallbackReceiver() {
        if (registeredCallbackReceiver) {
            unregisterReceiver(callbackReceiver)
            registeredCallbackReceiver = false
        }
        if (registeredVideoCallReceiver) {
            unregisterReceiver(videoCallReceiver)
            registeredVideoCallReceiver = false
        }
    }

    private fun manageWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive
        if (!isScreenOn) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "TwilioSDK:Wakelock"
            )
            wakeLock?.acquire(1000L * 60) //60s
        }
    }

    private fun resetAudioLevel() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentAudioLevel, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}