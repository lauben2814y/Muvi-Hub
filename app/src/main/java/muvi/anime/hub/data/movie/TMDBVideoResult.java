package muvi.anime.hub.data.movie;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.List;

public class TMDBVideoResult implements Serializable {
    private List<TMDBMovieVideo> results;

    public TMDBVideoResult(List<TMDBMovieVideo> results) {
        this.results = results;
    }

    public List<TMDBMovieVideo> getResults() {
        return results;
    }

    public void setResults(List<TMDBMovieVideo> results) {
        this.results = results;
    }

}
