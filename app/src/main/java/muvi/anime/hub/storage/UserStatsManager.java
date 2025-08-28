package muvi.anime.hub.storage;

import android.content.Context;
import android.content.SharedPreferences;

public class UserStatsManager {
    private static final String PREF_NAME = "UserStatsPrefs";
    private static final String KEY_MOVIES_WATCHED = "movies_watched";
    private static final String KEY_COINS = "coins";
    private static final String KEY_DOWNLOADS = "downloads";

    private final SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public UserStatsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public int getMoviesWatched() {
        return sharedPreferences.getInt(KEY_MOVIES_WATCHED, 0);
    }

    public void incrementMoviesWatched() {
        editor.putInt(KEY_MOVIES_WATCHED, getMoviesWatched() + 1);
        editor.apply();
    }

    public int getCoins() {
        return sharedPreferences.getInt(KEY_COINS, 0);
    }

    public void addCoins(int amount) {
        editor.putInt(KEY_COINS, getCoins() + amount);
        editor.apply();
    }

    public void deductCoins(int amount) {
        int newBalance = Math.max(0, getCoins() - amount);
        editor.putInt(KEY_COINS, newBalance);
        editor.apply();
    }

    public int getDownloads() {
        return sharedPreferences.getInt(KEY_DOWNLOADS, 0);
    }

    public void incrementDownloads() {
        editor.putInt(KEY_DOWNLOADS, getDownloads() + 1);
        editor.apply();
    }

    public void resetStats() {
        editor.clear();
        editor.apply();
    }
}
