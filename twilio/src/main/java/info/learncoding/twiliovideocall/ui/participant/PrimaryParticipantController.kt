package info.learncoding.twiliovideocall.ui.participant

import android.view.View
import android.widget.ImageView
import com.twilio.video.NetworkQualityLevel
import com.twilio.video.VideoTrack
import info.learncoding.twiliovideocall.R
import info.learncoding.twiliovideocall.ui.participant.ParticipantView.State
import info.learncoding.twiliovideocall.ui.room.VideoTrackViewState

internal class PrimaryParticipantController(
    private val primaryView: ParticipantView
) {
    private var primaryItem: Item? = null

    fun renderAsPrimary(
        identity: String?,
        videoTrack: VideoTrackViewState?,
        muted: Boolean,
        mirror: Boolean,
        networkQualityLevel: NetworkQualityLevel?
    ) {
        val old = primaryItem
        val newItem = Item(
            identity,
            videoTrack?.videoTrack,
            muted,
            mirror
        )
        primaryItem = newItem
        primaryView.setIdentity(newItem.identity)
        primaryView.setMuted(newItem.muted)
        primaryView.setMirror(newItem.mirror)
        val newVideoTrack = newItem.videoTrack

        // Only update sink for a new video track
        if (newVideoTrack != old?.videoTrack) {
            old?.let { removeSink(it.videoTrack, primaryView) }
            newVideoTrack?.let { if (it.isEnabled) it.addSink(primaryView.videoTextureView) }
        }

        newVideoTrack?.let {
            primaryView.setState(State.VIDEO)
        } ?: primaryView.setState(State.NO_VIDEO)

        primaryView.networkQualityLevelImg?.let {
            setNetworkQualityLevelImage(it, networkQualityLevel)
        }
    }

    private fun setNetworkQualityLevelImage(
        networkQualityImage: ImageView,
        networkQualityLevel: NetworkQualityLevel?
    ) {
        when (networkQualityLevel) {
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_ZERO -> R.drawable.twilio_network_quality_level_0
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_ONE -> R.drawable.twilio_network_quality_level_1
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_TWO -> R.drawable.twilio_network_quality_level_2
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_THREE -> R.drawable.twilio_network_quality_level_3
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_FOUR -> R.drawable.twilio_network_quality_level_4
            NetworkQualityLevel.NETWORK_QUALITY_LEVEL_FIVE -> R.drawable.twilio_network_quality_level_5
            else -> null
        }?.let { image ->
            networkQualityImage.visibility = View.VISIBLE
            networkQualityImage.setImageResource(image)
        } ?: run { networkQualityImage.visibility = View.GONE }
    }

    private fun removeSink(videoTrack: VideoTrack?, view: ParticipantView) {
        if (videoTrack == null || !videoTrack.sinks.contains(view.videoTextureView)) return
        videoTrack.removeSink(view.videoTextureView)
    }

    internal class Item(
        var identity: String?,
        var videoTrack: VideoTrack?,
        var muted: Boolean,
        var mirror: Boolean
    )
}
