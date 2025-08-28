package muvi.anime.hub.gestures;

import android.app.Activity;
import android.view.WindowManager;

public class BrightnessController {
    private final Activity activity;
    private final WindowManager.LayoutParams layoutParams;
    private float currentBrightness;

    public BrightnessController(Activity activity) {
        this.activity = activity;
        this.layoutParams = activity.getWindow().getAttributes();
        this.currentBrightness = layoutParams.screenBrightness;

        if (currentBrightness < 0) {
            currentBrightness = 0.5f; // Default brightness
        }
    }

    public int adjustBrightness(float delta) {
        currentBrightness += delta;
        currentBrightness = Math.max(0.01f, Math.min(1.0f, currentBrightness));

        layoutParams.screenBrightness = currentBrightness;
        activity.getWindow().setAttributes(layoutParams);

        return (int) (currentBrightness * 100);
    }

    public float getCurrentBrightness() {
        return currentBrightness;
    }

    public void setBrightness(float brightness) {
        currentBrightness = Math.max(0.01f, Math.min(1.0f, brightness));
        layoutParams.screenBrightness = currentBrightness;
        activity.getWindow().setAttributes(layoutParams);
    }
}
