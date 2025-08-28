package muvi.anime.hub.data.tv;

import java.io.Serializable;
import java.util.List;

public class Season implements Serializable {
    private Integer episode_count;
    private List<Episode> episodes;
    private String name;
    private String overview;
    private String poster_path;
    private int season_number;

    public Season(Integer episode_count, List<Episode> episodes, String name, String overview, String poster_path, int season_number) {
        this.episode_count = episode_count;
        this.episodes = episodes;
        this.name = name;
        this.overview = overview;
        this.poster_path = poster_path;
        this.season_number = season_number;
    }

    public Integer getEpisode_count() {
        return episode_count;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public String getName() {
        return name;
    }

    public String getOverview() {
        return overview;
    }

    public String getPoster_path() {
        return poster_path;
    }

    public int getSeason_number() {
        return season_number;
    }

}
