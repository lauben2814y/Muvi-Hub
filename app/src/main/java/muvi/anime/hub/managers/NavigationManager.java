package muvi.anime.hub.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;

import muvi.anime.hub.data.Utils;
import muvi.anime.hub.ui.FullScreenPreloader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;

public class NavigationManager {
    private static NavigationManager instance;
    private final InterstitialAdManager interstitialAdManager;

    private NavigationManager(Context context) {
        this.interstitialAdManager = InterstitialAdManager.getInstance(context);
    }

    public static synchronized NavigationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NavigationManager(context);
        }
        return instance;
    }

    public void navigateWithAd(Activity currentActivity, Intent destination, FullScreenPreloader preloader) {
        // Initial validation
        if (currentActivity == null || currentActivity.isFinishing() || currentActivity.isDestroyed()) {
            Log.w(Utils.getTag(), "Cannot navigate: Activity is invalid");
            safelyDismissPreloader(preloader);
            return;
        }

        if (destination == null) {
            Log.w(Utils.getTag(), "Cannot navigate: Destination intent is null");
            safelyDismissPreloader(preloader);
            return;
        }

        if (interstitialAdManager.isAdLoaded()) {
            showAdAndNavigate(currentActivity, destination, preloader);
        } else {
            if (interstitialAdManager.isAdBlockerDetected()) {
                safelyDismissPreloader(preloader);
                showAdBlockerDetectedDialog(currentActivity, interstitialAdManager.getLastAdError());
            } else {
                // Proceed with navigation without ad
                safelyDismissPreloader(preloader);
                try {
                    currentActivity.startActivity(destination);
                } catch (Exception e) {
                    Log.e(Utils.getTag(), "Error starting activity: " + e.getMessage());
                }
            }
        }

//        final long MAX_WAIT_TIME = 5000; // 5 seconds
//        final long startTime = System.currentTimeMillis();
//
//        // Store weak reference to prevent memory leaks
//        final WeakReference<Activity> activityRef = new WeakReference<>(currentActivity);
//
//        final Handler handler = new Handler(Looper.getMainLooper());
//        final Runnable checkAdStatusRunnable = new Runnable() {
//            @Override
//            public void run() {
//                // Get the activity from weak reference and check if it's still valid
//                Activity activity = activityRef.get();
//                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
//                    Log.w(Utils.getTag(), "Activity is no longer valid, cancelling navigation");
//                    safelyDismissPreloader(preloader);
//                    return;
//                }
//
//                // Check for ad blocker first
//                if (interstitialAdManager.isAdBlockerDetected()) {
//                    safelyDismissPreloader(preloader);
//                    showAdBlockerDetectedDialog(activity, interstitialAdManager.getLastAdError());
//                    return;
//                }
//
//                // Check if ad is now loaded
//                if (interstitialAdManager.isAdLoaded()) {
//                    showAdAndNavigate(activity, destination, preloader);
//                    return;
//                }
//
//                // Check if we've exceeded the maximum wait time
//                if (System.currentTimeMillis() - startTime > MAX_WAIT_TIME) {
//                    Log.d(Utils.getTag(), "Max wait time exceeded for ad loading");
//
//                    if (interstitialAdManager.isAdBlockerDetected()) {
//                        safelyDismissPreloader(preloader);
//                        showAdBlockerDetectedDialog(activity, interstitialAdManager.getLastAdError());
//                    } else {
//                        // Proceed with navigation without ad
//                        safelyDismissPreloader(preloader);
//                        try {
//                            activity.startActivity(destination);
//                        } catch (Exception e) {
//                            Log.e(Utils.getTag(), "Error starting activity: " + e.getMessage());
//                        }
//                    }
//                    return;
//                }
//
//                // Continue checking every 500ms
//                handler.postDelayed(this, 500);
//            }
//        };
//
//        // Start the periodic check
//        handler.postDelayed(checkAdStatusRunnable, 500);
    }

    private void showAdBlockerDetectedDialog(Activity currentActivity, String lastAdError) {
        if (currentActivity == null || currentActivity.isFinishing() || currentActivity.isDestroyed()) {
            Log.w(Utils.getTag(), "Attempted to show dialog on invalid activity.");
            return;
        }

        try {
            AlertDialog dialog = new MaterialAlertDialogBuilder(currentActivity).setTitle("Ad Blocker Detected 🚫").setMessage("We've detected that you may be using an ad blocker or VPN that's " + "preventing our ads from loading. Please disable it and open the App again").setPositiveButton("OK", (dialogInterface, which) -> dialogInterface.dismiss()).setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss()).setCancelable(true).create();

            // Check one more time before showing
            if (!currentActivity.isFinishing() && !currentActivity.isDestroyed()) {
                dialog.show();
            }
        } catch (Exception e) {
            Log.e(Utils.getTag(), "Error showing ad blocker dialog: " + e.getMessage());
        }

        // Log the detailed error for diagnostics
        Log.d(Utils.getTag(), "Ad blocker detected with error: " + lastAdError);
    }

    private void showAdAndNavigate(Activity currentActivity, Intent destination, FullScreenPreloader preloader) {
        if (currentActivity == null || currentActivity.isFinishing() || currentActivity.isDestroyed()) {
            Log.w(Utils.getTag(), "Activity is no longer valid for ad navigation");
            safelyDismissPreloader(preloader);
            return;
        }

        if (destination == null) {
            Log.w(Utils.getTag(), "Destination intent is null, cannot navigate");
            safelyDismissPreloader(preloader);
            return;
        }

        safelyDismissPreloader(preloader);

        // Store weak reference for the callback
        final WeakReference<Activity> activityRef = new WeakReference<>(currentActivity);

        try {
            interstitialAdManager.showInterstitialAd(currentActivity, new InterstitialAdManager.OnInterstitialAdCallback() {
                @Override
                public void onAdDismissed() {
                    Activity activity = activityRef.get();
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        try {
                            Toast.makeText(activity, "Ad dismissed", Toast.LENGTH_SHORT).show();
                            activity.startActivity(destination);
                        } catch (Exception e) {
                            Log.e(Utils.getTag(), "Error navigating after ad dismissed: " + e.getMessage());
                        }
                    } else {
                        Log.w(Utils.getTag(), "Activity no longer valid after ad dismissed");
                    }
                }

                @Override
                public void onAdFailedToShow() {
                    Activity activity = activityRef.get();
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        try {
                            Toast.makeText(activity, "Ad dismissed", Toast.LENGTH_SHORT).show();
                            activity.startActivity(destination);
                        } catch (Exception e) {
                            Log.e(Utils.getTag(), "Error navigating after ad dismissed: " + e.getMessage());
                        }
                    } else {
                        Log.w(Utils.getTag(), "Activity no longer valid after ad dismissed");
                    }
                }

                @Override
                public void onAdNotAvailable() {
                    Activity activity = activityRef.get();
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        try {
                            Toast.makeText(activity, "Ad dismissed", Toast.LENGTH_SHORT).show();
                            activity.startActivity(destination);
                        } catch (Exception e) {
                            Log.e(Utils.getTag(), "Error navigating after ad dismissed: " + e.getMessage());
                        }
                    } else {
                        Log.w(Utils.getTag(), "Activity no longer valid after ad dismissed");
                    }
                }

                @Override
                public void onAdShowed() {
                    Log.d(Utils.getTag(), "Interstitial ad showed successfully");
                }

                @Override
                public void onAdBlockerDetected() {
                    Activity activity = activityRef.get();
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        showAdBlockerDetectedDialog(activity, interstitialAdManager.getLastAdError());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(Utils.getTag(), "Error showing interstitial ad: " + e.getMessage());
        }
    }

    /**
     * Safely dismiss the preloader without causing crashes
     */
    private void safelyDismissPreloader(FullScreenPreloader preloader) {
        if (preloader != null) {
            try {
                preloader.dismiss();
            } catch (Exception e) {
                Log.w(Utils.getTag(), "Error dismissing preloader: " + e.getMessage());
            }
        }
    }

    /**
     * Clean up method to be called when the NavigationManager is no longer needed
     */
    public static void cleanup() {
        instance = null;
    }
}