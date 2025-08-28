package muvi.anime.hub.adapters;

import android.content.Context;
import android.util.Log;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.DownloadBlock;
import java.io.File;
import java.util.List;

import muvi.anime.hub.managers.FetchSingleton;
import muvi.anime.hub.pages.AppUpdateActivity;

/**
 * Improved adapter that works with your existing FetchSingleton
 * and filters only update-related downloads
 */
public class ImprovedFetchAdapter {

    private static final String TAG = "ImprovedFetchAdapter";
    private static final String UPDATE_TAG = "APP_UPDATE";

    private Context context;
    private Fetch fetch;
    private AppUpdateActivity.DownloadProgressCallback progressCallback;
    private int currentDownloadId = -1;

    public ImprovedFetchAdapter(Context context) {
        this.context = context;
        initializeFetch();
    }

    private void initializeFetch() {
        // Use your existing FetchSingleton
        fetch = FetchSingleton.getFetchInstance(context);
        fetch.addListener(fetchListener);
    }

    public void setProgressCallback(AppUpdateActivity.DownloadProgressCallback callback) {
        this.progressCallback = callback;
    }

    public void startDownload(String downloadUrl, String fileName) {
        try {
            // Create updates directory
            File updatesDir = new File(context.getExternalFilesDir(null), "updates");
            if (!updatesDir.exists()) {
                updatesDir.mkdirs();
            }

            String filePath = updatesDir.getAbsolutePath() + File.separator + fileName;

            // Create download request with special tag for updates
            Request request = new Request(downloadUrl, filePath);
            request.setPriority(com.tonyodev.fetch2.Priority.HIGH);
            request.setNetworkType(com.tonyodev.fetch2.NetworkType.ALL);
            request.addHeader("User-Agent", "AppUpdater/1.0");

            // Add a tag to identify this as an update download
            request.setTag(UPDATE_TAG);

            // Start download
            fetch.enqueue(request, updatedRequest -> {
                currentDownloadId = updatedRequest.getId();
                Log.d(TAG, "Update download started with ID: " + currentDownloadId);

                if (progressCallback != null) {
                    progressCallback.onProgress(0, 0, 0);
                }
            }, error -> {
                Log.e(TAG, "Failed to enqueue update download: " + error.toString());
                if (progressCallback != null) {
                    progressCallback.onError("Failed to start download: " + error.toString());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error starting update download", e);
            if (progressCallback != null) {
                progressCallback.onError("Error starting download: " + e.getMessage());
            }
        }
    }

    public void cancelDownload() {
        if (currentDownloadId != -1) {
            fetch.cancel(currentDownloadId);
            currentDownloadId = -1;
        }
    }

    public boolean isDownloading() {
        return currentDownloadId != -1;
    }

    public void cleanup() {
        if (fetch != null) {
            fetch.removeListener(fetchListener);
            // Don't close the fetch instance since it's a singleton
        }

        // Clean up any completed update downloads to save space
        cleanupOldUpdateFiles();
    }

    private void cleanupOldUpdateFiles() {
        try {
            File updatesDir = new File(context.getExternalFilesDir(null), "updates");
            if (updatesDir.exists() && updatesDir.isDirectory()) {
                File[] files = updatesDir.listFiles();
                if (files != null) {
                    long currentTime = System.currentTimeMillis();
                    for (File file : files) {
                        // Delete files older than 7 days
                        if (currentTime - file.lastModified() > (7 * 24 * 60 * 60 * 1000)) {
                            boolean deleted = file.delete();
                            Log.d(TAG, "Cleaned up old update file: " + file.getName() + " - " + deleted);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up old update files", e);
        }
    }

    private final FetchListener fetchListener = new FetchListener() {
        @Override
        public void onAdded(Download download) {
            if (isUpdateDownload(download)) {
                Log.d(TAG, "Update download added: " + download.getUrl());
            }
        }

        @Override
        public void onQueued(Download download, boolean waitingOnNetwork) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId) {
                Log.d(TAG, "Update download queued: " + download.getId());
                if (progressCallback != null) {
                    progressCallback.onProgress(0, 0, download.getTotal());
                }
            }
        }

        @Override
        public void onWaitingNetwork(Download download) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId) {
                Log.d(TAG, "Update download waiting for network: " + download.getId());
            }
        }

        @Override
        public void onCompleted(Download download) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId) {
                Log.d(TAG, "Update download completed: " + download.getFile());
                currentDownloadId = -1;

                if (progressCallback != null) {
                    File downloadedFile = new File(download.getFile());
                    progressCallback.onSuccess(downloadedFile);
                }
            }
        }

        @Override
        public void onError(Download download, Error error, Throwable throwable) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId) {
                Log.e(TAG, "Update download error: " + error.toString(), throwable);
                currentDownloadId = -1;

                if (progressCallback != null) {
                    progressCallback.onError(getErrorMessage(error));
                }
            }
        }

        @Override
        public void onDownloadBlockUpdated(Download download, DownloadBlock downloadBlock, int totalBlocks) {
            // Optional: Handle block updates if needed for update downloads only
        }

        @Override
        public void onStarted(Download download, List<? extends DownloadBlock> downloadBlocks, int totalBlocks) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId) {
                Log.d(TAG, "Update download started: " + download.getId());
            }
        }

        @Override
        public void onProgress(Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId && progressCallback != null) {
                long downloaded = download.getDownloaded();
                long total = download.getTotal();
                int progress = total > 0 ? (int) ((downloaded * 100) / total) : 0;

                progressCallback.onProgress(progress, downloaded, total);
            }
        }

        @Override
        public void onPaused(Download download) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId) {
                Log.d(TAG, "Update download paused: " + download.getId());
            }
        }

        @Override
        public void onResumed(Download download) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId) {
                Log.d(TAG, "Update download resumed: " + download.getId());
            }
        }

        @Override
        public void onCancelled(Download download) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId) {
                Log.d(TAG, "Update download cancelled: " + download.getId());
                currentDownloadId = -1;

                if (progressCallback != null) {
                    progressCallback.onError("Download cancelled by user");
                }
            }
        }

        @Override
        public void onRemoved(Download download) {
            if (isUpdateDownload(download)) {
                Log.d(TAG, "Update download removed: " + download.getId());
            }
        }

        @Override
        public void onDeleted(Download download) {
            if (isUpdateDownload(download)) {
                Log.d(TAG, "Update download deleted: " + download.getId());
            }
        }
    };

    /**
     * Check if this download is an update download by checking the tag
     */
    private boolean isUpdateDownload(Download download) {
        return UPDATE_TAG.equals(download.getTag());
    }

    /**
     * Convert Fetch error to user-friendly message
     */
    private String getErrorMessage(Error error) {
        switch (error) {
            case NO_NETWORK_CONNECTION:
                return "No internet connection. Please check your network and try again.";
            case HTTP_NOT_FOUND:
                return "Update file not found on server.";
            case WRITE_PERMISSION_DENIED:
                return "Storage permission denied. Please grant storage permission.";
            case NO_STORAGE_SPACE:
                return "Not enough storage space available.";
            case UNKNOWN_HOST:
                return "Cannot connect to update server. Please try again later.";
            case CONNECTION_TIMED_OUT:
                return "Connection timed out. Please try again.";
            default:
                return "Download failed: " + error.toString();
        }
    }
}
