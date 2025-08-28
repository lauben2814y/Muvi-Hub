package muvi.anime.hub.managers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkMonitor {
    private static final String TAG = "NetworkMonitor";
    private static NetworkMonitor instance;
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final List<NetworkCallback> callbacks = new CopyOnWriteArrayList<>();

    // Single network callback instance to avoid multiple registrations
    private ConnectivityManager.NetworkCallback systemNetworkCallback;
    private boolean isMonitoring = false;

    private NetworkMonitor(Context context) {
        this.context = context.getApplicationContext(); // Use application context to avoid memory leaks
        this.connectivityManager =
                (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }

    public synchronized void startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "Network monitoring already started");
            return;
        }

        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager is null, cannot start monitoring");
            return;
        }

        try {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build();

            systemNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Log.d(TAG, "Network available: " + network);
                    notifyCallbacks(true);
                }

                @Override
                public void onLost(Network network) {
                    Log.d(TAG, "Network lost: " + network);
                    notifyCallbacks(false);
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    Log.d(TAG, "Network capabilities changed. Has internet: " + hasInternet);
                    notifyCallbacks(hasInternet);
                }
            };

            connectivityManager.registerNetworkCallback(networkRequest, systemNetworkCallback);
            isMonitoring = true;
            Log.d(TAG, "Network monitoring started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start network monitoring: " + e.getMessage());
            systemNetworkCallback = null;
        }
    }

    /**
     * Ensures monitoring is started when the first callback is added
     */
    private void ensureMonitoringStarted() {
        if (!isMonitoring) {
            startMonitoring();
        }
    }

    public synchronized void stopMonitoring() {
        if (!isMonitoring || systemNetworkCallback == null) {
            Log.d(TAG, "Network monitoring not active or already stopped");
            return;
        }

        try {
            if (connectivityManager != null) {
                connectivityManager.unregisterNetworkCallback(systemNetworkCallback);
                Log.d(TAG, "Network monitoring stopped successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping network monitoring: " + e.getMessage());
        } finally {
            systemNetworkCallback = null;
            isMonitoring = false;
        }
    }

    public boolean isNetworkAvailable() {
        if (connectivityManager == null) {
            return false;
        }

        try {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability: " + e.getMessage());
            return false;
        }
    }

    public void addCallback(NetworkCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            ensureMonitoringStarted(); // Auto-start monitoring when first callback is added
            Log.d(TAG, "Network callback added. Total callbacks: " + callbacks.size());
        }
    }

    public void removeCallback(NetworkCallback callback) {
        if (callback != null) {
            boolean removed = callbacks.remove(callback);
            if (removed) {
                Log.d(TAG, "Network callback removed. Total callbacks: " + callbacks.size());

                // Stop monitoring if no more callbacks (optional - saves resources)
                if (callbacks.isEmpty()) {
                    stopMonitoring();
                }
            }
        }
    }

    public void removeAllCallbacks() {
        int callbackCount = callbacks.size();
        callbacks.clear();
        Log.d(TAG, "All network callbacks removed. Previous count: " + callbackCount);
    }

    private void notifyCallbacks(boolean isAvailable) {
        if (callbacks.isEmpty()) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // Use a copy to avoid ConcurrentModificationException
                List<NetworkCallback> callbacksCopy = new ArrayList<>(callbacks);
                for (NetworkCallback callback : callbacksCopy) {
                    try {
                        callback.onNetworkStateChanged(isAvailable);
                    } catch (Exception e) {
                        Log.e(TAG, "Error notifying network callback: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in notifyCallbacks: " + e.getMessage());
            }
        });
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }

    /**
     * Clean up resources when the NetworkMonitor is no longer needed
     */
    public void cleanup() {
        stopMonitoring();
        removeAllCallbacks();
    }

    public interface NetworkCallback {
        void onNetworkStateChanged(boolean isAvailable);
    }
}