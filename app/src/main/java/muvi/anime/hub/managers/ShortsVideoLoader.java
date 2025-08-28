package muvi.anime.hub.managers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.List;

import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.models.MovieShort;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShortsVideoLoader {
    private final Context context;
    private final SecureService movieService;
    public Boolean isLoading = false;
    private static final String TAG = "MuviShorts";

    public ShortsVideoLoader(Context context) {
        this.context = context;
        this.movieService = SecureClient.getApi(context);
    }

    public interface VideoLoadCallback {
        void onVideosLoaded(List<MovieShort> movieShorts);

        void onError(String message);
    }

    public void loadMoreShorts(VideoLoadCallback callback, int page) {
        isLoading = true;
        movieService.getShorts(
                page,
                20,
                UtilsManager.getShortFields()
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<MovieShort>> call, @NonNull Response<List<MovieShort>> response) {
                List<MovieShort> movieShorts = response.body();

                if (response.isSuccessful() && response.body() != null) {
                    // Notify on the main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onVideosLoaded(movieShorts);
                        isLoading = false;
                    });
                } else {
                    Log.e(TAG, "Shorts Video Manager: Error getting shorts");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MovieShort>> call, @NonNull Throwable throwable) {
                Toast.makeText(context, "Error getting shorts " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
