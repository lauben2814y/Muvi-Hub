package muvi.anime.hub.managers;

import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2core.DownloadBlock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import muvi.anime.hub.MainActivity;
import muvi.anime.hub.R;

public class DownloadService extends Service {
    private static final String TAG = "Muvi-Hub";
    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFICATION_ID_BASE = 1001;
    public static boolean isServiceRunning = false;

    // Action constants
    public static final String ACTION_PAUSE_DOWNLOAD = "muvi.anime.hub.PAUSE_DOWNLOAD";
    public static final String ACTION_RESUME_DOWNLOAD = "muvi.anime.hub.RESUME_DOWNLOAD";
    public static final String ACTION_CANCEL_DOWNLOAD = "muvi.anime.hub.CANCEL_DOWNLOAD";
    public static final String EXTRA_DOWNLOAD_ID = "download_id";
    public static final String ACTION_STOP_SERVICE = "muvi.anime.hub.STOP_SERVICE";

    private Fetch fetch;
    private NotificationManager notificationManager;
    private final Map<Integer, Integer> downloadProgress = new HashMap<>();
    private final Map<Integer, String> downloadNames = new HashMap<>();
    private final Map<Integer, Status> downloadStatus = new HashMap<>();
    private boolean isForegroundService = false;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        isServiceRunning = true;
        createNotificationChannel();

        // Call startForeground immediately in onCreate to avoid the timeout exception
        try {
            startForeground(NOTIFICATION_ID_BASE, createGenericNotification());
            isForegroundService = true;
            Log.d(TAG, "Service started in foreground mode in onCreate");
        } catch (Exception e) {
            Log.w(TAG, "Failed to start foreground in onCreate", e);
        }

