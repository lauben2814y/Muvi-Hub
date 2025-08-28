package muvi.anime.hub.data;

import java.util.List;

public class Utils {

    public static String getTag() {
        return "Muvi-Hub";
    }

    public static List<String> getVjs() {
        return List.of("Vj Junior", "Vj Kevo", "Vj Zaidi", "Vj Kali", "Vj Musa", "Vj Arash", "Vj Charlie", "Vj Moon", "Vj Emmy", "Vj ICEP", "Vj Sammy", "Vj Shao Khan Lee", "Vj Khan Lee", "Vj Muba", "Vj Mark", "Vj Waza Waza", "Vj Kin", "Vj Hd", "Vj Kiwa", "Vj 03", "Vj Ks", "Vj Kimuli", "Vj Isma K", "Vj Mox", "Vj Light", "Vj Pax", "Vj Mk", "Vj Freddy", "Vj Lance", "Vj Ivo", "Vj Hitman", "Vj Kevin", "Vj Ulio", "Vj Baros", "Vj Kamran", "Vj Little T", "Vj Kriss Sweet", "Vj Jimmy", "Vj Jingo", "Vj Ks", "Vj Cabs", "Vj Jeff", "Vj Eddy", "Vj Banks", "Vj Heavy Q", "Vj Dan", "Vj Tom", "Vj Dee", "Vj Ronagie", "Vj Ryan", "Vj RK", "Vj KhanLee", "Vj Shield", "Vj Mars", "Vj Nelson", "Vj Raji", "Vj Kisule", "Vj Ashim", "Vj Martin k", "Vj Jovan", "Vj Ronnie", "Vj Pauleta", "Vj Jumpers", "Vj Tonny", "Vj Henrico", "Vj Henrico", "Vj Ghost King", "Vj Pauleta");
    }

    public static String getMovieFields() {
        return "title,id,translated,non_translated,vj,genres,logo_path,translated_2,poster_path,release_date,overview,backdrop_path,collection_id";
    }

    public static String getTrailerFields() {
        return "video_id";
    }

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

    public static String getOrder(String header) {
        return switch (header) {
            case "Recently Uploaded" -> "created_at.desc";
            case "Latest on Muvi" -> "release_date.desc";
            case "Popular" -> "popularity.desc,release_date.desc";
            case "Top Rated" -> "vote_count.desc,vote_average.desc";
            default -> null;
        };
    }

    public static String getTvOrder(String header) {
        return switch (header) {
            case "Recently Uploaded" -> "created_at.desc";
            case "Latest on Muvi" -> "first_air_date.desc";
            case "Popular" -> "popularity.desc,first_air_date.desc";
            case "Top Rated" -> "vote_count.desc,vote_average.desc";
            default -> null;
        };
    }

}
