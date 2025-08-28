package muvi.anime.hub.data.tv;

import java.io.Serializable;

public class TMDBTvDetails implements Serializable {
    private TMDBTvCredit credits;

    public TMDBTvDetails(TMDBTvCredit credits) {
        this.credits = credits;
    }

    public TMDBTvCredit getCredits() {
        return credits;
    }

    public void setCredits(TMDBTvCredit credits) {
        this.credits = credits;
    }
}
