package muvi.anime.hub.data;

import com.tonyodev.fetch2.Status;

public class DownloadItem {
    private String fileName;
    private String fileUrl;
    private String fileImage;
    private int progress;
    private boolean isPaused;
    private long downloadedBytes;
    private long totalBytes;
    private String networkSpeed;
    private int downloadID;
    private Status downloadStatus;

    public DownloadItem(String fileName, String fileUrl, String fileImage, int progress, boolean isPaused, long downloadedBytes, long totalBytes, String networkSpeed, int downloadID, Status downloadStatus) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileImage = fileImage;
        this.progress = progress;
        this.isPaused = isPaused;
        this.downloadedBytes = downloadedBytes;
        this.totalBytes = totalBytes;
        this.networkSpeed = networkSpeed;
        this.downloadID = downloadID;
        this.downloadStatus = downloadStatus;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileImage() {
        return fileImage;
    }

    public void setFileImage(String fileImage) {
        this.fileImage = fileImage;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public String getNetworkSpeed() {
        return networkSpeed;
    }

    public void setNetworkSpeed(String networkSpeed) {
        this.networkSpeed = networkSpeed;
    }

    public int getDownloadID() {
        return downloadID;
    }

    public void setDownloadID(int downloadID) {
        this.downloadID = downloadID;
    }

    public Status getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(Status downloadStatus) {
        this.downloadStatus = downloadStatus;
    }
}
