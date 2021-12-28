package info.learncoding.twiliovideocall.ui.participant

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import info.learncoding.twiliovideocall.databinding.TwilioParticipantPrimaryViewBinding

internal class ParticipantPrimaryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ParticipantView(context, attrs, defStyleAttr) {

    private val binding: TwilioParticipantPrimaryViewBinding =
        TwilioParticipantPrimaryViewBinding.inflate(
            LayoutInflater.from(context), this, true
        )

    init {
        videoLayout = binding.videoLayout
        videoView = binding.video
        selectedLayout = binding.selectedLayout
        stubImage = binding.stub
        selectedIdentity = binding.selectedIdentity
        networkQualityLevelImg = binding.networkQuality
        audioToggle = binding.audioToggle
        setIdentity(identity)
        setState(state)
        setMirror(mirror)
        setScaleType(scaleType)
    }
}
