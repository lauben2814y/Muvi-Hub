package muvi.anime.hub.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.data.tv.SupabaseTv;

public class TvStorage {
    private static final String PREF_NAME = "TvPreferences";
    private static final String TV_KEY = "tvs";
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public TvStorage(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);;
        this.gson = new Gson();
    }

    public List<SupabaseTv> getTvs() {
        String json = sharedPreferences.getString(TV_KEY, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<SupabaseTv>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public boolean tvExists(int tvId) {
        List<SupabaseTv> tvs = getTvs();
        for (SupabaseTv supabaseTv: tvs) {
            if (supabaseTv.getId() == tvId) {
                return true;
            }
        }

        return false;
    }

    public void deleteTv(int tvId) {
        List<SupabaseTv> tvs = getTvs();
        tvs.removeIf( tv -> tv.getId() == tvId);
        saveTvs(tvs);
    }

    private void saveTvs(List<SupabaseTv> tvs) {
        String json = gson.toJson(tvs);
        sharedPreferences.edit().putString(TV_KEY, json).apply();
    }

    public void addTv(SupabaseTv supabaseTv) {
        List<SupabaseTv> tvs = getTvs();

        // Find and replace the movie if it already exists
        for (int i = 0; i < tvs.size(); i++) {
            if (tvs.get(i).getId() == supabaseTv.getId()) { // Assuming `id` is the unique identifier
                tvs.set(i, supabaseTv); // Replace existing movie
                saveTvs(tvs);
                return; // Exit after replacing
            }
        }

        tvs.add(supabaseTv);
        saveTvs(tvs);
    }
}
