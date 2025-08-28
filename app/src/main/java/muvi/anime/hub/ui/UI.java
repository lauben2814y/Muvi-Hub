package muvi.anime.hub.ui;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.DummyMedia;
import muvi.anime.hub.data.movie.SupabaseMovieResponse;

import android.content.Context;

public class UI {
    public UI() {

    }
    // Fetch methods
    public SupabaseMovieResponse getDummy(Context context) {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.latestmovies);
            InputStreamReader reader = new InputStreamReader(inputStream);

            Gson gson = new Gson();
            return gson.fromJson(reader, SupabaseMovieResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<DummyMedia> getDummyMedia(Context context) {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.dummy);
            InputStreamReader reader = new InputStreamReader(inputStream);

            Gson gson = new Gson();
            Type typeList = new TypeToken<List<DummyMedia>>() {}.getType();
            return gson.fromJson(reader, typeList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
