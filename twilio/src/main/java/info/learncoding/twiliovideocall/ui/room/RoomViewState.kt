package info.learncoding.twiliovideocall.ui.room

import com.google.gson.annotations.SerializedName
import com.twilio.audioswitch.AudioDevice
import info.learncoding.twiliovideocall.ui.participant.ParticipantViewState

data class RoomViewState(
    @SerializedName("primary_participant")
    val primaryParticipant: ParticipantViewState,
    @SerializedName("remote_participant")
    val participantThumbnails: List<ParticipantViewState>? = null,
    @SerializedName("selected_audio_device")
    val selectedDevice: AudioDevice? = null,
    @SerializedName("audio_devices")
    val availableAudioDevices: List<AudioDevice>? = null,
    @SerializedName("is_camera_enabled")
    val isCameraEnabled: Boolean = false,
    @SerializedName("is_mic_enabled")
    val isMicEnabled: Boolean = false,
    @SerializedName("is_audio_muted")
    val isAudioMuted: Boolean = false,
    @SerializedName("is_video_off")
    val isVideoOff: Boolean = false,
)