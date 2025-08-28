package muvi.anime.hub.managers;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tonyodev.fetch2.EnqueueAction;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;

import java.io.File;
import java.util.List;

import muvi.anime.hub.data.Utils;
import muvi.anime.hub.storage.UserStatsManager;

public class DownloadManager {
    private final UserManager userManager;
    private static final int STORAGE_PERMISSION_CODE = 1001;

    private final Activity activity;
    private final Fetch fetch;
    private final Context context;
    private final RewardedAdManager rewardedAdManager;

    private String pendingFileName;
    private String pendingFileUrl;
    private String pendingFileImage;
    public static String TAG = Utils.getTag();
    private final UserStatsManager statsManager;

    // ADD THIS LINE - Dialog tracking
    private AlertDialog adBlockerDialog;

    public DownloadManager(Activity activity, Fetch fetch, Context context) {
        this.activity = activity;
        this.fetch = fetch;
        this.context = context;
        this.rewardedAdManager = RewardedAdManager.getInstance(context);
        this.statsManager = new UserStatsManager(context);
        this.userManager = UserManager.getInstance(context);
    }

    private String getFileExtensionFromUrl(String url) {
        try {
            // First try to get extension from URL path
            String path = Uri.parse(url).getLastPathSegment();
            if (path != null) {
                int lastDot = path.lastIndexOf('.');
                if (lastDot >= 0) {
                    return path.substring(lastDot);
                }
            }

            // If no extension found, try to guess from the last segment
            String[] segments = url.split("/");
            String lastSegment = segments[segments.length - 1];
            int lastDot = lastSegment.lastIndexOf('.');
            if (lastDot >= 0) {
                return lastSegment.substring(lastDot);
            }

            // Default to .mp4 if no extension found (since this is a video downloader)
            return ".mp4";
        } catch (Exception e) {
            Log.e(TAG, "Error getting file extension", e);
            return ".mp4"; // Default fallback
        }
    }

