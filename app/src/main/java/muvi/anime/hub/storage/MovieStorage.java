package muvi.anime.hub.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import muvi.anime.hub.data.movie.SupabaseMovie;

public class MovieStorage {
    private static final String PREF_NAME = "MoviePreferences";
    private static final String MOVIE_KEY = "movies";
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public MovieStorage(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public List<SupabaseMovie> getMovies() {
        String json = sharedPreferences.getString(MOVIE_KEY, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<SupabaseMovie>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public boolean movieExists(int movieId) {
        List<SupabaseMovie> movies = getMovies();
        for (SupabaseMovie supabaseMovie : movies) {
            if (supabaseMovie.getId() == movieId) {
                return true;
            }
        }

        return false;
    }

    public void deleteMovie(int movieId) {
        List<SupabaseMovie> movies = getMovies();
        movies.removeIf(movie -> movie.getId() == movieId);
        saveMovies(movies);
    }

    private void saveMovies(List<SupabaseMovie> movies) {
        String json = gson.toJson(movies);
        sharedPreferences.edit().putString(MOVIE_KEY, json).apply();
    }

    public void addMovie(SupabaseMovie movie) {
        List<SupabaseMovie> movies = getMovies();

        // Find and replace the movie if it already exists
        for (int i = 0; i < movies.size(); i++) {
            if (movies.get(i).getId() == movie.getId()) { // Assuming `id` is the unique identifier
                movies.set(i, movie); // Replace existing movie
                saveMovies(movies);
                return; // Exit after replacing
            }
        }

        // If movie does not exist, add it
        movies.add(movie);
        saveMovies(movies);
    }

}
