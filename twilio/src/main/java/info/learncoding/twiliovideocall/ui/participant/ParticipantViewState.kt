package info.learncoding.twiliovideocall.ui.participant

import com.twilio.video.NetworkQualityLevel
import com.twilio.video.NetworkQualityLevel.NETWORK_QUALITY_LEVEL_UNKNOWN
import com.twilio.video.Participant
import com.twilio.video.RemoteVideoTrack
import info.learncoding.twiliovideocall.ui.room.VideoTrackViewState

data class ParticipantViewState(
    val sid: String? = null,
    val identity: String? = null,
    val videoTrack: VideoTrackViewState? = null,
    val isMuted: Boolean = false,
    val isMirrored: Boolean = false,
    val isLocalParticipant: Boolean = false,
    val networkQualityLevel: NetworkQualityLevel = NETWORK_QUALITY_LEVEL_UNKNOWN
) {

    fun getRemoteVideoTrack(): RemoteVideoTrack? =
        if (!isLocalParticipant) videoTrack?.videoTrack as RemoteVideoTrack? else null
}

fun buildParticipantViewState(participant: Participant): ParticipantViewState {
    val videoTrack = participant.videoTracks.firstOrNull()?.videoTrack
    return ParticipantViewState(
        participant.sid,
        participant.identity,
        videoTrack?.let { VideoTrackViewState(it) },
        networkQualityLevel = participant.networkQualityLevel,
        isMuted = participant.audioTracks.firstOrNull() == null
    )
}