        fetch = FetchSingleton.getFetchInstance(this);
        fetch.addListener(fetchListener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Foreground service is already started in onCreate, so we don't need to do it again
        // unless it failed in onCreate
        if (!isForegroundService) {
            try {
                startForeground(NOTIFICATION_ID_BASE, createGenericNotification());
                isForegroundService = true;
                Log.d(TAG, "Service started in foreground mode in onStartCommand");
            } catch (ForegroundServiceStartNotAllowedException e) {
                Log.w(TAG, "Cannot start foreground service, running in background mode", e);
                // Service will continue running in background mode
            } catch (Exception e) {
                Log.e(TAG, "Error starting foreground service", e);
            }
        }

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_STOP_SERVICE.equals(action)) {
                pauseAllDownloadsAndStop();
                return START_NOT_STICKY;
            } else {
                handleAction(intent);
            }
        }

        Log.d(TAG, "Download service started");
        return START_STICKY;
    }

    private void pauseAllDownloadsAndStop() {
        fetch.pauseAll();

        // Remove all notifications
        for (int downloadId : downloadNames.keySet()) {
            notificationManager.cancel(NOTIFICATION_ID_BASE + downloadId);
        }

        // Clear all tracking maps
        downloadProgress.clear();
        downloadNames.clear();
        downloadStatus.clear();

        // Stop the service
        if (isForegroundService) {
            stopForeground(true);
        }
        stopSelf();
        isServiceRunning = false;
    }

    private void handleAction(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        int downloadId = intent.getIntExtra(EXTRA_DOWNLOAD_ID, -1);

        switch (action) {
            case ACTION_PAUSE_DOWNLOAD:
                if (downloadId != -1) {
                    fetch.pause(downloadId);
                }
                break;
            case ACTION_RESUME_DOWNLOAD:
                if (downloadId != -1) {
                    fetch.resume(downloadId);
                }
                break;
            case ACTION_CANCEL_DOWNLOAD:
                if (downloadId != -1) {
                    fetch.remove(downloadId);
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fetch.removeListener(fetchListener);
        isServiceRunning = false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Download Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows download progress");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createGenericNotification() {
        // Create intent to open the app when notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Download Service")
                .setContentText("Downloads running in background...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private Notification createDownloadNotification(int downloadId, String fileName, int progress, Status status) {
        // Create intent to open the app when notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, downloadId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true);

        // Set different notification appearance based on status
        if (status == Status.PAUSED) {
            builder.setSmallIcon(android.R.drawable.ic_media_pause)
                    .setContentTitle("Download Paused")
                    .setContentText(fileName);
        } else {
            builder.setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle("Downloading")
                    .setContentText(progress + "%" + ", " + fileName);
        }

        // Add progress bar
        builder.setProgress(100, progress, false);

        // Add toggle action (pause/resume based on current status)
        if (status == Status.DOWNLOADING) {
            // Add pause button
            Intent pauseIntent = new Intent(this, DownloadService.class);
            pauseIntent.setAction(ACTION_PAUSE_DOWNLOAD);
            pauseIntent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
            PendingIntent pausePendingIntent = PendingIntent.getService(
                    this, 100 + downloadId, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent);
        } else if (status == Status.PAUSED) {
            // Add resume button
            Intent resumeIntent = new Intent(this, DownloadService.class);
            resumeIntent.setAction(ACTION_RESUME_DOWNLOAD);
            resumeIntent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
            PendingIntent resumePendingIntent = PendingIntent.getService(
                    this, 200 + downloadId, resumeIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent);
        }

        // Add cancel button
        Intent cancelIntent = new Intent(this, DownloadService.class);
        cancelIntent.setAction(ACTION_CANCEL_DOWNLOAD);
        cancelIntent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
        PendingIntent cancelPendingIntent = PendingIntent.getService(
                this, 300 + downloadId, cancelIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent);

        return builder.build();
    }

    private void updateDownloadNotification(int downloadId) {
        if (!downloadNames.containsKey(downloadId)) return;

        String fileName = downloadNames.get(downloadId);
        int progress = downloadProgress.getOrDefault(downloadId, 0);
        Status status = downloadStatus.getOrDefault(downloadId, Status.QUEUED);

        Notification notification = createDownloadNotification(downloadId, fileName, progress, status);
        notificationManager.notify(NOTIFICATION_ID_BASE + downloadId, notification);
    }

    private void removeDownloadNotification(int downloadId) {
        notificationManager.cancel(NOTIFICATION_ID_BASE + downloadId);
    }

    private void checkActiveDownloads() {
        if (downloadNames.isEmpty() && isForegroundService) {
            // No active downloads, stop the service
            stopForeground(true);
            stopSelf();
            isForegroundService = false;
        }
    }

    private final FetchListener fetchListener = new FetchListener() {
        @Override
        public void onDeleted(@NonNull Download download) {
            int downloadId = download.getId();
            downloadProgress.remove(downloadId);
            downloadNames.remove(downloadId);
            downloadStatus.remove(downloadId);
            removeDownloadNotification(downloadId);
            checkActiveDownloads();
        }

        @Override
        public void onRemoved(@NonNull Download download) {
            int downloadId = download.getId();
            downloadProgress.remove(downloadId);
            downloadNames.remove(downloadId);
            downloadStatus.remove(downloadId);
            removeDownloadNotification(downloadId);
            checkActiveDownloads();
        }

        @Override
        public void onCancelled(@NonNull Download download) {
            int downloadId = download.getId();
            downloadProgress.remove(downloadId);
            downloadNames.remove(downloadId);
            downloadStatus.remove(downloadId);
            removeDownloadNotification(downloadId);
            checkActiveDownloads();
        }

        @Override
        public void onResumed(@NonNull Download download) {
            int downloadId = download.getId();
            downloadStatus.put(downloadId, Status.DOWNLOADING);
            updateDownloadNotification(downloadId);
        }

        @Override
        public void onPaused(@NonNull Download download) {
            int downloadId = download.getId();
            downloadStatus.put(downloadId, Status.PAUSED);
            updateDownloadNotification(downloadId);
        }

        @Override
        public void onProgress(@NonNull Download download, long l, long l1) {
            int downloadId = download.getId();
            long total = download.getTotal();
            int progress = 0;

            if (total > 0) {
                progress = (int) ((download.getDownloaded() * 100) / total);
            }

            downloadProgress.put(downloadId, progress);
            downloadStatus.put(downloadId, download.getStatus());
            updateDownloadNotification(downloadId);
        }

        @Override
        public void onStarted(@NonNull Download download, @NonNull List<? extends DownloadBlock> list, int i) {
            int downloadId = download.getId();
            downloadProgress.put(downloadId, 0);
            String fileName = download.getRequest().getHeaders().getOrDefault("fileName", "Unknown File");
            downloadNames.put(downloadId, fileName);
            downloadStatus.put(downloadId, Status.DOWNLOADING);
            updateDownloadNotification(downloadId);
        }

        @Override
        public void onDownloadBlockUpdated(@NonNull Download download, @NonNull DownloadBlock downloadBlock, int i) {
            // Not needed for notification updates
        }

        @Override
        public void onError(@NonNull Download download, @NonNull Error error, @Nullable Throwable throwable) {
            int downloadId = download.getId();
            downloadProgress.remove(downloadId);
            downloadNames.remove(downloadId);
            downloadStatus.remove(downloadId);
            removeDownloadNotification(downloadId);
            checkActiveDownloads();
        }

        @Override
        public void onCompleted(@NonNull Download download) {
            int downloadId = download.getId();
            downloadProgress.remove(downloadId);
            downloadNames.remove(downloadId);
            downloadStatus.remove(downloadId);

            // Show completion notification then remove it after a delay
            String fileName = download.getRequest().getHeaders().getOrDefault("fileName", "Unknown File");
            NotificationCompat.Builder builder = new NotificationCompat.Builder(DownloadService.this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.check_24px)
                    .setContentTitle("Download Complete")
                    .setContentText(fileName)
                    .setAutoCancel(true);

            notificationManager.notify(NOTIFICATION_ID_BASE + downloadId, builder.build());

            // Remove completion notification after a few seconds
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    removeDownloadNotification(downloadId), 5000);

            checkActiveDownloads();
        }

        @Override
        public void onWaitingNetwork(@NonNull Download download) {
            int downloadId = download.getId();
            String fileName = download.getRequest().getHeaders().getOrDefault("fileName", "Unknown File");
            downloadNames.put(downloadId, fileName);
            downloadStatus.put(downloadId, Status.QUEUED);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(DownloadService.this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.network_cell_24px)
                    .setContentTitle("Waiting for network")
                    .setContentText(fileName);

            notificationManager.notify(NOTIFICATION_ID_BASE + downloadId, builder.build());
        }

        @Override
        public void onQueued(@NonNull Download download, boolean b) {
            int downloadId = download.getId();
            downloadProgress.put(downloadId, 0);
            String fileName = download.getRequest().getHeaders().getOrDefault("fileName", "Unknown File");
            downloadNames.put(downloadId, fileName);
            downloadStatus.put(downloadId, Status.QUEUED);
            updateDownloadNotification(downloadId);
        }

        @Override
        public void onAdded(@NonNull Download download) {
            int downloadId = download.getId();
            downloadProgress.put(downloadId, 0);
            String fileName = download.getRequest().getHeaders().getOrDefault("fileName", "Unknown File");
            downloadNames.put(downloadId, fileName);
            downloadStatus.put(downloadId, Status.QUEUED);
            updateDownloadNotification(downloadId);
        }
    };
}
