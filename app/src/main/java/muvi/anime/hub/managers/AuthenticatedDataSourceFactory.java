package muvi.anime.hub.managers;

import android.util.Base64;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;

import java.util.HashMap;
import java.util.Map;

import muvi.anime.hub.BuildConfig;

public class AuthenticatedDataSourceFactory {
    /**
     * Creates a DataSource.Factory with custom username and password
     * @param username Custom username
     * @param password Custom password
     * @return DataSource.Factory configured with authentication
     */
    @OptIn(markerClass = UnstableApi.class)
    public static DataSource.Factory create(String username, String password) {
        // Create authentication header
        String credentials = username + ":" + password;
        String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        // Create headers map
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", basicAuth);

        // Create and return DataSource factory with headers
        DefaultHttpDataSource.Factory factory = new DefaultHttpDataSource.Factory();
        factory.setDefaultRequestProperties(headers);
        factory.setAllowCrossProtocolRedirects(true);

        return factory;
    }
}
