package muvi.anime.hub.managers.update;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import muvi.anime.hub.api.UpdateApiService;
import muvi.anime.hub.models.GitHubRelease;
import muvi.anime.hub.models.UpdateResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class RetrofitUpdateChecker {
    private static final String TAG = "RetrofitUpdateChecker";
    private static final String GITHUB_API_BASE = "https://api.github.com/";
    private final Context context;
    private final String backendBaseUrl;
    private final String githubOwner;
    private final String githubRepo;
    private UpdateApiService apiService;
    private UpdateApiService githubApiService;

    public RetrofitUpdateChecker(Context context) {
        this.context = context;
        this.backendBaseUrl = UpdateConfig.BACKEND_URL;
        this.githubOwner = UpdateConfig.GITHUB_OWNER;
        this.githubRepo = UpdateConfig.GITHUB_REPO;

        initializeRetrofit();
    }

    private void initializeRetrofit() {
        // Create OkHttp client with logging (only in debug)
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        if (UpdateConfig.DEBUG_UPDATES) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            clientBuilder.addInterceptor(logging);
        }

        // Add User-Agent header
        clientBuilder.addInterceptor(chain -> {
            return chain.proceed(
                    chain.request()
                            .newBuilder()
                            .header("User-Agent", UpdateConfig.USER_AGENT)
                            .build()
            );
        });

        OkHttpClient okHttpClient = clientBuilder.build();

        // Backend API retrofit instance
        if (backendBaseUrl != null && !backendBaseUrl.isEmpty()) {
            Retrofit backendRetrofit = new Retrofit.Builder()
                    .baseUrl(backendBaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = backendRetrofit.create(UpdateApiService.class);
        }

        // GitHub API retrofit instance (fallback)
        Retrofit githubRetrofit = new Retrofit.Builder()
                .baseUrl(GITHUB_API_BASE)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        githubApiService = githubRetrofit.create(UpdateApiService.class);
    }

    public void checkForUpdate(UpdateCheckCallback callback) {
        if (UpdateConfig.DEBUG_UPDATES) {
            Log.d(TAG, "Starting update check for Muvi Hub...");
        }

        // Try backend first, fallback to GitHub if backend is unavailable
        if (apiService != null) {
            checkWithBackend(callback);
        } else {
            checkWithGitHub(callback);
        }
    }

    public void checkWithBackend(UpdateCheckCallback callback) {
        try {
            int currentVersionCode = getCurrentVersionCode();

            if (UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Checking backend: " + backendBaseUrl);
                Log.d(TAG, "Package: " + UpdateConfig.PACKAGE_NAME);
                Log.d(TAG, "Current version: " + currentVersionCode);
            }

            Call<UpdateResponse> call = apiService.checkForUpdate(
                    UpdateConfig.PACKAGE_NAME,
                    currentVersionCode,
                    "android"
            );

            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<UpdateResponse> call, @NonNull Response<UpdateResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        UpdateResponse updateResponse = response.body();

                        if (UpdateConfig.DEBUG_UPDATES) {
                            Log.d(TAG, "Backend response: has_update=" + updateResponse.hasUpdate);
                            if (updateResponse.hasUpdate) {
                                Log.d(TAG, "Available version: " + updateResponse.versionName +
                                        " (code: " + updateResponse.versionCode + ")");
                            }
                        }

                        if (updateResponse.hasUpdate) {
                            UpdateInfo updateInfo = new UpdateInfo(
                                    updateResponse.versionName,
                                    updateResponse.versionCode,
                                    updateResponse.downloadUrl,
                                    updateResponse.changelog,
                                    updateResponse.forceUpdate,
                                    updateResponse.fileSize,
                                    updateResponse.fileChecksum
                            );
                            callback.onUpdateAvailable(updateInfo);
                        } else {
                            callback.onNoUpdateAvailable();
                        }
                    } else {
                        Log.w(TAG, "Backend check failed with status: " + response.code() +
                                ", falling back to GitHub");
                        checkWithGitHub(callback);
                    }
                }

                @Override
                public void onFailure(Call<UpdateResponse> call, Throwable t) {
                    Log.e(TAG, "Backend API call failed: " + t.getMessage());
                    if (UpdateConfig.DEBUG_UPDATES) {
                        Log.e(TAG, "Falling back to GitHub", t);
                    }
                    checkWithGitHub(callback);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error checking with backend", e);
            checkWithGitHub(callback);
        }
    }

    public void checkWithGitHub(UpdateCheckCallback callback) {
        if (UpdateConfig.DEBUG_UPDATES) {
            Log.d(TAG, "Checking GitHub fallback...");
        }

        Call<GitHubRelease> call = githubApiService.getLatestRelease(
                githubOwner, githubRepo, "application/vnd.github.v3+json");

        call.enqueue(new Callback<GitHubRelease>() {
            @Override
            public void onResponse(@NonNull Call<GitHubRelease> call, @NonNull Response<GitHubRelease> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GitHubRelease release = response.body();

                    // Skip draft or pre release versions
                    if (release.draft || release.prerelease) {
                        if (UpdateConfig.DEBUG_UPDATES) {
                            Log.d(TAG, "Skipping draft/prerelease: " + release.tagName);
                        }
                        callback.onNoUpdateAvailable();
                        return;
                    }

                    GitHubRelease.GitHubAsset apkAsset = release.getApkAsset();
                    if (apkAsset == null) {
                        callback.onError("No APK found in the latest release");
                        return;
                    }

                    int releaseVersionCode = release.extractVersionCode();
                    int currentVersionCode = getCurrentVersionCode();

                    if (UpdateConfig.DEBUG_UPDATES) {
                        Log.d(TAG, "GitHub: current=" + currentVersionCode +
                                ", latest=" + releaseVersionCode);
                    }

                    if (releaseVersionCode > currentVersionCode) {
                        UpdateInfo updateInfo = new UpdateInfo(
                                release.tagName.replace("v", ""),
                                releaseVersionCode,
                                apkAsset.browserDownloadUrl,
                                release.body != null ? release.body : "Check the release notes on GitHub",
                                false, // GitHub releases are not force updates by default
                                apkAsset.size,
                                null // No checksum from GitHub API
                        );
                        callback.onUpdateAvailable(updateInfo);
                    } else {
                        callback.onNoUpdateAvailable();
                    }
                } else {
                    callback.onError("Failed to check GitHub releases: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<GitHubRelease> call, @NonNull Throwable t) {
                Log.e(TAG, "GitHub API call failed", t);
                callback.onError("Failed to check for updates: " + t.getMessage());
            }
        });
    }

    private int getCurrentVersionCode() {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get current version code", e);
            return 0;
        }
    }

    public interface UpdateCheckCallback {
        void onUpdateAvailable(UpdateInfo updateInfo);
        void onNoUpdateAvailable();
        void onError(String error);
    }

    public static class UpdateInfo {
        public String versionName;
        public int versionCode;
        public String downloadUrl;
        public String changelog;
        public boolean forceUpdate;
        public long fileSize;
        public String fileChecksum;

        public UpdateInfo(String versionName, int versionCode, String downloadUrl,
                          String changelog, boolean forceUpdate, long fileSize, String fileChecksum) {
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.downloadUrl = downloadUrl;
            this.changelog = changelog;
            this.forceUpdate = forceUpdate;
            this.fileSize = fileSize;
            this.fileChecksum = fileChecksum;
        }

        @NonNull
        @Override
        public String toString() {
            return "UpdateInfo{" +
                    "versionName='" + versionName + '\'' +
                    ", versionCode=" + versionCode +
                    ", downloadUrl='" + downloadUrl + '\'' +
                    ", forceUpdate=" + forceUpdate +
                    ", fileSize=" + fileSize +
                    '}';
        }
    }
}
