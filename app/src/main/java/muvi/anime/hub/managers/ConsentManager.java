package muvi.anime.hub.managers;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class ConsentManager {
    private static final String TAG = "Muvi-Hub";
    private final Context context;
    private ConsentInformation consentInformation;
    private ConsentForm consentForm;
    private ConsentStatusListener listener;
    private static ConsentManager instance;

    // Interface to notify when consent status changes
    public interface ConsentStatusListener {
        void onConsentStatusChanged(boolean canShowAds);
    }

    private ConsentManager(Context context) {
        this.context = context;
    }

    public static synchronized ConsentManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConsentManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setConsentStatusListener(ConsentStatusListener listener) {
        this.listener = listener;
    }

    // Change this method to take an Activity parameter
    public void initialize(Activity activity) {
        // Set tag for underage of consent. false means users are not underage.
        ConsentRequestParameters params = new ConsentRequestParameters
                .Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(context);
        consentInformation.requestConsentInfoUpdate(
                activity,  // Pass the Activity here, not context
                params,
                () -> {
                    // The consent information state was updated
                    if (consentInformation.isConsentFormAvailable()) {
                        loadForm(activity);
                    } else {
                        notifyConsentStatus();
                    }
                },
                formError -> {
                    // Handle the error
                    Log.e("ConsentManager", "Error requesting consent info: " + formError.getMessage());
                    notifyConsentStatus();
                });
    }

    // Update this to accept Activity
    private void loadForm(Activity activity) {
        UserMessagingPlatform.loadConsentForm(
                context,
                form -> {
                    this.consentForm = form;
                    if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                        showForm(activity);
                    } else {
                        notifyConsentStatus();
                    }
                },
                formError -> {
                    // Handle the error
                    Log.e("ConsentManager", "Error loading consent form: " + formError.getMessage());
                    notifyConsentStatus();
                }
        );
    }

    // Update to take an Activity parameter
    public void showForm(Activity activity) {
        if (consentForm != null) {
            consentForm.show(
                    activity,
                    formError -> {
                        if (formError != null) {
                            Log.e("ConsentManager", "Error showing consent form: " + formError.getMessage());
                        }
                        notifyConsentStatus();
                    });
        }
    }

    public boolean canShowAds() {
        if (consentInformation == null) {
            return false;
        }

        int status = consentInformation.getConsentStatus();
        return status == ConsentInformation.ConsentStatus.OBTAINED ||
                status == ConsentInformation.ConsentStatus.NOT_REQUIRED;
    }

    private void notifyConsentStatus() {
        if (listener != null) {
            listener.onConsentStatusChanged(canShowAds());
        }
    }

    public void reset() {
        if (consentInformation != null) {
            consentInformation.reset();
        }
    }
}
