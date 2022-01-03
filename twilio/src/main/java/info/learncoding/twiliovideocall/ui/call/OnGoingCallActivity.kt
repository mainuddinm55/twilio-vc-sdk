package info.learncoding.twiliovideocall.ui.call

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.twilio.audioswitch.AudioDevice
import dagger.hilt.EntryPoints
import dagger.hilt.android.AndroidEntryPoint
import info.learncoding.twiliovideocall.R
import info.learncoding.twiliovideocall.TwilioSdk
import info.learncoding.twiliovideocall.data.model.CallOptions
import info.learncoding.twiliovideocall.data.model.UserType
import info.learncoding.twiliovideocall.databinding.TwilioActivityOnGoingCallBinding
import info.learncoding.twiliovideocall.di.RoomEntryPoint
import info.learncoding.twiliovideocall.receiver.VideoCallReceiver
import info.learncoding.twiliovideocall.ui.attachment.AttachmentActivity
import info.learncoding.twiliovideocall.ui.audio.AudioDevicesBottomSheetFragment
import info.learncoding.twiliovideocall.ui.participant.ParticipantAdapter
import info.learncoding.twiliovideocall.ui.participant.ParticipantViewState
import info.learncoding.twiliovideocall.ui.participant.PrimaryParticipantController
import info.learncoding.twiliovideocall.ui.room.*
import info.learncoding.twiliovideocall.utils.NotificationHelper.getNotificationFlag
import info.learncoding.twiliovideocall.utils.visibility
import java.util.*
import javax.inject.Inject

private const val PERMISSIONS_REQUEST_CODE = 2323
private const val TAG = "OnGoingCallActivity"

@AndroidEntryPoint
class OnGoingCallActivity : AppCompatActivity() {
    private lateinit var binding: TwilioActivityOnGoingCallBinding
    private lateinit var primaryParticipantController: PrimaryParticipantController
    private lateinit var participantAdapter: ParticipantAdapter
    private var savedVolumeControlStream = 0

    private var pictureInPictureParams: PictureInPictureParams.Builder? = null

    private var callOptions: CallOptions? = null

