package muvi.anime.hub.data.tv;

import java.io.Serializable;

public class Episode implements Serializable {
    private Object episode_number;
    private String name;
    private String non_translated_url;
    private String overview;
    private Integer runtime;
    private Integer season_number;
    private String still_path;
    private String translated_url;
    private String currentStreamUrl;
    private String currentDownloadUrl;

    public Episode(Object episode_number, String name, String non_translated_url, String overview, Integer runtime, Integer season_number, String still_path, String translated_url) {
        this.episode_number = episode_number;
        this.name = name;
        this.non_translated_url = non_translated_url;
        this.overview = overview;
        this.runtime = runtime;
        this.season_number = season_number;
        this.still_path = still_path;
        this.translated_url = translated_url;
    }

    public Object getEpisode_number() {
        return episode_number;
    }

    public void setEpisode_number(Object episode_number) {
        this.episode_number = episode_number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNon_translated_url() {
        return non_translated_url != null ? non_translated_url : "";
    }

    public void setNon_translated_url(String non_translated_url) {
        this.non_translated_url = non_translated_url;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    public Integer getSeason_number() {
        return season_number;
    }

    public void setSeason_number(Integer season_number) {
        this.season_number = season_number;
    }

    public String getStill_path() {
        return "https://image.tmdb.org/t/p/w300" + still_path;
    }

    public void setStill_path(String still_path) {
        this.still_path = still_path;
    }

    public String getTranslated_url() {
        return translated_url != null ? translated_url : "";
    }

    public void setTranslated_url(String translated_url) {
        this.translated_url = translated_url;
    }

    public String getCurrentStreamUrl() {
        return currentStreamUrl;
    }

    public void setCurrentStreamUrl(String currentStreamUrl) {
        this.currentStreamUrl = currentStreamUrl;
    }

    public String getCurrentDownloadUrl() {
        return currentDownloadUrl;
    }

    public void setCurrentDownloadUrl(String currentDownloadUrl) {
        this.currentDownloadUrl = currentDownloadUrl;
    }
}
