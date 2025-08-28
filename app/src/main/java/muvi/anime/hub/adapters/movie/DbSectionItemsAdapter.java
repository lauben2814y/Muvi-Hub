package muvi.anime.hub.adapters.movie;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;

import java.util.List;
import java.util.Objects;

import muvi.anime.hub.R;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.interfaces.OnMoviePosterClicked;

public class DbSectionItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<SupabaseMovie> supabaseMovies;
    private final Context context;
    private final OnMoviePosterClicked onMoviePosterClicked;

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    public DbSectionItemsAdapter(List<SupabaseMovie> supabaseMovieList, Context context, OnMoviePosterClicked onMoviePosterClicked) {
        this.supabaseMovies = supabaseMovieList;
        this.context = context;
        this.onMoviePosterClicked = onMoviePosterClicked;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.supabase_movie_preloader, parent, false);
            return new dbItemsLoadingViewHolder(view);

        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.supabase_movie_main_item, parent, false);
            return new dbSectionItemsViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof dbSectionItemsViewHolder) {
            SupabaseMovie supabaseMovie = supabaseMovies.get(position);
            ((dbSectionItemsViewHolder) holder).bind(supabaseMovie, onMoviePosterClicked);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return Objects.equals(supabaseMovies.get(position).getIsLoading(), "Loading") ?  VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return supabaseMovies.size();
    }

    public void addSupabaseMovies(List<SupabaseMovie> newSupabaseMovies) {
        int previousSize = supabaseMovies.size();
        supabaseMovies.addAll(newSupabaseMovies);
        notifyItemRangeInserted(previousSize, newSupabaseMovies.size());
    }

    public void showLoading() {
        supabaseMovies.add(new SupabaseMovie("Loading"));
        notifyItemInserted(supabaseMovies.size() - 1);
    }

    public void hideLoading() {
        supabaseMovies.remove(supabaseMovies.size() -1);
        notifyItemRemoved(supabaseMovies.size());
    }

    public class dbSectionItemsViewHolder extends RecyclerView.ViewHolder {
        TextView movieTitle, movieReleaseDate, movieVj;
        ImageView moviePoster;

        public dbSectionItemsViewHolder(@NonNull View itemView) {
            super(itemView);
            movieTitle = itemView.findViewById(R.id.movieCastName);
            movieReleaseDate = itemView.findViewById(R.id.movieReleaseDate);
            movieVj = itemView.findViewById(R.id.moviePosterVJ);
            moviePoster = itemView.findViewById(R.id.moviePoster);
        }

        public void bind(SupabaseMovie supabaseMovie, OnMoviePosterClicked onMoviePosterClicked) {
            movieTitle.setText(supabaseMovie.getTitle());
            movieVj.setText(supabaseMovie.getVj());
            movieReleaseDate.setText(supabaseMovie.getRelease_date());

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(moviePoster)
                    .load(supabaseMovie.getPoster_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(moviePoster);

            moviePoster.setOnClickListener( view -> {
                int position = getBindingAdapterPosition();

                if (onMoviePosterClicked != null && position != RecyclerView.NO_POSITION) {
                    onMoviePosterClicked.onMoviePosterClick(supabaseMovies.get(position));
                }
            });
        }
    }

    static class dbItemsLoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;
        public dbItemsLoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
