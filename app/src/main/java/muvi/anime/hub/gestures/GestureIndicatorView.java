package muvi.anime.hub.gestures;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import muvi.anime.hub.R;

public class GestureIndicatorView {
    private final Context context;
    private final ViewGroup rootContainer;
    private final Handler handler;

    private FrameLayout overlayLayout;
    private TextView gestureText;
    private ImageView gestureIcon;
    private boolean isCreated = false;

    private final Runnable hideIndicator = () -> {
        if (overlayLayout != null) {
            overlayLayout.setVisibility(View.GONE);
        }
    };

    public GestureIndicatorView(Context context, ViewGroup rootContainer) {
        this.context = context;
        this.rootContainer = rootContainer;
        this.handler = new Handler(Looper.getMainLooper());
        createOverlay();
    }

    private void createOverlay() {
        if (isCreated) return;

        // Create main overlay
        overlayLayout = new FrameLayout(context);
        overlayLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        overlayLayout.setVisibility(View.GONE);
        overlayLayout.setBackgroundColor(Color.parseColor("#80000000"));

        // Create indicator container
        LinearLayout indicatorContainer = new LinearLayout(context);
        indicatorContainer.setOrientation(LinearLayout.VERTICAL);
        indicatorContainer.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.gravity = Gravity.CENTER;
        indicatorContainer.setLayoutParams(containerParams);

        // Create icon
        gestureIcon = new ImageView(context);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dpToPx(64), dpToPx(64)
        );
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        iconParams.bottomMargin = dpToPx(16);
        gestureIcon.setLayoutParams(iconParams);
        gestureIcon.setColorFilter(Color.WHITE);

        // Create text
        gestureText = new TextView(context);
        gestureText.setTextColor(Color.WHITE);
        gestureText.setTextSize(16);
        gestureText.setGravity(Gravity.CENTER);

        indicatorContainer.addView(gestureIcon);
        indicatorContainer.addView(gestureText);
        overlayLayout.addView(indicatorContainer);

        rootContainer.addView(overlayLayout);
        isCreated = true;
    }

    public void showBrightnessIndicator(int percentage) {
        showIndicator("Brightness: " + percentage + "%", R.drawable.brightness_6_24px);
    }

    public void showVolumeIndicator(VolumeController.VolumeInfo volumeInfo) {
        String text = volumeInfo.isMuted ? "Muted" : "Volume: " + volumeInfo.percentage + "%";
        showIndicator(text, volumeInfo.iconRes);
    }

    public void showSeekIndicator(boolean isBackward, long milliseconds) {
        int seconds = (int) (milliseconds / 1000);
        String text = (isBackward ? "- " : "+ ") + seconds + "s";
        int iconRes = isBackward ? R.drawable.replay_10_24px : R.drawable.forward_10_24px;
        showIndicator(text, iconRes);
    }

    private void showIndicator(String text, int iconRes) {
        if (overlayLayout == null || gestureText == null || gestureIcon == null) return;

        gestureText.setText(text);
        gestureIcon.setImageResource(iconRes);
        overlayLayout.setVisibility(View.VISIBLE);

        // Auto-hide after 1 second
        handler.removeCallbacks(hideIndicator);
        handler.postDelayed(hideIndicator, 1000);
    }

    public void hide() {
        if (overlayLayout != null) {
            overlayLayout.setVisibility(View.GONE);
        }
        handler.removeCallbacks(hideIndicator);
    }

    public void destroy() {
        handler.removeCallbacks(hideIndicator);
        if (overlayLayout != null && rootContainer != null) {
            rootContainer.removeView(overlayLayout);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}