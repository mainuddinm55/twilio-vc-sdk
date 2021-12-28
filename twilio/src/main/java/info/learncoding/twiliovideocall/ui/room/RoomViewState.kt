package info.learncoding.twiliovideocall.ui.room

import com.twilio.audioswitch.AudioDevice
import info.learncoding.twiliovideocall.ui.participant.ParticipantViewState

data class RoomViewState(
    val primaryParticipant: ParticipantViewState,
    val title: String? = null,
    val participantThumbnails: List<ParticipantViewState>? = null,
    val selectedDevice: AudioDevice? = null,
    val availableAudioDevices: List<AudioDevice>? = null,
    val isCameraEnabled: Boolean = false,
    val localVideoTrack: VideoTrackViewState? = null,
    val isMicEnabled: Boolean = false,
    val isAudioMuted: Boolean = false,
    val isAudioEnabled: Boolean = true,
    val isVideoEnabled: Boolean = true,
    val isVideoOff: Boolean = false,
)
