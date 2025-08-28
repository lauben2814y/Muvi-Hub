package muvi.anime.hub.gestures;

import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class VideoGestureListener extends GestureDetector.SimpleOnGestureListener {
    private final VideoGestureController.VideoGestureCallback callback;
    private final GestureIndicatorView indicatorView;
    private final BrightnessController brightnessController;
    private final VolumeController volumeController;
    private final View playerView;

    private long seekDuration = 10000; // 10 seconds default
    private float brightnessSensitivity = 0.0005f; // Much slower (was 0.002f)
    private float volumeSensitivity = 0.003f; // Much slower (was 0.01f)
    private int gestureThreshold = 50;

    private boolean isScrolling = false;
    private boolean isAdjustingBrightness = false;
    private boolean isAdjustingVolume = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable resetBrightnessFlag;
    private Runnable resetVolumeFlag;

    public VideoGestureListener(VideoGestureController.VideoGestureCallback callback,
                                GestureIndicatorView indicatorView,
                                BrightnessController brightnessController,
                                VolumeController volumeController,
                                View playerView) {
        this.callback = callback;
        this.indicatorView = indicatorView;
        this.brightnessController = brightnessController;
        this.volumeController = volumeController;
        this.playerView = playerView;

        // Initialize runnables after callback is set
        this.resetBrightnessFlag = () -> {
            isAdjustingBrightness = false;
            callback.onGestureEnd();
        };
        this.resetVolumeFlag = () -> {
            isAdjustingVolume = false;
            callback.onGestureEnd();
        };
    }

    @Override
    public boolean onDown(MotionEvent e) {
        isScrolling = false;
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        float screenWidth = playerView.getWidth();
        float tapX = e.getX();

        if (tapX < screenWidth * 0.4f) {
            // Left side - seek backward
            callback.onSeekBackward(seekDuration);
            indicatorView.showSeekIndicator(true, seekDuration);
        } else if (tapX > screenWidth * 0.6f) {
            // Right side - seek forward
            callback.onSeekForward(seekDuration);
            indicatorView.showSeekIndicator(false, seekDuration);
        } else {
            // Center - toggle play/pause
            callback.onTogglePlayPause();
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (e1 == null || e2 == null) return false;

        float deltaY = e2.getY() - e1.getY();

        if (Math.abs(deltaY) > gestureThreshold && Math.abs(deltaY) > Math.abs(e2.getX() - e1.getX())) {
            float screenWidth = playerView.getWidth();

            if (!isAdjustingBrightness && !isAdjustingVolume) {
                callback.onGestureStart();
            }

            if (e1.getX() < screenWidth / 2) {
                // Left side - volume control (swapped)
                adjustVolume(-deltaY);
            } else {
                // Right side - brightness control (swapped)
                adjustBrightness(-deltaY);
            }
            isScrolling = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (!isScrolling && !isAdjustingBrightness && !isAdjustingVolume) {
            callback.onToggleControls();
        }
        return true;
    }

    private void adjustBrightness(float delta) {
        if (!isAdjustingBrightness) {
            isAdjustingBrightness = true;
        }

        int brightnessPercent = brightnessController.adjustBrightness(delta * brightnessSensitivity);
        indicatorView.showBrightnessIndicator(brightnessPercent); // Show on right side

        handler.removeCallbacks(resetBrightnessFlag);
        handler.postDelayed(resetBrightnessFlag, 500);
    }

    private void adjustVolume(float delta) {
        if (!isAdjustingVolume) {
            isAdjustingVolume = true;
        }

        VolumeController.VolumeInfo volumeInfo = volumeController.adjustVolume(delta * volumeSensitivity);
        indicatorView.showVolumeIndicator(volumeInfo); // Show on left side

        handler.removeCallbacks(resetVolumeFlag);
        handler.postDelayed(resetVolumeFlag, 500);
    }

    // Setters for configuration
    public void setSeekDuration(long milliseconds) {
        this.seekDuration = milliseconds;
    }

    public void setSensitivity(float brightnessSensitivity, float volumeSensitivity) {
        this.brightnessSensitivity = brightnessSensitivity;
        this.volumeSensitivity = volumeSensitivity;
    }

    public void setGestureThreshold(int threshold) {
        this.gestureThreshold = threshold;
    }
}