    public void startDownload(String fileName, String fileUrl, String fileImage) {
        String extension = getFileExtensionFromUrl(fileUrl);
        this.pendingFileName = fileName.endsWith(extension) ? fileName : fileName + extension;
        this.pendingFileUrl = fileUrl;
        this.pendingFileImage = fileImage;

        Log.d(TAG, "Start download called successfully");

        if (checkAndRequestPermissions()) {
            initiateDownload();
        }
    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{
                        Manifest.permission.READ_MEDIA_VIDEO
                }, STORAGE_PERMISSION_CODE);
                return false;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
                return false;
            }
        }
        return true;
    }

    private void initiateDownload() {
        try {
            // Get the DownloadsFragment directory path
            String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/Muvi-Hub";
            String fullPath = downloadPath + File.separator + pendingFileName;
            // Start the download process
            prepareAd(fullPath);
        } catch (Exception e) {
            Log.e(TAG, "Error initiating download", e);
            Toast.makeText(activity, "Error starting download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startDownloadQueue(String downloadPath) {
        Request request = new Request(pendingFileUrl, downloadPath);
        request.setPriority(Priority.HIGH);
        request.setNetworkType(NetworkType.ALL);
        request.addHeader("fileName", pendingFileName);
        request.addHeader("fileImage", pendingFileImage);
        String credentials = userManager.getCurrentUser().getMedia_user_name()
                + ":" +
                userManager.getCurrentUser().getMedia_password();
        String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        request.addHeader("Authorization", basicAuth);

        request.setAutoRetryMaxAttempts(3);
        request.setEnqueueAction(EnqueueAction.REPLACE_EXISTING);

        // Start foreground service before enqueuing the download
        startDownloadService();

        fetch.enqueue(request,
                updatedRequest -> {
                    Log.d(TAG, "Download started successfully: " + updatedRequest.getFile());
                    Toast.makeText(activity, "Download started", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Log.e(TAG, "Download start failed: " + error.toString());
                    handleDownloadError(error, downloadPath);
                });
    }

    private void startDownloadService() {
        // Check if service is already running
        if (DownloadService.isServiceRunning) {
            Log.d(TAG, "Download service is already running");
            return;
        }

        Intent serviceIntent = new Intent(context, DownloadService.class);

        try {
            // For Android 14+ (API 34+), we need to be more careful about when we can start foreground services
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ - Check if we can start foreground service
                if (canStartForegroundService()) {
                    ContextCompat.startForegroundService(context, serviceIntent);
                    Log.d(TAG, "Started foreground service on Android 14+");
                } else {
                    // Fallback to regular service if foreground service is not allowed
                    context.startService(serviceIntent);
                    Log.d(TAG, "Started background service on Android 14+ (foreground not allowed)");

                    // Optionally show user notification about background limitations
                    showBackgroundServiceNotification();
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8-13
                ContextCompat.startForegroundService(context, serviceIntent);
                Log.d(TAG, "Started foreground service on Android 8-13");
            } else {
                // Below Android 8
                context.startService(serviceIntent);
                Log.d(TAG, "Started service on Android < 8");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start download service", e);
            // Show error to user if needed
            Toast.makeText(context, "Unable to start download service", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private boolean canStartForegroundService() {
        // Check if the app is in foreground or has recently been in foreground
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.processName.equals(context.getPackageName())) {
                    // App is in foreground or visible
                    return processInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE;
                }
            }
        }

        // Additional checks you can add:
        // - Check if user initiated the action (e.g., button press)
        // - Check if app has been granted battery optimization exemption
        // - Check if app is whitelisted for background activities

        return false;
    }

    private void showBackgroundServiceNotification() {
        // Inform user that downloads will run in background with limitations
        Toast.makeText(context,
                "Downloads will run in background. For better performance, keep the app open.",
                Toast.LENGTH_LONG).show();
    }

    private String checkAndHandleIOError(String downloadPath) {
        File downloadFile = new File(downloadPath);

        // Check if directory exists and is writable
        File parentDir = downloadFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                return "Failed to create download directory";
            }
        }

        // Check if file already exists and is locked
        if (downloadFile.exists()) {
            if (!downloadFile.canWrite()) {
                return "File is locked or permission denied";
            }
            // Try to delete existing file
            if (!downloadFile.delete()) {
                return "Cannot override existing file";
            }
        }

        // Check available space
        assert parentDir != null;
        long availableSpace = parentDir.getFreeSpace();
        if (availableSpace < 1024 * 1024 * 10) { // 10MB minimum
            return "Insufficient storage space";
        }

        return "Unknown IO error occurred";
    }

    private void validateAndFixStorageAccess(String downloadPath) {
        File downloadDir = new File(downloadPath).getParentFile();
        if (downloadDir != null && !downloadDir.exists()) {
            boolean created = downloadDir.mkdirs();
            if (!created) {
                Log.e(TAG, "Failed to create download directory");
            }
        }
    }

    private void handleDownloadError(Error error, String downloadPath) {
        String errorMessage;

        switch (error) {
            case UNKNOWN_IO_ERROR:
                errorMessage = checkAndHandleIOError(downloadPath);
                break;
            case NO_STORAGE_SPACE:
                errorMessage = "Not enough storage space available";
                break;
            case FILE_NOT_CREATED:
                errorMessage = "Cannot create file. Checking permissions...";
                validateAndFixStorageAccess(downloadPath);
                break;
            case CONNECTION_TIMED_OUT:
                errorMessage = "Connection timed out. Check your internet connection";
                break;
            default:
                errorMessage = "Download failed: " + error;
        }

        Toast.makeText(activity, "Download failed reason: " + errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void prepareAd(String downloadPath) {
        if (rewardedAdManager.isAdLoaded()) {
            showAdAndStartDownload(downloadPath);
            return;
        }

        Toast.makeText(activity, "Preparing ad...", Toast.LENGTH_SHORT).show();

        final long MAX_WAIT_TIME = 5000; // 5 seconds
        final long startTime = System.currentTimeMillis();

        // ADD THIS - Flag to track if ad blocker dialog was already shown
        final boolean[] adBlockerDialogShown = {false};

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // MODIFY THIS - Only show dialog once
                if (rewardedAdManager.isAdBlockerDetected() && !adBlockerDialogShown[0]) {
                    showAdBlockerDetectedDialog(activity, rewardedAdManager.getLastAdError());
                    adBlockerDialogShown[0] = true;
                    return; // Exit the loop after showing dialog
                }

                if (rewardedAdManager.isAdLoaded()) {
                    showAdAndStartDownload(downloadPath);
                    return;
                }

                if (System.currentTimeMillis() - startTime > MAX_WAIT_TIME) {
                    if (rewardedAdManager.isAdBlockerDetected() && !adBlockerDialogShown[0]) {
                        showAdBlockerDetectedDialog(activity, rewardedAdManager.getLastAdError());
                        adBlockerDialogShown[0] = true;
                    } else if (!adBlockerDialogShown[0]) {
                        startDownloadQueue(downloadPath);
                    }
                    return; // Exit after timeout
                }

                handler.postDelayed(this, 500);
            }
        }, 500);
    }

    private void showAdAndStartDownload(String downloadPath) {
        rewardedAdManager.showRewardedAd(activity, new RewardedAdManager.OnAdRewardedCallback() {
            @Override
            public void onRewarded() {
                statsManager.incrementDownloads();
                statsManager.addCoins(3);
                startDownloadQueue(downloadPath);
            }

            @Override
            public void onAdFailedToShow() {

            }

            @Override
            public void onAdNotAvailable() {

            }

            @Override
            public void onAdDismissed() {

            }

            @Override
            public void onAdShowed() {

            }

            @Override
            public void onAdBlockerDetected() {

            }
        });
    }

    private void showAdBlockerDetectedDialog(Activity currentActivity, String lastAdError) {
        if (currentActivity == null || currentActivity.isFinishing() || currentActivity.isDestroyed()) {
            Log.w(Utils.getTag(), "Attempted to show dialog on invalid activity.");
            return;
        }

        // ADD THIS - Dismiss any existing dialog first
        if (adBlockerDialog != null && adBlockerDialog.isShowing()) {
            adBlockerDialog.dismiss();
        }

        // MODIFY THIS - Create and store the dialog reference
        adBlockerDialog = new MaterialAlertDialogBuilder(currentActivity)
                .setTitle("Ad Blocker Detected 🚫")
                .setMessage("We've detected that you may be using an ad blocker or VPN that's " +
                        "preventing our ads from loading. Please disable it and open the App again")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Handle OK action if needed
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create();

        adBlockerDialog.show();

        // Log the detailed error for diagnostics
        Log.d(Utils.getTag(), "Ad blocker detected with error: " + lastAdError);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                initiateDownload();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    showPermissionExplanationDialog();
                } else {
                    Toast.makeText(activity,
                            "Storage permission is required to download files",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void showPermissionExplanationDialog() {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Permissions Required")
                .setMessage("To download files, this app needs permission to save media. Please grant the necessary permissions in Settings.")
                .setPositiveButton("OK", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
}