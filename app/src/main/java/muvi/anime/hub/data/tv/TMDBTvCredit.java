package muvi.anime.hub.data.tv;

import java.io.Serializable;
import java.util.List;

public class TMDBTvCredit implements Serializable {
    private List<TMDBTvCast> cast;

    public TMDBTvCredit(List<TMDBTvCast> cast) {
        this.cast = cast;
    }

    public List<TMDBTvCast> getCast() {
        return cast;
    }

    public void setCast(List<TMDBTvCast> cast) {
        this.cast = cast;
    }
}
