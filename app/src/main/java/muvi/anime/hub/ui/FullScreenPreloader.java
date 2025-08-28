package muvi.anime.hub.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

import muvi.anime.hub.R;

public class FullScreenPreloader {
    public final Dialog dialog;
    private final WeakReference<Context> contextReference;

    public FullScreenPreloader(Context context) {
        this.contextReference = new WeakReference<>(context);
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.fullscreen_preloader);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);

            // Add these lines to extend behind the notch
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                window.setAttributes(layoutParams);

                window.setFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                );
            }
        }
    }

    public void show() {
        try {
            Context context = contextReference.get();
            if (context instanceof Activity activity) {
                if (!activity.isFinishing() && !activity.isDestroyed() && !dialog.isShowing()) {
                    dialog.show();
                }
            } else if (!dialog.isShowing()) {
                dialog.show();
            }
        } catch (Exception e) {
            Log.e("FullScreenPreloader", "Error showing dialog: " + e.getMessage());
        }
    }

    public void dismiss() {
        try {
            Context context = contextReference.get();
            if (context instanceof Activity activity) {
                if (!activity.isFinishing() && !activity.isDestroyed() && dialog.isShowing()) {
                    dialog.dismiss();
                }
            } else if (dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            Log.e("FullScreenPreloader", "Error dismissing dialog: " + e.getMessage());
        }
    }
}
