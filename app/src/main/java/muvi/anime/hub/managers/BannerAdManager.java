package muvi.anime.hub.managers;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class BannerAdManager {
    private static final String TAG = "W&Q";
    private static final int MAX_AD_LOAD_RETRIES = 3;
    private static BannerAdManager instance;

    private final Context context;
    private AdView bannerAd;
    private boolean isBannerAdLoaded;
    private boolean isAdLoading = false;
    private int adLoadRetryCount = 0;
    private ViewGroup currentParent = null;

    // Add a flag to track if we've reached max retries
    private boolean maxRetriesReached = false;

    private BannerAdManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent memory leaks
    }

    public static synchronized BannerAdManager getInstance(Context context) {
        if (instance == null) {
            instance = new BannerAdManager(context);
        }
        return instance;
    }

    public boolean isAdLoaded() {
        return isBannerAdLoaded && bannerAd != null;
    }

    public void initializeBannerAd() {
        if (isAdLoading) {
            Log.d(TAG, "Banner ad is already loading, skipping initialization");
            return;
        }

        if (isAdLoaded()) {
            Log.d(TAG, "Banner ad is already loaded, skipping initialization");
            return;
        }

        // Don't try loading if we've already hit max retries
        if (maxRetriesReached) {
            Log.d(TAG, "Max retries already reached, not attempting to load banner ad");
            return;
        }

        isAdLoading = true;
        adLoadRetryCount = 0;
        loadBannerAd();
    }

    // Added method to reset retry status
    public void resetRetryStatus() {
        maxRetriesReached = false;
        adLoadRetryCount = 0;
        Log.d(TAG, "Banner ad retry status has been reset");
    }

    private void loadBannerAd() {
        // Don't try loading if we've already hit max retries
        if (maxRetriesReached) {
            Log.d(TAG, "Max retries already reached, not attempting to load banner ad");
            isAdLoading = false;
            return;
        }

        // Create a new banner ad
        // Important: Always create a new AdView to avoid parent issues
        destroyBannerAd(); // Destroy previous AdView if exists

        bannerAd = new AdView(context);
        bannerAd.setAdUnitId("ca-app-pub-2115223186894781/8455136408");
        bannerAd.setAdSize(AdSize.BANNER);

        // Set ad listener
        bannerAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                isBannerAdLoaded = true;
                isAdLoading = false;
                adLoadRetryCount = 0;
                maxRetriesReached = false; // Reset flag on successful load
                Log.d(TAG, "Banner ad loaded successfully");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.e(TAG, "Banner ad failed to load: " + loadAdError.getMessage());

                bannerAd = null;
                isBannerAdLoaded = false;
                isAdLoading = false;

                // Retry logic
                if (adLoadRetryCount < MAX_AD_LOAD_RETRIES) {
                    adLoadRetryCount++;
                    Log.d(TAG, "Retrying banner ad load, attempt " + adLoadRetryCount);

                    new Handler().postDelayed(() -> {
                        loadBannerAd();
                    }, 1000L * adLoadRetryCount); // Exponential backoff
                } else {
                    // Set flag to prevent further retries
                    maxRetriesReached = true;
                    Log.d(TAG, "Max retry attempts reached, stopping banner ad loading");
                }
            }
        });

        // Load the ad
        AdRequest adRequest = new AdRequest.Builder().build();
        bannerAd.loadAd(adRequest);
    }

    public void displayBannerAd(ViewGroup container, OnBannerAdCallback callback) {
        if (container == null) {
            Log.e(TAG, "Container is null, cannot display banner ad");
            if (callback != null) {
                callback.onBannerAdNotAvailable();
            }
            return;
        }

        // Always clear the container first
        container.removeAllViews();

        if (isAdLoaded()) {
            // Set a new ad listener that will notify through the callback
            bannerAd.setAdListener(new AdListener() {
                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    if (callback != null) {
                        callback.onBannerAdClicked();
                    }
                }

                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    if (callback != null) {
                        callback.onBannerAdClosed();
                    }
                }
            });

            // Check if the AdView is already attached to a parent
            if (bannerAd.getParent() != null) {
                ((ViewGroup) bannerAd.getParent()).removeView(bannerAd);
            }

            // Add the ad to the container
            container.addView(bannerAd);
            currentParent = container;

            if (callback != null) {
                callback.onBannerAdAvailable();
            }
        } else {
            // Ad is not loaded yet
            if (callback != null) {
                callback.onBannerAdNotAvailable();
            }

            // Try to load it if not already loading and max retries not reached
            if (!isAdLoading && !maxRetriesReached) {
                initializeBannerAd();
            }
        }
    }

    public void destroyBannerAd() {
        if (bannerAd != null) {
            // First, remove from parent if attached
            if (bannerAd.getParent() != null) {
                ((ViewGroup) bannerAd.getParent()).removeView(bannerAd);
            }

            // Destroy the ad
            bannerAd.destroy();
            bannerAd = null;
            isBannerAdLoaded = false;
            currentParent = null;
        }
    }

    public interface OnBannerAdCallback {
        void onBannerAdAvailable();
        void onBannerAdNotAvailable();
        void onBannerAdClicked();
        void onBannerAdClosed();
    }
}
