package info.learncoding.twiliovideocall.ui.participant;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.twilio.video.VideoScaleType;
import com.twilio.video.VideoTextureView;
import com.twilio.video.VideoTrack;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import info.learncoding.twiliovideocall.R;

public abstract class ParticipantView extends FrameLayout {

    private static final VideoScaleType DEFAULT_VIDEO_SCALE_TYPE = VideoScaleType.ASPECT_FIT;

    String identity = "";
    int state = State.NO_VIDEO;
    boolean mirror = false;
    int scaleType = DEFAULT_VIDEO_SCALE_TYPE.ordinal();

    VideoTrack videoTrack;
    ConstraintLayout videoLayout;
    VideoTextureView videoView;
    RelativeLayout selectedLayout;
    ImageView stubImage;
    @Nullable
    ImageView networkQualityLevelImg;
    TextView selectedIdentity;
    @Nullable
    ImageView audioToggle;

    public ParticipantView(@NonNull Context context) {
        super(context);
        initParams(context, null);
    }

    public ParticipantView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initParams(context, attrs);
    }

    public ParticipantView(
            @NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initParams(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    ParticipantView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr,
            @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initParams(context, attrs);
    }

    public void setIdentity(String identity) {
        this.identity = identity;
        selectedIdentity.setText(identity);
    }

    public void setState(int state) {
        this.state = state;
        switch (state) {
            case State.SWITCHED_OFF:
            case State.VIDEO:
                videoState();
                break;
            case State.NO_VIDEO:
                videoLayout.setVisibility(GONE);
                videoView.setVisibility(GONE);

                selectedLayout.setVisibility(VISIBLE);
                stubImage.setVisibility(VISIBLE);
                selectedIdentity.setVisibility(VISIBLE);
                break;
            default:
                break;
        }
    }

    private void videoState() {
        selectedLayout.setVisibility(GONE);
        stubImage.setVisibility(GONE);
        selectedIdentity.setVisibility(GONE);

        videoLayout.setVisibility(VISIBLE);
        videoView.setVisibility(VISIBLE);
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
        videoView.setMirror(this.mirror);
    }

    void setScaleType(int scaleType) {
        this.scaleType = scaleType;
        videoView.setVideoScaleType(VideoScaleType.values()[this.scaleType]);
    }

    public void setMuted(boolean muted) {
        if (audioToggle != null) audioToggle.setVisibility(muted ? VISIBLE : GONE);
    }

    public VideoTextureView getVideoTextureView() {
        return videoView;
    }

    void initParams(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray stylables = context.getTheme().obtainStyledAttributes(attrs, R.styleable.twilio_ParticipantView, 0, 0);

            // obtain identity
            int identityResId = stylables.getResourceId(R.styleable.twilio_ParticipantView_twilio_identity, -1);
            identity = (identityResId != -1) ? context.getString(identityResId) : "";

            // obtain state
            state = stylables.getInt(R.styleable.twilio_ParticipantView_twilio_state, State.NO_VIDEO);

            // obtain mirror
            mirror = stylables.getBoolean(R.styleable.twilio_ParticipantView_twilio_mirror, false);

            // obtain scale type
            scaleType = stylables.getInt(R.styleable.twilio_ParticipantView_twilio_type, DEFAULT_VIDEO_SCALE_TYPE.ordinal());

            stylables.recycle();
        }
    }

    @IntDef({
            State.VIDEO,
            State.NO_VIDEO,
            State.SWITCHED_OFF
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
        int VIDEO = 0;
        int NO_VIDEO = 1;
        int SWITCHED_OFF = 2;
    }
}
