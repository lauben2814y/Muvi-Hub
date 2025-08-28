package muvi.anime.hub.managers.update;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.DownloadBlock;
import java.io.File;
import java.util.List;

import muvi.anime.hub.managers.FetchSingleton;

public class ImprovedFetchAdapter {
    private static final String TAG = "ImprovedFetchAdapter";
    private static final String UPDATE_TAG = "MUVI_HUB_UPDATE";

    private final Context context;
    private Fetch fetch;
    private DownloadProgressCallback progressCallback;
    private int currentDownloadId = -1;

    public ImprovedFetchAdapter(Context context) {
        this.context = context;
        initializeFetch();
    }

    private void initializeFetch() {
        fetch = FetchSingleton.getFetchInstance(context);
        fetch.addListener(fetchListener);

        if (UpdateConfig.DEBUG_UPDATES) {
            Log.d(TAG, "ImprovedFetchAdapter initialized with FetchSingleton");
        }
    }

    public void setProgressCallback(DownloadProgressCallback callback) {
        this.progressCallback = callback;
    }

    public void startDownload(String downloadUrl, String fileName) {
        try {
            // Create updates directory
            File updatesDir = new File(context.getExternalFilesDir(null), "updates");
            if (!updatesDir.exists()) {
                boolean created = updatesDir.mkdirs();
                if (UpdateConfig.DEBUG_UPDATES) {
                    Log.d(TAG, "Created updates directory: " + created);
                }
            }

            String filePath = updatesDir.getAbsolutePath() + File.separator + fileName;

            if (UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Starting download: " + downloadUrl);
                Log.d(TAG, "Saving to: " + filePath);
            }

            // Create download request with proper headers for GitHub
            Request request = new Request(downloadUrl, filePath);
            request.setPriority(com.tonyodev.fetch2.Priority.HIGH);
            request.setNetworkType(com.tonyodev.fetch2.NetworkType.ALL);

            // Add headers that GitHub expects
            request.addHeader("User-Agent", UpdateConfig.USER_AGENT);
            request.addHeader("Accept", "application/octet-stream");
            request.addHeader("Accept-Encoding", "identity"); // Prevent compression issues

            // For GitHub releases, we might need to handle redirects properly
            // The browser_download_url from GitHub API should work directly

            // Add tag to identify as update download
            request.setTag(UPDATE_TAG);

            if (UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Request headers: User-Agent=" + UpdateConfig.USER_AGENT);
                Log.d(TAG, "Request URL: " + downloadUrl);
            }

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
                    progressCallback.onError("Failed to start download: " + getErrorMessage(error));
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
            if (UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Cancelling download: " + currentDownloadId);
            }
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
        }
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
                            if (UpdateConfig.DEBUG_UPDATES) {
                                Log.d(TAG, "Cleaned up old update file: " + file.getName() + " - " + deleted);
                            }
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
        public void onAdded(@NonNull Download download) {
            if (isUpdateDownload(download) && UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Update download added: " + download.getUrl());
            }
        }

        @Override
        public void onQueued(@NonNull Download download, boolean waitingOnNetwork) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId) {
                if (UpdateConfig.DEBUG_UPDATES) {
                    Log.d(TAG, "Update download queued: " + download.getId());
                }
                if (progressCallback != null) {
                    progressCallback.onProgress(0, 0, download.getTotal());
                }
            }
        }

        @Override
        public void onWaitingNetwork(@NonNull Download download) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId && UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Update download waiting for network: " + download.getId());
            }
        }

        @Override
        public void onCompleted(@NonNull Download download) {
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
        public void onError(@NonNull Download download, @NonNull Error error, Throwable throwable) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId) {
                Log.e(TAG, "Update download error: " + error, throwable);
                currentDownloadId = -1;

                if (progressCallback != null) {
                    progressCallback.onError(getErrorMessage(error));
                }
            }
        }

        @Override
        public void onDownloadBlockUpdated(@NonNull Download download, @NonNull DownloadBlock downloadBlock, int totalBlocks) {
            // Optional: Handle block updates
        }

        @Override
        public void onStarted(@NonNull Download download, @NonNull List<? extends DownloadBlock> downloadBlocks, int totalBlocks) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId && UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Update download started: " + download.getId());
            }
        }

        @Override
        public void onProgress(@NonNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId && progressCallback != null) {
                long downloaded = download.getDownloaded();
                long total = download.getTotal();
                int progress = total > 0 ? (int) ((downloaded * 100) / total) : 0;

                progressCallback.onProgress(progress, downloaded, total);
            }
        }

        @Override
        public void onPaused(@NonNull Download download) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId && UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Update download paused: " + download.getId());
            }
        }

        @Override
        public void onResumed(@NonNull Download download) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId && UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Update download resumed: " + download.getId());
            }
        }

        @Override
        public void onCancelled(@NonNull Download download) {
            if (isUpdateDownload(download) && download.getId() == currentDownloadId) {
                if (UpdateConfig.DEBUG_UPDATES) {
                    Log.d(TAG, "Update download cancelled: " + download.getId());
                }
                currentDownloadId = -1;

                if (progressCallback != null) {
                    progressCallback.onError("Download cancelled by user");
                }
            }
        }

        @Override
        public void onRemoved(@NonNull Download download) {
            if (isUpdateDownload(download) && UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Update download removed: " + download.getId());
            }
        }

        @Override
        public void onDeleted(@NonNull Download download) {
            if (isUpdateDownload(download) && UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Update download deleted: " + download.getId());
            }
        }
    };

    private boolean isUpdateDownload(Download download) {
        return UPDATE_TAG.equals(download.getTag());
    }

    /**
     * Enhanced error message handling for GitHub downloads
     */
    private String getErrorMessage(Error error) {
        return switch (error) {
            case REQUEST_NOT_SUCCESSFUL ->
                    "GitHub download failed. This may be due to rate limiting or authentication issues.";
            case NO_NETWORK_CONNECTION ->
                    "No internet connection. Please check your network and try again.";
            case HTTP_NOT_FOUND ->
                    "Update file not found on GitHub. The release may have been removed.";
            case WRITE_PERMISSION_DENIED ->
                    "Storage permission denied. Please grant storage permission.";
            case NO_STORAGE_SPACE -> "Not enough storage space available.";
            case UNKNOWN_HOST -> "Cannot connect to GitHub. Please try again later.";
            case CONNECTION_TIMED_OUT ->
                    "Connection timed out. Please check your internet connection.";
            default ->
                    "Download failed: " + error + ". Try again or download manually from GitHub.";
        };
    }

    public interface DownloadProgressCallback {
        void onProgress(int progress, long downloadedBytes, long totalBytes);
        void onSuccess(File file);
        void onError(String error);
    }
}