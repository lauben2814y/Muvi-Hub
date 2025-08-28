package muvi.anime.hub.managers.update;

import android.content.Context;
import android.content.SharedPreferences;

public class UpdatePreferences {
    private static final String PREFS_NAME = "muvi_hub_update_prefs";
    private static final String KEY_AUTO_CHECK = "auto_check_updates";
    private static final String KEY_WIFI_ONLY = "download_wifi_only";
    private static final String KEY_LAST_CHECK = "last_update_check";
    private static final String KEY_DISMISSED_VERSION = "dismissed_version_code";
    private static final String KEY_LAST_NOTIFICATION = "last_notification_time";

    private SharedPreferences prefs;

    public UpdatePreferences(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAutoCheckEnabled() {
        return prefs.getBoolean(KEY_AUTO_CHECK, true);
    }

    public void setAutoCheckEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK, enabled).apply();
    }

    public boolean isWifiOnlyEnabled() {
        return prefs.getBoolean(KEY_WIFI_ONLY, true);
    }

    public void setWifiOnlyEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, enabled).apply();
    }

    public long getLastCheckTime() {
        return prefs.getLong(KEY_LAST_CHECK, 0);
    }

    public void setLastCheckTime(long time) {
        prefs.edit().putLong(KEY_LAST_CHECK, time).apply();
    }

    public int getDismissedVersionCode() {
        return prefs.getInt(KEY_DISMISSED_VERSION, 0);
    }

    public void setDismissedVersionCode(int versionCode) {
        prefs.edit().putInt(KEY_DISMISSED_VERSION, versionCode).apply();
    }

    public long getLastNotificationTime() {
        return prefs.getLong(KEY_LAST_NOTIFICATION, 0);
    }

    public void setLastNotificationTime(long time) {
        prefs.edit().putLong(KEY_LAST_NOTIFICATION, time).apply();
    }

    public boolean shouldCheckForUpdates() {
        if (!isAutoCheckEnabled()) {
            return false;
        }

        long lastCheck = getLastCheckTime();
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastCheck;

        // Check at most once per day, but respect minimum interval
        return timeDiff > UpdateConfig.CHECK_INTERVAL_MS;
    }

    public boolean shouldShowUpdateNotification(int versionCode) {
        // Don't show notification if user dismissed this version
        if (versionCode <= getDismissedVersionCode()) {
            return false;
        }

        // Don't spam notifications - at most once every 4 hours
        long lastNotification = getLastNotificationTime();
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastNotification;

        return timeDiff > UpdateConfig.MIN_CHECK_INTERVAL_MS;
    }

    public void clearDismissedVersions() {
        prefs.edit().remove(KEY_DISMISSED_VERSION).apply();
    }

    // Debug method to get all preferences as string
    public String getDebugInfo() {
        return "UpdatePreferences{" +
                "autoCheck=" + isAutoCheckEnabled() +
                ", wifiOnly=" + isWifiOnlyEnabled() +
                ", lastCheck=" + getLastCheckTime() +
                ", dismissedVersion=" + getDismissedVersionCode() +
                ", lastNotification=" + getLastNotificationTime() +
                '}';
    }
}
