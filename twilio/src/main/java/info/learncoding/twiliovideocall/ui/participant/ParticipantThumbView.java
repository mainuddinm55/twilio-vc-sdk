package info.learncoding.twiliovideocall.ui.participant;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.content.ContextCompat;

import info.learncoding.twiliovideocall.R;
import info.learncoding.twiliovideocall.databinding.TwilioParticipantViewBinding;

public class ParticipantThumbView extends ParticipantView {
    private TwilioParticipantViewBinding binding;

    public ParticipantThumbView(Context context) {
        super(context);
        init(context);
    }

    public ParticipantThumbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ParticipantThumbView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ParticipantThumbView(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        binding = TwilioParticipantViewBinding.inflate(LayoutInflater.from(context), this, true);
        videoLayout = binding.videoLayout;
        videoView = binding.video;
        selectedLayout = binding.selectedLayout;
        stubImage = binding.stub;
        networkQualityLevelImg = binding.networkQuality;
        selectedIdentity = binding.selectedIdentity;
        audioToggle = binding.audioToggle;
        setIdentity(identity);
        setState(state);
        setMirror(mirror);
        setScaleType(scaleType);
    }

    @Override
    public void setState(int state) {
        super.setState(state);
        int resId = R.drawable.twilio_participant_background;
        selectedLayout.setBackground(ContextCompat.getDrawable(getContext(), resId));
    }

    private int isSwitchOffViewVisible(int state) {
        return state == State.SWITCHED_OFF ? View.VISIBLE : View.GONE;
    }
}
