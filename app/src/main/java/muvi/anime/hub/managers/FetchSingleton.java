package muvi.anime.hub.managers;

import android.content.Context;

import androidx.annotation.NonNull;

import com.tonyodev.fetch2.DefaultFetchNotificationManager;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;

public class FetchSingleton {
    private static Fetch fetchInstance;

    public static Fetch getFetchInstance(Context context) {
        if (fetchInstance == null) {
            FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(context)
                    .setDownloadConcurrentLimit(4)
                    .enableLogging(true)
                    .enableRetryOnNetworkGain(true)
                    .setNotificationManager(null)
                    .build();

            fetchInstance = Fetch.Impl.getInstance(fetchConfiguration);
        }
        return fetchInstance;
    }
}
