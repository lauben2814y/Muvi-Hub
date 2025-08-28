package muvi.anime.hub.managers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import muvi.anime.hub.pages.AppUpdateActivity;

public class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private final Context context;
    private final String updateUrl;

    public UpdateChecker(Context context, String updateUrl) {
        this.context = context;
        this.updateUrl = updateUrl;
    }

    public void checkForUpdate(UpdateCheckCallback callback) {
        new CheckUpdateTask(callback).execute();
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

        public UpdateInfo(String versionName, int versionCode, String downloadUrl,
                          String changelog, boolean forceUpdate) {
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.downloadUrl = downloadUrl;
            this.changelog = changelog;
            this.forceUpdate = forceUpdate;
        }
    }

    private class CheckUpdateTask extends AsyncTask<Void, Void, UpdateInfo> {

        private UpdateCheckCallback callback;
        private String errorMessage;

        public CheckUpdateTask(UpdateCheckCallback callback) {
            this.callback = callback;
        }

        @Override
        protected UpdateInfo doInBackground(Void... voids) {
            try {
                URL url = new URL(updateUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse JSON response
                    JSONObject json = new JSONObject(response.toString());
                    String versionName = json.getString("version_name");
                    int versionCode = json.getInt("version_code");
                    String downloadUrl = json.getString("download_url");
                    String changelog = json.optString("changelog", "Bug fixes and improvements");
                    boolean forceUpdate = json.optBoolean("force_update", false);

                    // Check if update is needed
                    int currentVersionCode = getCurrentVersionCode();
                    if (versionCode > currentVersionCode) {
                        return new UpdateInfo(versionName, versionCode, downloadUrl,
                                changelog, forceUpdate);
                    }

                } else {
                    errorMessage = "Server returned error: " + responseCode;
                }

            } catch (Exception e) {
                errorMessage = "Error checking for updates: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(UpdateInfo updateInfo) {
            if (callback != null) {
                if (updateInfo != null) {
                    callback.onUpdateAvailable(updateInfo);
                } else if (errorMessage != null) {
                    callback.onError(errorMessage);
                } else {
                    callback.onNoUpdateAvailable();
                }
            }
        }
    }

    private int getCurrentVersionCode() {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void showUpdateDialog(Context context, UpdateInfo updateInfo) {
        Intent intent = new Intent(context, AppUpdateActivity.class);
        intent.putExtra("new_version_name", updateInfo.versionName);
        intent.putExtra("new_version_code", updateInfo.versionCode);
        intent.putExtra("download_url", updateInfo.downloadUrl);
        intent.putExtra("changelog", updateInfo.changelog);
        intent.putExtra("force_update", updateInfo.forceUpdate);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
