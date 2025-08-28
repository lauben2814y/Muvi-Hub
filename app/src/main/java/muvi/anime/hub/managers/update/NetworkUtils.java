package muvi.anime.hub.managers.update;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiNetwork != null && wifiNetwork.isConnected();
        }
    }

    public static boolean isCellularConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            NetworkInfo cellularNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return cellularNetwork != null && cellularNetwork.isConnected();
        }
    }

    public static boolean shouldAllowDownload(Context context, UpdatePreferences prefs) {
        if (!isNetworkAvailable(context)) {
            if (UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "No network available");
            }
            return false;
        }

        if (prefs.isWifiOnlyEnabled()) {
            boolean wifiConnected = isWifiConnected(context);
            if (UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "WiFi only enabled, WiFi connected: " + wifiConnected);
            }
            return wifiConnected;
        }

        if (UpdateConfig.DEBUG_UPDATES) {
            Log.d(TAG, "Network available, WiFi only disabled - allowing download");
        }
        return true;
    }

    public static String getNetworkType(Context context) {
        if (isWifiConnected(context)) {
            return "WiFi";
        } else if (isCellularConnected(context)) {
            return "Cellular";
        } else if (isNetworkAvailable(context)) {
            return "Other";
        } else {
            return "None";
        }
    }

    public static boolean isMeteredConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return cm.isActiveNetworkMetered();
        } else {
            // On older versions, assume cellular is metered
            return isCellularConnected(context);
        }
    }
}
