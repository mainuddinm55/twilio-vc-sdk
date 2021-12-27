package info.learncoding.twiliovideocall.ui.room

import com.twilio.video.VideoTrack

data class VideoTrackViewState(
    val videoTrack: VideoTrack,
    val isSwitchedOff: Boolean = false
)
