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

import muvi.anime.hub.data.movie.SupabaseMovieSection;
import muvi.anime.hub.managers.NetworkMonitor;
import muvi.anime.hub.adapters.movie.DbSectionAdapter;
import muvi.anime.hub.data.movie.SupabaseMovie;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieCallHandler {
    private final Activity activity;
    private final DbSectionAdapter dbSectionAdapter;
    private final NetworkMonitor networkMonitor;
    private final NetworkMonitor.NetworkCallback networkCallback;
    private final Map<String, PendingApiCall> pendingCalls = new HashMap<>();
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 5000; // 5 secs

    public MovieCallHandler(Activity activity, DbSectionAdapter dbSectionAdapter) {
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

    public interface MovieSectionCallback {
        void onSuccess(List<SupabaseMovie> movies);
    }

    private static class PendingApiCall {
        final Call<List<SupabaseMovie>> call;
        final String sectionTitle;
        final MovieSectionCallback callback;
        int retryCount = 0;

        PendingApiCall(Call<List<SupabaseMovie>> call, String sectionTitle,
                       MovieSectionCallback callback) {
            this.call = call;
            this.sectionTitle = sectionTitle;
            this.callback = callback;
        }
    }

    public void fetchMovieSection(Call<List<SupabaseMovie>> call, String sectionTitle,
                                  MovieSectionCallback nextAction) {
        if (!networkMonitor.isNetworkAvailable()) {
            handleNoNetwork(call, sectionTitle, nextAction);
            return;
        }

        dbSectionAdapter.addPreloader();

        executeCall(new PendingApiCall(call, sectionTitle, nextAction));
    }

    private void executeCall(PendingApiCall pendingCall) {
        pendingCall.call.clone().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseMovie>> call, @NonNull Response<List<SupabaseMovie>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (isActivityValid()) {
                        activity.runOnUiThread(() -> {
                            List<SupabaseMovie> movies = response.body();
                            SupabaseMovieSection section =
                                    new SupabaseMovieSection(pendingCall.sectionTitle, movies);

                            dbSectionAdapter.addSupabaseMovieSection(section);

                            if (pendingCall.callback != null) {
                                pendingCall.callback.onSuccess(movies);
                            }

                            // Remove from pending calls if it was there
                            pendingCalls.remove(pendingCall.sectionTitle);
                        });
                    }
                } else {
                    handleFailure(pendingCall, new Exception("Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseMovie>> call, @NonNull Throwable throwable) {
                handleFailure(pendingCall, throwable);
            }
        });
    }

    private void handleFailure(PendingApiCall pendingCall, Throwable throwable) {
        if (isActivityValid()) {
            activity.runOnUiThread(() -> {
                Log.e("Muvi-Hub-Main", "Error when executing call: " + throwable.getMessage());

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

    private void handleNoNetwork(Call<List<SupabaseMovie>> call, String sectionTitle,
                                 MovieSectionCallback callback) {
        PendingApiCall pendingCall = new PendingApiCall(call, sectionTitle, callback);
        pendingCalls.put(sectionTitle, pendingCall);
        showNoNetworkSnackbar();
    }

    private void scheduleRetry(PendingApiCall pendingCall) {
        pendingCall.retryCount++;
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> executeCall(pendingCall),
                (long) RETRY_DELAY_MS * pendingCall.retryCount
        );
    }

    private void retryPendingCalls() {
        if (isActivityValid()) {
            activity.runOnUiThread(() -> {
                for (PendingApiCall pendingCall : new ArrayList<>(pendingCalls.values())) {
                    pendingCall.retryCount = 0; // Reset retry count
                    executeCall(pendingCall);
                }
            });
        }
    }

    private void showRetrySnackbar(PendingApiCall pendingCall) {
        if (isActivityValid()) {
            View rootView = activity.findViewById(android.R.id.content);
            Snackbar.make(rootView,
                            "Failed to load " + pendingCall.sectionTitle,
                            Snackbar.LENGTH_LONG)
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
            Snackbar.make(rootView,
                            "No internet connection. Will retry when connected.",
                            Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private boolean isActivityValid() {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    public void cleanup() {
        // Properly remove the specific callback
        if (networkCallback != null) {
            networkMonitor.removeCallback(networkCallback);
        }
        // Clear pending calls
        pendingCalls.clear();
    }
}