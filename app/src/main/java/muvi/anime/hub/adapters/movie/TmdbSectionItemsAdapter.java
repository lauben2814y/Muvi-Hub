package muvi.anime.hub.adapters.movie;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.movie.TMDBMovie;

public class TmdbSectionItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<TMDBMovie> tmdbMovieList;
    private final Context context;

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_LOADING = 1;
    private boolean isLoading = false;

    public TmdbSectionItemsAdapter(List<TMDBMovie> tmdbMovieList, Context context) {
        this.tmdbMovieList = tmdbMovieList;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return isLoading && position == tmdbMovieList.size() ? TYPE_LOADING : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tmdb_movie_preloader, parent, false);
            return new MovieTMDBItemsLoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tmdb_movie_main_item, parent, false);
            return new MovieTMDBItemsViewHolder(view, this);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MovieTMDBItemsViewHolder) {
            MovieTMDBItemsViewHolder movieTMDBItemsViewHolder = (MovieTMDBItemsViewHolder) holder;
            TMDBMovie tmdbMovie = tmdbMovieList.get(position);

            movieTMDBItemsViewHolder.movieTitle.setText(tmdbMovie.getTitle());
            movieTMDBItemsViewHolder.movieReleaseDate.setText(tmdbMovie.getRelease_date());

            Glide.with(movieTMDBItemsViewHolder.moviePoster)
                    .load("http://image.tmdb.org/t/p/w500" + tmdbMovie.getPoster_path())
                    .centerCrop()
                    .into(movieTMDBItemsViewHolder.moviePoster);

        }
    }

    public void addItems(List<TMDBMovie> newtmdbMovieList) {
        int startPosition = tmdbMovieList.size();
        tmdbMovieList.addAll(newtmdbMovieList);
        notifyItemRangeInserted(startPosition, newtmdbMovieList.size());
    }

    @Override
    public int getItemCount() {
        return tmdbMovieList.size();
    }

    public void showLoading() {
        if (isLoading) return;
        isLoading = true;
        notifyItemInserted(tmdbMovieList.size());
    }

    public void hideLoading() {
        if (isLoading) {
            isLoading = false;
            notifyItemRemoved(tmdbMovieList.size());
        }
    }

    static class MovieTMDBItemsViewHolder extends RecyclerView.ViewHolder {
        TextView movieTitle, movieReleaseDate, movieVj;
        ImageView moviePoster;

        public MovieTMDBItemsViewHolder(@NonNull View itemView, TmdbSectionItemsAdapter movieTMDBItemsAdapter) {
            super(itemView);
            movieTitle = itemView.findViewById(R.id.TMDBMovieTitle);
            movieReleaseDate = itemView.findViewById(R.id.TMDBMovieReleaseDate);
            moviePoster = itemView.findViewById(R.id.TMDBMoviePoster);
        }
    }

    static class MovieTMDBItemsLoadingViewHolder extends RecyclerView.ViewHolder {

        public MovieTMDBItemsLoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
