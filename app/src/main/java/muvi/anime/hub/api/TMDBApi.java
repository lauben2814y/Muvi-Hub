package muvi.anime.hub.api;

import muvi.anime.hub.data.movie.CollectionResponse;
import muvi.anime.hub.data.movie.TMDBMovieDetails;
import muvi.anime.hub.data.movie.TMDBMovieResponse;
import muvi.anime.hub.data.tv.TMDBTv;
import muvi.anime.hub.data.tv.TMDBTvDetails;
import muvi.anime.hub.data.tv.TMDBTvResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TMDBApi {
    @GET("movie/popular")
    Call<TMDBMovieResponse> getPopularMovies(@Query("language") String language, @Query("page") int page);

    @GET("trending/movie/week")
    Call<TMDBMovieResponse> getTrendingMovies(@Query("language") String language, @Query("page") int page);

    @GET("trending/tv/week")
    Call<TMDBTvResponse> getTrendingTvs(@Query("language") String language, @Query("page") int page);

    @GET("tv/popular")
    Call<TMDBTvResponse> getPopularTvs(@Query("language") String language, @Query("page") int page);

    @GET("collection/{id}")
    Call<CollectionResponse> getCollectionDetails(
            @Path("id") int collectionId,
            @Query("language") String language
    );

    @GET("movie/{id}")
    Call<TMDBMovieDetails> getMovieDetails(
            @Path("id") int movieId,
            @Query("append_to_response") String appendToResponse
    );

    @GET("tv/{id}")
    Call<TMDBTv> getTvDetails(
            @Path("id") int tvId,
            @Query("append_to_response") String appendToResponse
    );
}
