package muvi.anime.hub.storage;

import android.content.Context;
import android.content.SharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class PreferenceHelper {
    private static final String PREF_NAME = "app_prefs";
    private static final String KEY_STATUS = "status";
    private static final String KEY_VIDEO_ID = "video_id";
    private static final String KEY_SHORT_PAGE = "shorts_page";
    private static final String KEY_ID_LIST = "id_list"; // New key for storing IDs

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private static PreferenceHelper instance;

    // Constructor
    public PreferenceHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized PreferenceHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceHelper(context.getApplicationContext());
        }
        return instance;
    }

    // Save boolean status
    public void saveStatus(boolean status) {
        editor.putBoolean(KEY_STATUS, status);
        editor.apply();
    }

    // Get boolean status
    public boolean getStatus() {
        return sharedPreferences.getBoolean(KEY_STATUS, false);
    }

    // Video ID
    public void saveCurrentVideoID(int position) {
        editor.putInt(KEY_VIDEO_ID, position);
        editor.apply();
    }

    public int getCurrentVideoID() {
        return sharedPreferences.getInt(KEY_VIDEO_ID, 0);
    }

    // Video Page
    public void saveShortsPage(int page) {
        editor.putInt(KEY_SHORT_PAGE, page);
        editor.apply();
    }

    public int getShortsPage() {
        return sharedPreferences.getInt(KEY_SHORT_PAGE, 1);
    }

    // Save ID to list
    public void saveIdToList(int id) {
        List<Integer> idList = getIdList();
        if (!idList.contains(id)) { // Avoid duplicates
            idList.add(id);
            saveIdList(idList);
        }
    }

    // Check if ID exists
    public boolean isIdExists(int id) {
        return getIdList().contains(id);
    }

    // Get saved ID list
    public List<Integer> getIdList() {
        String json = sharedPreferences.getString(KEY_ID_LIST, "[]");
        List<Integer> idList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                idList.add(jsonArray.getInt(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return idList;
    }

    // Save ID list
    private void saveIdList(List<Integer> idList) {
        JSONArray jsonArray = new JSONArray();
        for (int id : idList) {
            jsonArray.put(id);
        }
        editor.putString(KEY_ID_LIST, jsonArray.toString());
        editor.apply();
    }
}
