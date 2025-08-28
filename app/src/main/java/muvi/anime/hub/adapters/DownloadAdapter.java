package muvi.anime.hub.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Status;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import muvi.anime.hub.R;
import muvi.anime.hub.data.DownloadItem;
import muvi.anime.hub.managers.DownloadService;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {
    private final List<DownloadItem> downloadItems;
    private final Fetch fetch;
    private final Context context;

    public DownloadAdapter(List<DownloadItem> downloadItems, Fetch fetch, Context context) {
        this.downloadItems = downloadItems;
        this.fetch = fetch;
        this.context = context;
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.download_item, parent, false);
        return new DownloadViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadItem downloadItem = downloadItems.get(position);

        holder.fileName.setText(downloadItem.getFileName());
        holder.progressPercentage.setText(downloadItem.getProgress() + "%");
        holder.progressBytes.setText(formatProgressDetails(downloadItem.getDownloadedBytes(), downloadItem.getTotalBytes()));
        holder.progressNetwork.setText(downloadItem.getNetworkSpeed() == null ? "0 bytes" : downloadItem.getNetworkSpeed());
        holder.progressBar.setProgress(downloadItem.getProgress(), true);

        String currentImageTag = (String) holder.poster.getTag();
        if (currentImageTag == null || !currentImageTag.equals(downloadItem.getFileImage())) {
            Glide.with(holder.poster)
                    .load(downloadItem.getFileImage())
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.poster);

            holder.poster.setTag(downloadItem.getFileImage());
        }

        updateDownloadState(holder, downloadItem);

        // Disable pause/resume button for queued DownloadsFragment
        holder.pauseBtn.setEnabled(downloadItem.getDownloadStatus() != Status.QUEUED &&
                downloadItem.getDownloadStatus() != Status.ADDED);

        // Set click listener for cancel download
        holder.cancelBtn.setOnClickListener(view -> {
            cancelDownload(downloadItem, holder.getBindingAdapterPosition());
        });
    }

    private void updateDownloadState(DownloadViewHolder holder, DownloadItem downloadItem) {
        // Hide all status indicators first
        holder.progressBar.setVisibility(View.GONE);
        holder.pausedTxt.setVisibility(View.GONE);
        holder.queuedTxt.setVisibility(View.GONE);
        holder.failedTxt.setVisibility(View.GONE);
        holder.resumeText.setVisibility(View.GONE);

        switch (downloadItem.getDownloadStatus()) {
            case QUEUED:
            case ADDED:
                holder.queuedTxt.setVisibility(View.VISIBLE);
                holder.progressNetwork.setVisibility(View.GONE);
                break;

            case DOWNLOADING:
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.progressNetwork.setVisibility(View.VISIBLE);

                // Set click listener and update icon
                holder.pauseBtn.setImageResource(R.drawable.pause_circle_24px);
                holder.pauseBtn.setOnClickListener(view -> {
                    pauseDownload(downloadItem, holder.getBindingAdapterPosition());
                });
                break;

            case PAUSED:
                holder.pausedTxt.setVisibility(View.VISIBLE);
                holder.progressNetwork.setVisibility(View.GONE);

                // Set click listener and update icon
                holder.pauseBtn.setImageResource(R.drawable.play_circle_24px);
                holder.pauseBtn.setOnClickListener(view -> {
                    resumeDownload(downloadItem, holder.getBindingAdapterPosition());
                });
                break;

            case FAILED:
                holder.failedTxt.setVisibility(View.VISIBLE);
                holder.progressNetwork.setVisibility(View.GONE);

                // try and setup retry logic
                holder.pauseBtn.setImageResource(R.drawable.refresh_24px);
                holder.pauseBtn.setOnClickListener(view -> {
                    retryDownload(downloadItem);
                });
                break;

            default: {
                break;
            }
        }
    }

    private void cancelDownload(DownloadItem downloadItem, int adapterPosition) {
        try {
            int downloadId = downloadItem.getDownloadID();

            fetch.getDownload(downloadId, download -> {
                if (download != null) {
                    fetch.remove(downloadId);
                } else {
                    Log.e("Download", "Download not found when cancelling download: " + downloadId);
                    Toast.makeText(context, "Download not found when cancelling download", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception ignored) {

        }
    }

    public void removeItem(int downloadID, Download download) {
        for (int i = 0; i < downloadItems.size(); i++) {
            if (downloadItems.get(i).getDownloadID() == downloadID) {
                downloadItems.remove(i);
                notifyItemRemoved(i);
                // lets also delete the actual file
                break;
            }
        }
    }

    public void updateItems(List<DownloadItem> newItems) {
        this.downloadItems.clear();
        this.downloadItems.addAll(newItems.stream()
                .filter(downloadItem -> downloadItem.getProgress() < 100) // additional safety check
                .collect(Collectors.toList()));

        notifyDataSetChanged();
    }

    private void pauseDownload(DownloadItem downloadItem, int position) {
        try {
            int downloadId = downloadItem.getDownloadID();

            fetch.getDownload(downloadId, download -> {
                if (download != null) {
                    fetch.pause(downloadId);
                    downloadItem.setPaused(true);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        notifyItemChanged(position);
                    });
                } else {
                    Log.e("Download", "Download not found when pausing download: " + downloadId);
                    Toast.makeText(context, "Download not found when pausing download", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("DownloadAdapter", "Error pausing download", e);
            Toast.makeText(context, "Failed to pause download !", Toast.LENGTH_SHORT).show();
        }
    }

    private void resumeDownload(DownloadItem downloadItem, int position) {
        try {
            int downloadId = downloadItem.getDownloadID();

            fetch.getDownload(downloadId, download -> {
                // Start the download service before resuming
                startDownloadService();

                if (download != null) {
                    fetch.resume(downloadId);
                    notifyItemChanged(position);
                } else {
                    Log.e("Download", "Download not found: " + downloadId);
                    Toast.makeText(context, "Download not found", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("DownloadAdapter", "Error resuming download", e);
            Toast.makeText(context, "Failed to resume download", Toast.LENGTH_SHORT).show();
        }
    }

    private void startDownloadService() {
        Intent serviceIntent = new Intent(context, DownloadService.class);

        // Use ContextCompat to handle the service correctly for different Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public void retryDownload(DownloadItem downloadItem) {
        try {
            int downloadId = downloadItem.getDownloadID();

            // Start the download service before resuming
            startDownloadService();

            fetch.getDownload(downloadId, download -> {
                if (download != null) {
                    fetch.retry(downloadId);
                } else {
                    Toast.makeText(context, "Download not found when retrying", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateItem(DownloadItem updatedItem, int id) {
        int existingItemIndex = -1;
        for (int i = 0; i < downloadItems.size(); i++) {
            if (downloadItems.get(i).getDownloadID() == id) {
                existingItemIndex = i;
                break;
            }
        }

        if (existingItemIndex != -1) {
            // If found update the download item in position existing index
            downloadItems.set(existingItemIndex, updatedItem);
            notifyItemChanged(existingItemIndex);
        } else {
            // If not found, add the new item
            downloadItems.add(updatedItem);
            notifyItemInserted(downloadItems.size() - 1);
        }
    }

    private String formatProgressDetails(long downloadedBytes, long totalBytes) {
        return String.format("%s of %s", formatBytes(downloadedBytes), formatBytes(totalBytes));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public int getItemCount() {
        return downloadItems.size();
    }

    public void deleteFile(Download download) {
        // Then find and delete the actual file
        String filePath = download.getFile();
        File fileToDelete = new File(filePath);
        if (fileToDelete.exists()) {
            boolean deleted = fileToDelete.delete();
            if (deleted) {
                Log.d("DownloadManager", "File deleted successfully");
            } else {
                Log.e("DownloadManager", "File deletion successfully");
            }
        }
    }

    public static class DownloadViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, progressBytes, progressPercentage, progressNetwork, pausedTxt, resumeText, failedTxt, queuedTxt;
        ProgressBar progressBar;
        ImageView poster;
        ImageButton pauseBtn, cancelBtn;

        public DownloadViewHolder(@NonNull View itemView, DownloadAdapter downloadAdapter) {
            super(itemView);
            fileName = itemView.findViewById(R.id.movieCollectionTitle);
            progressBytes = itemView.findViewById(R.id.movieCollectionType);
            progressPercentage = itemView.findViewById(R.id.downloadPercentage);
            progressNetwork = itemView.findViewById(R.id.networkSpeed);
            progressBar = itemView.findViewById(R.id.downloadProgressBar);
            poster = itemView.findViewById(R.id.movieCollectionPoster);
            pauseBtn = itemView.findViewById(R.id.pauseBtn);
            cancelBtn = itemView.findViewById(R.id.deleteBtn);
            pausedTxt = itemView.findViewById(R.id.pausedTxt);
            resumeText = itemView.findViewById(R.id.resumingTxt);
            failedTxt = itemView.findViewById(R.id.failedTxt);
            queuedTxt = itemView.findViewById(R.id.queuedTxt);
        }
    }
}
