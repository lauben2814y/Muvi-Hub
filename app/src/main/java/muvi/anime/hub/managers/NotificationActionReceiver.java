package muvi.anime.hub.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.tonyodev.fetch2.Fetch;

public class NotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_DOWNLOAD_STATE_CHANGED = "com.your.package.ACTION_DOWNLOAD_STATE_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        int downloadId = intent.getIntExtra("downloadId", -1);
        if (downloadId == -1) return;

        Fetch fetch = FetchSingleton.getFetchInstance(context);
        String action = intent.getAction();

        if (action != null) {
            boolean isPaused = false;

            switch (action) {
                case "pause":
                    fetch.pause(downloadId);
                    isPaused = true;
                    break;
                case "resume":
                    fetch.resume(downloadId);
                    isPaused = false;
                    break;
                case "cancel":
                    fetch.remove(downloadId);
                    // No need to broadcast for cancel as FetchListener will handle removal
                    return;
            }

            // Broadcast the state change to update the UI
            Intent stateIntent = new Intent(ACTION_DOWNLOAD_STATE_CHANGED);
            stateIntent.putExtra("downloadId", downloadId);
            stateIntent.putExtra("isPaused", isPaused);
            LocalBroadcastManager.getInstance(context).sendBroadcast(stateIntent);
        }
    }
}