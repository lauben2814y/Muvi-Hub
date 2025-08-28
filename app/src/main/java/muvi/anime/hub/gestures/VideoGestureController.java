package muvi.anime.hub.gestures;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.GestureDetectorCompat;

public class VideoGestureController {
    private final Activity activity;
    private final View playerView;
    private final VideoGestureListener gestureListener;
    private final BrightnessController brightnessController;
    private final VolumeController volumeController;
    private final GestureIndicatorView indicatorView;
    private final GestureDetectorCompat gestureDetector;

    public interface VideoGestureCallback {
        void onSeekForward(long milliseconds);
        void onSeekBackward(long milliseconds);
        void onTogglePlayPause();
        void onToggleControls();
        void onGestureStart();
        void onGestureEnd();
    }

    public VideoGestureController(Activity activity, View playerView, ViewGroup rootContainer, VideoGestureCallback callback) {
        this.activity = activity;
        this.playerView = playerView;

        // Initialize controllers
        this.brightnessController = new BrightnessController(activity);
        this.volumeController = new VolumeController(activity);
        this.indicatorView = new GestureIndicatorView(activity, rootContainer);
        this.gestureListener = new VideoGestureListener(callback, indicatorView, brightnessController, volumeController, playerView);
        this.gestureDetector = new GestureDetectorCompat(activity, gestureListener);

        setupTouchListener();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener() {
        playerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    public void setSeekDuration(long milliseconds) {
        gestureListener.setSeekDuration(milliseconds);
    }

    public void setSensitivity(float brightnessSensitivity, float volumeSensitivity) {
        gestureListener.setSensitivity(brightnessSensitivity, volumeSensitivity);
    }

    public void setGestureThreshold(int threshold) {
        gestureListener.setGestureThreshold(threshold);
    }

    public void destroy() {
        indicatorView.destroy();
    }
}

