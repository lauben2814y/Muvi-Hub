package muvi.anime.hub.managers;

public class UtilsManager {
    public static String getPage(int page) {
        int from = (page - 1) * 20;
        int to = from + 20 - 1;

        return from + "-" + to;
    }

    public static String getTvFields() {
        return "name, poster_path, first_air_date, id, seasons, created_at, vj, genres, overview, backdrop_path, logo_path, vote_average";
    }

    public static String getShortFields() {
        return "id, created_at, start, end, movie_id, translated_url, title, overview, genres, likes, dislikes";
    }
}
