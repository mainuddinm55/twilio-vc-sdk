package info.learncoding.twiliovideocall.ui.participant

import android.content.Context
import android.util.Log
import com.twilio.video.*
import com.twilio.video.ktx.createLocalAudioTrack
import info.learncoding.twiliovideocall.R
import info.learncoding.twiliovideocall.ui.room.*

private const val TAG = "LocalParticipantManager"

class LocalParticipantManager(
    private val context: Context,
    private val roomManager: RoomManager,
) {

    private var localAudioTrack: LocalAudioTrack? = null
        set(value) {
            field = value
            roomManager.updateLocalAudioTrack(value == null)
        }
    internal var localParticipant: LocalParticipant? = null
    private var cameraVideoTrack: LocalVideoTrack? = null
        set(value) {
            field = value
            Log.d(TAG, "cameraVideoTrack: isFrontCamera: ${cameraCapturer?.isFrontCamera()}")
            roomManager.updateLocalVideoTrack(value, cameraCapturer?.isFrontCamera() == true)
        }
    private var cameraCapturer: CameraCapturerCompat? = null

    private var isAudioMuted = false
    private var isVideoMuted = false
    internal val localVideoTrackNames: MutableMap<String, String> = HashMap()

    fun setupLocalTrack() {
        Log.d(TAG, "setupLocalTrack: audio mute $isAudioMuted")
        Log.d(TAG, "setupLocalTrack: video mute $isVideoMuted")
        if (!isAudioMuted) setupLocalAudioTrack()
        if (!isVideoMuted) setupLocalVideoTrack()
    }

    fun releaseLocalTrack() {
        removeCameraTrack()
        removeAudioTrack()
    }

    fun toggleLocalVideo() {
        if (!isVideoMuted) {
            isVideoMuted = true
            removeCameraTrack()
        } else {
            isVideoMuted = false
            setupLocalVideoTrack()
        }
    }

    fun toggleLocalAudio() {
        if (!isAudioMuted) {
            isAudioMuted = true
            removeAudioTrack()
        } else {
            isAudioMuted = false
            setupLocalAudioTrack()
        }
    }

    fun enableLocalVideo() {
        cameraVideoTrack?.enable(true)
    }

    fun disableLocalVideo() {
        cameraVideoTrack?.enable(false)
    }

    fun publishLocalTracks() {
        publishAudioTrack(localAudioTrack)
        publishCameraTrack(cameraVideoTrack)
    }

    fun switchCamera() {
        cameraCapturer?.switchCamera()
        Log.d(TAG, "switchCamera:isFrontCamera: ${cameraCapturer?.isFrontCamera()}")
        roomManager.updateLocalVideoTrack(
            cameraVideoTrack,
            cameraCapturer?.isFrontCamera() == false
        )
    }

    private fun setupLocalAudioTrack() {
        if (localAudioTrack == null && !isAudioMuted) {
            localAudioTrack = createLocalAudioTrack(context, true, MICROPHONE_TRACK_NAME)
            localAudioTrack?.let { publishAudioTrack(it) }
                ?: Log.e(TAG, "Failed to create local audio track", RuntimeException())
        }
    }

    private fun publishCameraTrack(localVideoTrack: LocalVideoTrack?) {
        if (!isVideoMuted) {
            localVideoTrack?.let {
                localParticipant?.publishTrack(
                    it,
                    LocalTrackPublicationOptions(TrackPriority.LOW)
                )
            }
        }
    }

    private fun publishAudioTrack(localAudioTrack: LocalAudioTrack?) {
        if (!isAudioMuted) {
            localAudioTrack?.let { localParticipant?.publishTrack(it) }
        }
    }

    private fun unpublishTrack(localVideoTrack: LocalVideoTrack?) =
        localVideoTrack?.let { localParticipant?.unpublishTrack(it) }

    private fun unpublishTrack(localAudioTrack: LocalAudioTrack?) =
        localAudioTrack?.let { localParticipant?.unpublishTrack(it) }

    private fun setupLocalVideoTrack() {
        val videoFormat = VideoFormat(VideoDimensions.VGA_VIDEO_DIMENSIONS, 30)

        cameraCapturer = CameraCapturerCompat.newInstance(context)
        cameraVideoTrack = cameraCapturer?.let { cameraCapturer ->
            LocalVideoTrack.create(
                context,
                true,
                cameraCapturer,
                videoFormat,
                CAMERA_TRACK_NAME
            )
        }
        cameraVideoTrack?.let { cameraVideoTrack ->
            localVideoTrackNames[cameraVideoTrack.name] =
                context.getString(R.string.twilio_local_video_track)
            publishCameraTrack(cameraVideoTrack)
        } ?: run {
            Log.e(TAG, "Failed to create the local camera video track", RuntimeException())
        }
    }

    private fun removeCameraTrack() {
        cameraVideoTrack?.let { cameraVideoTrack ->
            unpublishTrack(cameraVideoTrack)
            localVideoTrackNames.remove(cameraVideoTrack.name)
            cameraVideoTrack.release()
            this.cameraVideoTrack = null
        }
    }

    private fun removeAudioTrack() {
        localAudioTrack?.let { localAudioTrack ->
            unpublishTrack(localAudioTrack)
            localAudioTrack.release()
            this.localAudioTrack = null
        }
    }
}
