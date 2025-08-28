package muvi.anime.hub.managers;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.google.android.gms.ads.MobileAds;

public class AdManagerCoordinator {
    private final Context context;
    private final ConsentManager consentManager;
    private final RewardedAdManager rewardedAdManager;
    private final InterstitialAdManager interstitialAdManager;
    private final BannerAdManager bannerAdManager;

    public AdManagerCoordinator(Context context) {
        this.context = context;
        this.rewardedAdManager = RewardedAdManager.getInstance(context);
        this.interstitialAdManager = InterstitialAdManager.getInstance(context);
        this.bannerAdManager = BannerAdManager.getInstance(context);

        // Initialize consent manager
        this.consentManager = ConsentManager.getInstance(context);
        this.consentManager.setConsentStatusListener(this::onConsentStatusChanged);
    }

    // Modify to take Activity parameter
    public void initialize(Activity activity) {
        // Initialize AdMob first
        MobileAds.initialize(context, initializationStatus -> Log.d("AdManagerCoordinator", "AdMob SDK initialized"));

        // Now initialize consent with Activity
        consentManager.initialize(activity);
    }

    private void onConsentStatusChanged(boolean canShowAds) {
        if (canShowAds) {
            Log.d("Muvi-Hub", "Consent obtained Starting ad loading ..,");
            // Now that we have consent, initialize all ad managers
             rewardedAdManager.initializeDownloadRewarded();
             interstitialAdManager.initializeInterstitial();
             bannerAdManager.initializeBannerAd();
        } else {
            Log.e("Muvi-Hub", "Cannot show ads due to consent status");
        }
    }
}
