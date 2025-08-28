package muvi.anime.hub.managers;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.net.HttpURLConnection;
import java.net.URL;

import muvi.anime.hub.data.Utils;

public class InterstitialAdManager {
    private static final String TAG = Utils.getTag();
    private static final int MAX_AD_LOAD_RETRIES = 3;
    private static InterstitialAdManager instance;

    private final Context context;
    private InterstitialAd interstitialAd;
    private boolean isInterstitialLoaded;
    private boolean isAdLoading = false;
    private int adLoadRetryCount = 1;

    // Enhanced ad blocker detection fields
    private boolean adBlockerDetected = false;
    private String lastAdError = "";
    private static final String[] AD_SERVER_DOMAINS = {
            "googleads.g.doubleclick.net",
            "pagead2.googlesyndication.com",
            "www.googleadservices.com"
    };

    // Add common ad blocker error patterns
    private static final String[] AD_BLOCKER_ERROR_PATTERNS = {
            "failed to connect",
            "connection refused",
            "0.0.0.0",
            "network error",
            "unable to obtain a javascript engine",  // Add the specific error you're seeing
            "javascript",
            "webview error",
            "script",
            "timeout",
            "resource blocked",
            "ad failed",
            "blocked by client"
    };

    private InterstitialAdManager(Context context) {
        this.context = context.getApplicationContext(); // use app context to prevent leaks
    }

    public static synchronized InterstitialAdManager getInstance(Context context) {
        if (instance == null) {
            instance = new InterstitialAdManager(context);
        }
        return instance;
    }

    public boolean isAdLoaded() {
        return isInterstitialLoaded && interstitialAd != null;
    }

    // New methods for ad blocker detection
    public boolean isAdBlockerDetected() {
        return adBlockerDetected;
    }

    public String getLastAdError() {
        return lastAdError;
    }

    public void resetAdBlockerStatus() {
        adBlockerDetected = false;
        lastAdError = "";
    }