    @Inject
    lateinit var roomManagerProvider: RoomManagerProvider
    private lateinit var viewModel: RoomViewModel
    private var roomManager: RoomManager? = null


    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            requestPermissions()
        }
    }
    private val widgetPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        showFloatingWidget()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // So calls can be answered when screen is locked
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        roomManagerProvider.roomComponent?.let { component ->
            EntryPoints.get(component, RoomEntryPoint::class.java)
        }?.getRoomManager().also {
            roomManager = it
        }
        if (roomManager == null) {
            finish()
            return
        }
        viewModel = ViewModelProvider(
            this,
            RoomViewModel.Factory(roomManager!!)
        )[RoomViewModel::class.java]

        binding = TwilioActivityOnGoingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                pictureInPictureParams = PictureInPictureParams.Builder()
            }
        }

        binding.answerCallImageView.setOnClickListener {
            callOptions ?: return@setOnClickListener
            viewModel.processInput(RoomActionEvent.Connect(callOptions!!))
        }

        binding.endCallImageView.setOnClickListener {
            viewModel.processInput(
                RoomActionEvent.Disconnect(
                    callOptions?.userType == UserType.RECEIVER && roomManager?.room == null,
                    callOptions?.userType == UserType.CALLER && roomManager?.isCallConnected() == false
                )
            )
        }

        binding.videoControllerImageView.setOnClickListener {
            viewModel.processInput(RoomActionEvent.ToggleLocalVideo)
        }
        binding.audioControllerImageView.setOnClickListener {
            viewModel.processInput(RoomActionEvent.ToggleLocalAudio)
        }
        binding.cameraControllerImageView.setOnClickListener {
            viewModel.processInput(RoomActionEvent.SwitchCamera)
        }
        binding.audioDeviceControllerImageView.setOnClickListener {
            displayAudioDevices()
        }
        binding.attachmentImageView.setOnClickListener {
            showAttachment()
        }

        savedVolumeControlStream = volumeControlStream

        primaryParticipantController = PrimaryParticipantController(
            binding.contentVideo.primaryVideo
        )

        setupParticipantRecyclerView()

        viewModel.viewState.observe(this) {
            bindRoomViewState(it)
        }
        viewModel.callState.observe(this) {
            bindCallState(it)
        }
        viewModel.duration.observe(this) {
            bindCallDuration(it)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (isAllPermissionGrant(grantResults)) {
                viewModel.processInput(RoomActionEvent.Setup(true))
            } else {
                MaterialAlertDialogBuilder(this).apply {
                    setTitle(getString(R.string.twilio_need_permissions))
                    setMessage(getString(R.string.twilio_permission_need_desc))
                        .setPositiveButton(R.string.twilio_goto_settings) { dialog, _ ->
                            dialog.dismiss()
                            permissionLauncher.launch(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:$packageName")
                                ),
                            )

                        }
                    setCancelable(false)
                }.also {
                    it.show()
                }
            }
        }
    }

    private fun isAllPermissionGrant(grantResults: IntArray): Boolean {
        var isGrant = true
        for (result in grantResults) {
            isGrant = result == PackageManager.PERMISSION_GRANTED
            if (!isGrant) {
                break
            }
        }
        return isGrant
    }

    override fun onPause() {
        super.onPause()
        if (isPermissionsGranted()) {
            when (viewModel.callState.value) {
                is CallState.Connected, is CallState.Connecting, is CallState.Incoming -> {
                    Log.d(
                        TAG,
                        "onPause: enable pip : ${isPipEnabled()} params: $pictureInPictureParams"
                    )
                    if (isPipEnabled() && pictureInPictureParams != null) {
                        minimizeCall()
                    } else {
                        showFloatingWidget()
                    }
                }
                else -> Unit
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Log.d(TAG, "onPictureInPictureModeChanged: $isInPictureInPictureMode")
        if (isInPictureInPictureMode) {
            binding.controllerLayout.visibility(false)
            binding.audioDeviceControllerImageView.visibility(false)
            binding.contentVideo.remoteVideoThumbnails.visibility(false)
        } else {
            binding.controllerLayout.visibility(true)
            binding.audioDeviceControllerImageView.visibility(true)
            binding.contentVideo.remoteVideoThumbnails.visibility(true)
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: ")
        if (isPermissionsGranted()) {
            viewModel.processInput(RoomActionEvent.Setup(true))
        } else {
            requestPermissions()
        }
    }

    override fun onBackPressed() {
        if (isPipEnabled()) {
            onUserLeaveHint()
        } else {
            super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPipEnabled() && pictureInPictureParams != null) {
            minimizeCall()
        } else {
            viewModel.showFloatingWidget()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun minimizeCall() {
        pictureInPictureParams?.setAspectRatio(Rational(2, 3))
        enterPictureInPictureMode(pictureInPictureParams!!.build())
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

    private fun requestPermissions() {
        // nested if statements used to keep lint happy and avoid needing a @SuppressLint decoration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    PERMISSIONS_REQUEST_CODE
                )
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                    ),
                    PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    private fun setupParticipantRecyclerView() {
        participantAdapter = ParticipantAdapter()

        binding.contentVideo.remoteVideoThumbnails.apply {
            layoutManager = LinearLayoutManager(
                this@OnGoingCallActivity, LinearLayoutManager.HORIZONTAL, false
            )
            adapter = participantAdapter
        }
    }

    private fun displayAudioDevices() {
        val audioDevicesBottomSheetFragment = AudioDevicesBottomSheetFragment()
        audioDevicesBottomSheetFragment.deviceSelectedListener = { item: AudioDevice? ->
            item?.let {
                viewModel.processInput(RoomActionEvent.SwitchAudioDevice(item))
            }
        }
        audioDevicesBottomSheetFragment.show(supportFragmentManager, "audio_device")
    }

    private fun bindRoomViewState(roomViewState: RoomViewState) {
        if (!isPipMode()) {
            binding.audioDeviceControllerImageView.visibility(
                roomViewState.availableAudioDevices?.isNotEmpty() ?: false
            )
        }
        renderPrimaryView(roomViewState.primaryParticipant)
        renderThumbnails(roomViewState)
        Log.d(TAG, "bindRoomViewState: $roomViewState")
        updateLayout(roomViewState)
        updateAudioDeviceIcon(roomViewState.selectedDevice)
        updateAttachmentIconVisibility()
    }

    private fun renderPrimaryView(primaryParticipant: ParticipantViewState) {
        primaryParticipant.run {
            primaryParticipantController.renderAsPrimary(
                if (isLocalParticipant) getString(R.string.twilio_you) else identity,
                videoTrack,
                isMuted,
                isMirrored,
                networkQualityLevel
            )
        }
    }

    private fun renderThumbnails(roomViewState: RoomViewState) {
        val newThumbnails = roomViewState.participantThumbnails
        Log.d(TAG, "renderThumbnails: $newThumbnails")
        if (newThumbnails.isNullOrEmpty()) return
        participantAdapter.submitList(newThumbnails)
    }

    private fun updateLayout(roomViewState: RoomViewState) {
        val isMicEnabled = roomViewState.isMicEnabled
        val isCameraEnabled = roomViewState.isCameraEnabled
        val isLocalMediaEnabled = isMicEnabled && isCameraEnabled
        binding.audioControllerImageView.isEnabled = isLocalMediaEnabled
        binding.videoControllerImageView.isEnabled = isLocalMediaEnabled
        val micDrawable =
            if (roomViewState.isAudioMuted || !isLocalMediaEnabled) R.drawable.twilio_ic_mic_off_black_24dp else R.drawable.twilio_ic_mic_black_24dp
        val videoDrawable =
            if (roomViewState.isVideoOff || !isLocalMediaEnabled) R.drawable.twilio_ic_videocam_off_black_24dp else R.drawable.twilio_ic_videocam_black_24dp
        binding.audioControllerImageView.setImageResource(micDrawable)
        binding.videoControllerImageView.setImageResource(videoDrawable)

    }

    private fun updateAudioDeviceIcon(selectedAudioDevice: AudioDevice?) {
        val audioDeviceMenuIcon = when (selectedAudioDevice) {
            is AudioDevice.BluetoothHeadset -> R.drawable.twilio_ic_bluetooth_white_24dp
            is AudioDevice.WiredHeadset -> R.drawable.twilio_ic_headset_mic_white_24dp
            is AudioDevice.Speakerphone -> R.drawable.twilio_ic_volume_up_white_24dp
            else -> R.drawable.twilio_ic_phonelink_ring_white_24dp
        }
        binding.audioDeviceControllerImageView.setImageResource(audioDeviceMenuIcon)
    }

    private fun updateAttachmentIconVisibility() {
        if (callOptions?.attachments.isNullOrEmpty()) {
            binding.attachmentImageView.visibility(isVisible = false)
        } else {
            binding.attachmentImageView.visibility(isVisible = !isPipMode())
        }
    }

    private fun showAttachment() {
        if (callOptions?.attachments.isNullOrEmpty()) {
            Toast.makeText(this, "No attachment", Toast.LENGTH_SHORT).show()
            return
        }
        val images: ArrayList<String> = callOptions?.attachments?.map {
            return@map it
        } as ArrayList<String>

        AttachmentActivity.buildIntent(this, images).also {
            startActivity(it)
        }
    }

    private fun bindCallState(callState: CallState) {
        Log.d(TAG, "bindRoomViewEffects: $callState")
        when (callState) {
            is CallState.Connected -> {
                toggleAudioDevice(true)
                showConnectViews(callState)
            }
            is CallState.Disconnected -> {
                toggleAudioDevice(false)
                finish()
            }
            is CallState.ConnectionFailed -> {
                toggleAudioDevice(false)
                finish()
            }

            CallState.Lobby -> showEmptyState()
            is CallState.Incoming -> showIncomingViews(callState)
            is CallState.Ringing -> showRingingViews(callState)
            is CallState.Connecting -> showConnectingViews(callState)
            is CallState.Reconnecting -> {
            }
        }
        if (isPipEnabled()) {
            updatePipActions(callState)
        }
    }

    private fun showEmptyState() {
        binding.answerCallImageView.visibility(false)
        binding.endCallImageView.visibility(false)
        binding.statusTextView.text = null
        binding.identityNameTextView.text = null
        binding.callDurationTextView.text = null
    }

    private fun showIncomingViews(callState: CallState.Incoming) {
        Log.d(TAG, "showIncomingViews: $callState")
        this.callOptions = callState.callOptions
        binding.answerCallImageView.visibility(true)
        binding.endCallImageView.visibility(true)
        binding.statusTextView.text = getString(R.string.twilio_maya_expert_calling)
        binding.identityNameTextView.text = callState.callOptions.remoteIdentity
    }

    private fun showConnectingViews(callState: CallState) {
        Log.d(TAG, "showConnectingViews: $callState")
        this.callOptions = callState.options
        binding.answerCallImageView.visibility(false)
        binding.endCallImageView.visibility(true)
        binding.statusTextView.text = getString(R.string.twilio_call_state_connecting)
        binding.identityNameTextView.text = callState.options?.remoteIdentity

    }

    private fun showConnectViews(callState: CallState.Connected) {
        this.callOptions = callState.callOptions
        binding.answerCallImageView.visibility(false)
        binding.endCallImageView.visibility(true)
        binding.statusTextView.text = getString(R.string.twilio_call_state_connected)
        binding.identityNameTextView.text = callState.callOptions?.remoteIdentity
    }

    private fun showRingingViews(callState: CallState.Ringing) {
        this.callOptions = callState.callOptions
        binding.answerCallImageView.visibility(false)
        binding.endCallImageView.visibility(true)
        binding.statusTextView.text = getString(R.string.twilio_call_state_ringing)
        binding.identityNameTextView.text = callState.callOptions?.remoteIdentity
    }

    private fun toggleAudioDevice(enableAudioDevice: Boolean) {
        setVolumeControl(enableAudioDevice)
    }

    private fun setVolumeControl(setVolumeControl: Boolean) {
        volumeControlStream = if (setVolumeControl) {
            /*
             * Enable changing the volume using the up/down keys during a conversation
             */
            AudioManager.STREAM_VOICE_CALL
        } else {
            savedVolumeControlStream
        }
    }

    private fun bindCallDuration(duration: Long) {
        if (duration > 0) {
            val minutes = (duration % 3600) / 60
            val seconds = duration % 60

            val durationFormatted = String.format(
                Locale.getDefault(),
                "%02d:%02d", minutes, seconds
            )
            binding.callDurationTextView.text = durationFormatted
            binding.callDurationTextView.visibility(true)
        } else {
            binding.callDurationTextView.visibility(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePipActions(callState: CallState) {
        val actions = mutableListOf<RemoteAction>()

        when (callState) {
            is CallState.Connected -> actions.add(
                getCallEndRemoteAction()
            )
            is CallState.Connecting -> actions.add(
                getCallEndRemoteAction()
            )
            is CallState.ConnectionFailed -> Unit
            is CallState.Disconnected -> Unit
            is CallState.Incoming -> {
                actions.add(getCallAcceptRemoteAction())
                actions.add(getCallEndRemoteAction())
            }
            CallState.Lobby -> Unit
            is CallState.Reconnecting -> actions.add(
                getCallEndRemoteAction()
            )
            is CallState.Ringing -> actions.add(
                getCallEndRemoteAction()
            )
        }

        pictureInPictureParams?.setActions(actions)
        pictureInPictureParams?.let {
            setPictureInPictureParams(it.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCallEndRemoteAction(): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(this, R.drawable.twilio_ic_baseline_call_end_24),
            "End",
            "End",
            PendingIntent.getBroadcast(
                this, TwilioSdk.REQUEST_CODE_END,
                Intent(this, VideoCallReceiver::class.java).apply {
                    action = TwilioSdk.ACTION_CALL
                    putExtra(TwilioSdk.EXTRA_TYPE, TwilioSdk.TYPE_END)
                },
                getNotificationFlag()
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCallAcceptRemoteAction(): RemoteAction {
        Log.d(TAG, "getCallAcceptRemoteAction: ${Gson().toJson(callOptions)}")
        val callOptionsJson = Gson().toJson(callOptions)
        val bundle = bundleOf(
            TwilioSdk.EXTRA_CALL_OPTIONS to callOptionsJson,
            TwilioSdk.EXTRA_TYPE to TwilioSdk.TYPE_ACCEPT
        )
        bundle.keySet().forEach {
            Log.d(TAG, "getCallAcceptRemoteAction: key $it and value ${bundle.get(it)}")
        }
        return RemoteAction(
            Icon.createWithResource(this, R.drawable.twilio_ic_baseline_call_24),
            "Answer",
            "Answer",
            PendingIntent.getBroadcast(
                this, TwilioSdk.REQUEST_CODE_ANSWER,
                Intent(this, VideoCallReceiver::class.java).apply {
                    action = TwilioSdk.ACTION_CALL
                    putExtras(bundle)
                },
                getNotificationFlag()
            )
        )
    }

    private fun isPipMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isInPictureInPictureMode
        } else {
            false
        }
    }

    private fun isPipEnabled(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.hasSystemFeature(
            PackageManager.FEATURE_PICTURE_IN_PICTURE
        )
    }

    private fun showFloatingWidget() {
        primaryParticipantController.removeExistingSink()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (drawingPermissionGranted()) {
                viewModel.showFloatingWidget()
            } else {
                requestDrawingPermission()
            }
        } else {
            viewModel.showFloatingWidget()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun drawingPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestDrawingPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        widgetPermissionLauncher.launch(intent)
    }

}