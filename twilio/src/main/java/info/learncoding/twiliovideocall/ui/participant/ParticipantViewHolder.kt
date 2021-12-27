package info.learncoding.twiliovideocall.ui.participant

import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.twilio.video.NetworkQualityLevel
import com.twilio.video.NetworkQualityLevel.NETWORK_QUALITY_LEVEL_FIVE
import com.twilio.video.NetworkQualityLevel.NETWORK_QUALITY_LEVEL_FOUR
import com.twilio.video.NetworkQualityLevel.NETWORK_QUALITY_LEVEL_ONE
import com.twilio.video.NetworkQualityLevel.NETWORK_QUALITY_LEVEL_THREE
import com.twilio.video.NetworkQualityLevel.NETWORK_QUALITY_LEVEL_TWO
import com.twilio.video.NetworkQualityLevel.NETWORK_QUALITY_LEVEL_ZERO
import com.twilio.video.VideoTrack
import info.learncoding.twiliovideocall.R
import info.learncoding.twiliovideocall.ui.room.VideoTrackViewState

private const val TAG = "ParticipantViewHolder"

internal class ParticipantViewHolder(private val thumb: ParticipantThumbView) :
    RecyclerView.ViewHolder(thumb) {

    private val localParticipantIdentity = thumb.context.getString(R.string.twilio_you)

    fun bind(participantViewState: ParticipantViewState) {
        Log.d(TAG, "bind ParticipantViewHolder with data item: $participantViewState")
        Log.d(TAG, "thumb: $thumb")

        thumb.run {
            val identity = if (participantViewState.isLocalParticipant)
                localParticipantIdentity else participantViewState.identity
            setIdentity(identity)
            setMuted(participantViewState.isMuted)

            updateVideoTrack(participantViewState)

            networkQualityLevelImg?.let {
                setNetworkQualityLevelImage(it, participantViewState.networkQualityLevel)
            }
        }
    }

    private fun updateVideoTrack(participantViewState: ParticipantViewState) {
        thumb.run {
            val videoTrackViewState = participantViewState.videoTrack
            val newVideoTrack = videoTrackViewState?.videoTrack
            if (videoTrack !== newVideoTrack) {
                removeSink(videoTrack, this)
                videoTrack = newVideoTrack
                videoTrack?.let { videoTrack ->
                    setVideoState(videoTrackViewState)
                    if (videoTrack.isEnabled) videoTrack.addSink(this.videoTextureView)
                } ?: setState(ParticipantView.State.NO_VIDEO)
            } else {
                setVideoState(videoTrackViewState)
            }
        }
    }

    private fun ParticipantThumbView.setVideoState(videoTrackViewState: VideoTrackViewState?) {
        if (videoTrackViewState?.isSwitchedOff == true) {
            setState(ParticipantView.State.SWITCHED_OFF)
        } else {
            videoTrackViewState?.videoTrack?.let { setState(ParticipantView.State.VIDEO) }
                ?: setState(ParticipantView.State.NO_VIDEO)
        }
    }

    private fun removeSink(videoTrack: VideoTrack?, view: ParticipantView) {
        if (videoTrack == null || !videoTrack.sinks.contains(view.videoTextureView)) return
        videoTrack.removeSink(view.videoTextureView)
    }

    private fun setNetworkQualityLevelImage(
        networkQualityImage: ImageView,
        networkQualityLevel: NetworkQualityLevel?
    ) {
        when (networkQualityLevel) {
            NETWORK_QUALITY_LEVEL_ZERO -> R.drawable.twilio_network_quality_level_0
            NETWORK_QUALITY_LEVEL_ONE -> R.drawable.twilio_network_quality_level_1
            NETWORK_QUALITY_LEVEL_TWO -> R.drawable.twilio_network_quality_level_2
            NETWORK_QUALITY_LEVEL_THREE -> R.drawable.twilio_network_quality_level_3
            NETWORK_QUALITY_LEVEL_FOUR -> R.drawable.twilio_network_quality_level_4
            NETWORK_QUALITY_LEVEL_FIVE -> R.drawable.twilio_network_quality_level_5
            else -> null
        }?.let { image ->
            networkQualityImage.visibility = View.VISIBLE
            networkQualityImage.setImageResource(image)
        } ?: run { networkQualityImage.visibility = View.GONE }
    }
}