    // Add a method to check ad blocker using WebView
    public void performAdBlockerCheck(final AdBlockerCheckCallback callback) {
        new Thread(() -> {
            try {
                // Use WebView to try loading ad domains as another detection method
                URL url = new URL("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.connect();
                int responseCode = connection.getResponseCode();
                connection.disconnect();

                // If we can't connect to ad servers, likely an ad blocker
                boolean adBlockerFound = responseCode != 200;

                // Run callback on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    adBlockerDetected = adBlockerFound;
                    if (adBlockerFound) {
                        lastAdError = "Ad resources blocked by network";
                    }
                    if (callback != null) {
                        callback.onCheckComplete(adBlockerFound);
                    }
                });
            } catch (Exception e) {
                // Exception likely means ad blocker is active
                new Handler(Looper.getMainLooper()).post(() -> {
                    adBlockerDetected = true;
                    lastAdError = "Exception when checking ad resources: " + e.getMessage();
                    if (callback != null) {
                        callback.onCheckComplete(true);
                    }
                });
            }
        }).start();
    }

    public interface AdBlockerCheckCallback {
        void onCheckComplete(boolean adBlockerDetected);
    }

    public void initializeInterstitial() {
        if (isAdLoading) {
            Log.d(TAG, "Ad is already loading, skipping initialization");
            return;
        }

        if (isAdLoaded()) {
            Log.d(TAG, "Ad is already loaded, skipping initialization");
            return;
        }

        isAdLoading = true;
        adLoadRetryCount = 0;

        // Reset ad blocker status on each new load attempt
        resetAdBlockerStatus();

        // Perform additional ad blocker check before loading
        performAdBlockerCheck(isBlocked -> {
            if (isBlocked) {
                Log.w(TAG, "Ad blocker detected for Interstitial during pre-check");
                isAdLoading = false;
                // Optionally notify user or take action here
            } else {
                 loadInterstitialAd();
            }
        });
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, "ca-app-pub-2115223186894781/1024203093", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Interstitial ad loading failed: " + loadAdError.getMessage());
                lastAdError = loadAdError.getMessage();
                String errorMsg = loadAdError.getMessage().toLowerCase();

                // Check if the error message contains ad server domain references
                for (String domain : AD_SERVER_DOMAINS) {
                    if (errorMsg.contains(domain.toLowerCase())) {
                        adBlockerDetected = true;
                        Log.w(TAG, "Ad blocker detected! Failed to connect to " + domain);
                        break;
                    }
                }

                // Enhanced pattern matching for ad blocker detection
                if (!adBlockerDetected) {
                    for (String pattern : AD_BLOCKER_ERROR_PATTERNS) {
                        if (errorMsg.contains(pattern.toLowerCase())) {
                            adBlockerDetected = true;
                            Log.w(TAG, "Ad blocker detected based on error pattern: " + pattern);
                            break;
                        }
                    }
                }

                // Check error code - certain codes are more likely to be ad blockers
                int errorCode = loadAdError.getCode();
                if (errorCode == 3 || errorCode == 0) {  // No fill (rare for test ads) or network error
                    Log.w(TAG, "Potential ad blocker based on error code: " + errorCode);
                    // Only mark as ad blocker if we also have a suspicious error message
                    if (errorMsg.contains("fail") || errorMsg.contains("error") ||
                            errorMsg.contains("unable") || errorMsg.contains("blocked")) {
                        adBlockerDetected = true;
                    }
                }

                interstitialAd = null;
                isInterstitialLoaded = false;
                isAdLoading = false;

                // Retry logic
                if (adLoadRetryCount < MAX_AD_LOAD_RETRIES) {
                    adLoadRetryCount++;
                    new Handler().postDelayed(() -> {
                        Log.d(TAG, "Retrying ad load, attempt " + adLoadRetryCount);
                        loadInterstitialAd();
                    }, 1000L * adLoadRetryCount); // Exponential backoff
                } else {
                    if (!adBlockerDetected && errorMsg.contains("failed")) {
                        adBlockerDetected = true;
                        Log.w(TAG, "Ad blocker suspected after multiple failed load attempts");
                    }
                }
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd newInterstitialAd) {
                interstitialAd = newInterstitialAd;
                isInterstitialLoaded = true;
                isAdLoading = false;
                adLoadRetryCount = 0;
                adBlockerDetected = false;  // Successful ad load means no ad blocker
                Log.d(TAG, "Interstitial ad loaded successfully");
            }
        });

    }

    public void showInterstitialAd(Activity activity, OnInterstitialAdCallback callback) {
        // Get reference to consent manager and check status
        ConsentManager consentManager = ConsentManager.getInstance(context);
        if (!consentManager.canShowAds()) {
            Toast.makeText(activity, "You need to consent refer to the community for help", Toast.LENGTH_SHORT).show();
            return;
        }

        // First check if ad blocker is detected
        if (adBlockerDetected) {
            Log.w(TAG, "Ad blocker detected, notifying user");
            // You can show a dialog here asking user to disable ad blocker
            Toast.makeText(activity, "Please disable your ad blocker to support this app", Toast.LENGTH_LONG).show();
            callback.onAdBlockerDetected(); // Add this to your callback interface
            return;
        }

        if (isAdLoaded()) {
            interstitialAd.show(activity);

            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    isInterstitialLoaded = false;
                    interstitialAd = null;
                    // start loading the next ad
                    initializeInterstitial();
                    callback.onAdFailedToShow();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    isInterstitialLoaded = false;
                    interstitialAd = null;
                    // start loading the next ad
                    initializeInterstitial();
                    callback.onAdShowed();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Important: This is when the user has finished watching the ad
                    callback.onAdDismissed();
                    initializeInterstitial();
                }
            });
        } else {
            callback.onAdNotAvailable();
            initializeInterstitial();
        }
    }

    public interface OnInterstitialAdCallback {
        void onAdDismissed();
        void onAdFailedToShow();
        void onAdNotAvailable();
        void onAdShowed();
        void onAdBlockerDetected();
    }
}
