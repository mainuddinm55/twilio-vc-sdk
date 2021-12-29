package info.learncoding.twiliovideocall.ui.participant

import com.google.gson.annotations.SerializedName
import com.twilio.video.NetworkQualityLevel
import com.twilio.video.NetworkQualityLevel.NETWORK_QUALITY_LEVEL_UNKNOWN
import com.twilio.video.Participant
import com.twilio.video.RemoteVideoTrack
import info.learncoding.twiliovideocall.ui.room.VideoTrackViewState

data class ParticipantViewState(
    @SerializedName("sid")
    val sid: String? = null,
    @SerializedName("identity")
    val identity: String? = null,
    @SerializedName("video_track")
    val videoTrack: VideoTrackViewState? = null,
    @SerializedName("is_muted")
    val isMuted: Boolean = false,
    @SerializedName("is_mirror")
    val isMirrored: Boolean = false,
    @SerializedName("is_local")
    val isLocalParticipant: Boolean = false,
    @SerializedName("network_quality")
    val networkQualityLevel: NetworkQualityLevel = NETWORK_QUALITY_LEVEL_UNKNOWN
) {

    fun getRemoteVideoTrack(): RemoteVideoTrack? =
        if (!isLocalParticipant) videoTrack?.videoTrack as RemoteVideoTrack? else null

    override fun toString(): String {
        return "ParticipantViewState(sid=$sid, identity=$identity, isMuted=$isMuted, isMirrored=$isMirrored, isLocalParticipant=$isLocalParticipant, networkQualityLevel=$networkQualityLevel)"
    }

}

fun buildParticipantViewState(participant: Participant, name: String?): ParticipantViewState {
    val videoTrack = participant.videoTracks.firstOrNull()?.videoTrack
    return ParticipantViewState(
        participant.sid,
        name ?: participant.identity,
        videoTrack?.let { VideoTrackViewState(it) },
        networkQualityLevel = participant.networkQualityLevel,
        isMuted = participant.audioTracks.firstOrNull() == null
    )
}
