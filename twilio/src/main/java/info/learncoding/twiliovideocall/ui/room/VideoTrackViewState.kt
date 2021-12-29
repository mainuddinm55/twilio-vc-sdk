package info.learncoding.twiliovideocall.ui.room

import com.google.gson.annotations.SerializedName
import com.twilio.video.VideoTrack

data class VideoTrackViewState(
    @SerializedName("video_track")
    @Transient
    val videoTrack: VideoTrack,
    @SerializedName("is_switched_off")
    val isSwitchedOff: Boolean = false
)
