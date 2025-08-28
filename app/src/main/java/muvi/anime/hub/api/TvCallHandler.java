package muvi.anime.hub.api;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;


import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import muvi.anime.hub.managers.NetworkMonitor;
import muvi.anime.hub.adapters.tv.DbSectionAdapter;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.data.tv.SupabaseTvSection;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TvCallHandler {
    private final Activity activity;
    private final DbSectionAdapter dbSectionAdapter;
    private final NetworkMonitor networkMonitor;
    private final NetworkMonitor.NetworkCallback networkCallback;
    private final Map<String, PendingApiCall> pendingCalls = new HashMap<>();
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 5000; // 5 seconds


    public TvCallHandler(Activity activity, DbSectionAdapter dbSectionAdapter) {
        this.activity = activity;
        this.dbSectionAdapter = dbSectionAdapter;
        this.networkMonitor = NetworkMonitor.getInstance(activity); // Use singleton pattern

        // Store the callback reference for proper cleanup
        this.networkCallback = isAvailable -> {
            if (isAvailable) {
                retryPendingCalls();
            }
        };

        setupNetworkMonitoring();
    }

    private void setupNetworkMonitoring() {
        networkMonitor.addCallback(networkCallback);
    }

    public interface TvSectionCallback {
        void onSuccess(List<SupabaseTv> tvs);
    }

    private static class PendingApiCall {
        final Call<List<SupabaseTv>> call;
        final String sectionTitle;
        final TvSectionCallback callback;
        int retryCount = 0;

        private PendingApiCall(Call<List<SupabaseTv>> call, String sectionTitle, TvSectionCallback callback) {
            this.call = call;
            this.sectionTitle = sectionTitle;
            this.callback = callback;
        }
    }

    public void fetchTvSection(Call<List<SupabaseTv>> call, String sectionTitle, TvSectionCallback nextAction) {
        if (!networkMonitor.isNetworkAvailable()) {
            handleNetwork(call, sectionTitle, nextAction);
            return;
        }

        dbSectionAdapter.addPreloader();
        executeCall(new PendingApiCall(call, sectionTitle, nextAction));
    }

    private void executeCall(PendingApiCall pendingCall) {
        Log.d("Muvi-Hub", "Executing call for section: " + pendingCall.sectionTitle);

        pendingCall.call.clone().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseTv>> call, @NonNull Response<List<SupabaseTv>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (isActivityValid()) {
                        activity.runOnUiThread(() -> {
                            List<SupabaseTv> tvs = response.body();
                            SupabaseTvSection section = new SupabaseTvSection(pendingCall.sectionTitle, tvs);

                            dbSectionAdapter.addSupabaseTvSection(section);

                            if (pendingCall.callback != null) {
                                pendingCall.callback.onSuccess(tvs);
                            }

                            // Remove from pending calls if it was there
                            pendingCalls.remove(pendingCall.sectionTitle);

                        });
                    }
                } else {
                    Log.d("Muvi-Hub", "Error: " + response.code());
                    Log.d("Muvi-Hub", "Error message: " + response.message());
                    Log.d("Muvi-Hub", "Error body: " + response.errorBody());
                    Log.d("Muvi-Hub", "Error raw: " + response.raw());
                    handleFailure(pendingCall, new Exception("Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseTv>> call, @NonNull Throwable throwable) {
                Log.e("Muvi-Hub", "Error: " + throwable.getMessage());
                Log.e("Muvi-Hub", "Error cause: " + throwable.getCause());
                Log.e("Muvi-Hub", "Error stack trace: " + Log.getStackTraceString(throwable));
                handleFailure(pendingCall, throwable);
            }
        });
    }

    private void handleFailure(PendingApiCall pendingCall, Throwable throwable) {
        if (isActivityValid()) {
            activity.runOnUiThread(() -> {
                System.err.println("Error: " + throwable.getMessage());

                if (pendingCall.retryCount < MAX_RETRIES && networkMonitor.isNetworkAvailable()) {
                    scheduleRetry(pendingCall);
                } else {
                    // Store for later retry when network becomes available
                    pendingCalls.put(pendingCall.sectionTitle, pendingCall);
                    showRetrySnackbar(pendingCall);
                }
            });
        }
    }

    private void scheduleRetry(PendingApiCall pendingCall) {
        pendingCall.retryCount++;
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> executeCall(pendingCall),
                (long) RETRY_DELAY_MS * pendingCall.retryCount
        );
    }

    private void handleNetwork(Call<List<SupabaseTv>> call, String sectionTitle, TvSectionCallback callback) {
        PendingApiCall pendingApiCall = new PendingApiCall(call, sectionTitle, callback);
        pendingCalls.put(sectionTitle, pendingApiCall);
        showNoNetworkSnackbar();
    }

    private void showRetrySnackbar(PendingApiCall pendingCall) {
        if (isActivityValid()) {
            View rootView = activity.findViewById(android.R.id.content);
            Snackbar.make(rootView, "Failed to load " + pendingCall.sectionTitle, Snackbar.LENGTH_LONG)
                    .setAction("Retry", v -> {
                        pendingCall.retryCount = 0;
                        executeCall(pendingCall);
                    })
                    .show();
        }
    }

    private void showNoNetworkSnackbar() {
        if (isActivityValid()) {
            View rootView = activity.findViewById(android.R.id.content);
            Snackbar.make(rootView, "No internet connection. Will retry when connected.", Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private boolean isActivityValid() {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private void retryPendingCalls() {
        if (isActivityValid()) {
            activity.runOnUiThread(() -> {
                for (PendingApiCall pendingCall : new ArrayList<>(pendingCalls.values())) {
                    pendingCall.retryCount = 0;
                    executeCall(pendingCall);
                }
            });
        }
    }

    public void cleanUp() {
        // Properly remove the specific callback
        if (networkCallback != null) {
            networkMonitor.removeCallback(networkCallback);
        }
        // Clear pending calls
        pendingCalls.clear();
    }
}
