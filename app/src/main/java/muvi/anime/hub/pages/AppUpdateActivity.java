package muvi.anime.hub.pages;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import muvi.anime.hub.R;
import muvi.anime.hub.adapters.ImprovedFetchAdapter;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;

public class AppUpdateActivity extends AppCompatActivity {
    private MaterialTextView tvCurrentVersion;
    private MaterialTextView tvNewVersion;
    private MaterialTextView tvChangeLog;
    private MaterialTextView tvDownloadStatus;
    private MaterialTextView tvProgressText;
    private LinearProgressIndicator progressIndicator;
    private MaterialButton btnUpdate;
    private MaterialButton btnCancel;
    private MaterialButton btnInstall;
    private MaterialCardView cardProgress;

    private String downloadUrl;
    private String newVersionName;
    private int newVersionCode;
    private String changeLog;
    private File downloadedApk;

    // Your fetch downloader instance
    private ImprovedFetchAdapter fetchDownloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_app_update);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initFetchDownloader();
        loadUpdateInfo();
        setupClickListeners();
    }

    private void initViews() {
        tvCurrentVersion = findViewById(R.id.tv_current_version);
        tvNewVersion = findViewById(R.id.tv_new_version);
        tvChangeLog = findViewById(R.id.tv_changelog);
        tvDownloadStatus = findViewById(R.id.tv_download_status);
        tvProgressText = findViewById(R.id.tv_progress_text);
        progressIndicator = findViewById(R.id.progress_indicator);
        btnUpdate = findViewById(R.id.btn_update);
        btnCancel = findViewById(R.id.btn_cancel);
        btnInstall = findViewById(R.id.btn_install);
        cardProgress = findViewById(R.id.card_progress);

        // Initially hide progress elements
        cardProgress.setVisibility(View.GONE);
        btnInstall.setVisibility(View.GONE);
    }

    private void initFetchDownloader() {
        // Initialize the improved fetch downloader adapter
        fetchDownloader = new ImprovedFetchAdapter(this);

        // Set up progress callback
        fetchDownloader.setProgressCallback(new DownloadProgressCallback() {
            @Override
            public void onProgress(int progress, long downloadedBytes, long totalBytes) {
                runOnUiThread(() -> updateProgress(progress, downloadedBytes, totalBytes));
            }

            @Override
            public void onSuccess(File file) {
                runOnUiThread(() -> onDownloadSuccess(file));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> onDownloadError(error));
            }
        });
    }

    private void loadUpdateInfo() {
        // Get update info from intent or your update checker
        Intent intent = getIntent();
        newVersionName = intent.getStringExtra("new_version_name");
        newVersionCode = intent.getIntExtra("new_version_code", 0);
        downloadUrl = intent.getStringExtra("download_url");
        changeLog = intent.getStringExtra("changelog");

        // Set current version
        try {
            String currentVersion = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            tvCurrentVersion.setText("Current: v" + currentVersion);
        } catch (Exception e) {
            tvCurrentVersion.setText("Current: Unknown");
        }

        // Set new version info
        tvNewVersion.setText("Available: v" + newVersionName);
        tvChangeLog.setText(changeLog != null ? changeLog : "Bug fixes and improvements");
    }

    private void setupClickListeners() {
        btnUpdate.setOnClickListener(v -> startUpdate());
        btnCancel.setOnClickListener(v -> {
            if (fetchDownloader.isDownloading()) {
                fetchDownloader.cancelDownload();
            }
            finish();
        });
        btnInstall.setOnClickListener(v -> installApk());
    }

    private void startUpdate() {
        // Show progress UI
        cardProgress.setVisibility(View.VISIBLE);
        btnUpdate.setEnabled(false);
        tvDownloadStatus.setText("Preparing download...");

        // Start download using your fetch downloader
        String fileName = "app_update_v" + newVersionName + ".apk";
        fetchDownloader.startDownload(downloadUrl, fileName);
    }

    private void updateProgress(int progress, long downloadedBytes, long totalBytes) {
        progressIndicator.setProgress(progress);

        String downloadedMB = String.format("%.1f", downloadedBytes / (1024.0 * 1024.0));
        String totalMB = String.format("%.1f", totalBytes / (1024.0 * 1024.0));

        tvProgressText.setText(progress + "%");
        tvDownloadStatus.setText(String.format("Downloading... %s MB / %s MB", downloadedMB, totalMB));

        // Update button text
        btnCancel.setText("Cancel");
    }

    private void onDownloadSuccess(File file) {
        downloadedApk = file;

        // Update UI for successful download
        progressIndicator.setProgress(100);
        tvProgressText.setText("100%");
        tvDownloadStatus.setText("Download completed successfully!");

        // Show install button
        btnInstall.setVisibility(View.VISIBLE);
        btnUpdate.setText("Download Complete");
        btnCancel.setText("Close");

        // Haptic feedback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            findViewById(android.R.id.content).performHapticFeedback(
                    android.view.HapticFeedbackConstants.CONFIRM);
        }
    }

    private void onDownloadError(String error) {
        // Update UI for download error
        tvDownloadStatus.setText("Download failed: " + error);
        tvProgressText.setText("Failed");
        progressIndicator.setProgress(0);

        // Reset buttons
        btnUpdate.setEnabled(true);
        btnUpdate.setText("Retry Download");
        btnCancel.setText("Close");

        Toast.makeText(this, "Download failed. Please try again.", Toast.LENGTH_LONG).show();
    }

    private void installApk() {
        if (downloadedApk == null || !downloadedApk.exists()) {
            Toast.makeText(this, "APK file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7.0+
                Uri apkUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", downloadedApk);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(downloadedApk),
                        "application/vnd.android.package-archive");
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, "Cannot install APK: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fetchDownloader != null) {
            fetchDownloader.cleanup();
        }
    }

    // Interface for your fetch downloader callbacks
    public interface DownloadProgressCallback {
        void onProgress(int progress, long downloadedBytes, long totalBytes);

        void onSuccess(File file);

        void onError(String error);
    }
}