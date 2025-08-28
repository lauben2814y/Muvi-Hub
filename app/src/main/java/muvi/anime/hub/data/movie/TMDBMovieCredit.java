package muvi.anime.hub.data.movie;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.List;

public class TMDBMovieCredit implements Serializable {
    private List<TMDBMovieCast> cast;

    public TMDBMovieCredit(List<TMDBMovieCast> cast) {
        this.cast = cast;
    }

    public List<TMDBMovieCast> getCast() {
        return cast;
    }

    public void setCast(List<TMDBMovieCast> cast) {
        this.cast = cast;
    }

}
