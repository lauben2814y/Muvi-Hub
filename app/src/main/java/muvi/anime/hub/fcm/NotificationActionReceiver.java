package muvi.anime.hub.fcm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int notificationId = intent.getIntExtra("NOTIFICATION_ID", 0);

        // Cancel the notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);

        if ("WATCH_LATER_ACTION".equals(action)) {
            String movieId = intent.getStringExtra("MOVIE_ID");
            Log.d(TAG, "Adding movie to Watch Later: " + movieId);

            // Call your API to add the movie to Watch Later
            addMovieToWatchLater(context, movieId);

        } else if ("ADD_WATCHLIST_ACTION".equals(action)) {
            String tvId = intent.getStringExtra("TV_ID");
            Log.d(TAG, "Adding TV show to Watchlist: " + tvId);

            // Call your API to add the TV show to Watchlist
            addTvToWatchlist(context, tvId);
        }
    }

    private void addMovieToWatchLater(Context context, String movieId) {
        // Implement API call to add movie to Watch Later
        // This might involve your backend API client

    }

    private void addTvToWatchlist(Context context, String tvId) {
        // Implement API call to add TV show to Watchlist
        // This might involve your backend API client

    }
}
