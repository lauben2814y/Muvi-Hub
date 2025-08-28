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

import muvi.anime.hub.R;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.interfaces.OnMoviePosterClicked;

public class MoreAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<SupabaseMovie> supabaseMovies;
    private final Context context;
    private final OnMoviePosterClicked onMoviePosterClicked;

    public static final int TYPE_ITEM = 0;
    public static final int TYPE_LOADING = 1;

    public MoreAdapter(List<SupabaseMovie> supabaseMovies, Context context, OnMoviePosterClicked onMoviePosterClicked) {
        this.supabaseMovies = supabaseMovies;
        this.context = context;
        this.onMoviePosterClicked = onMoviePosterClicked;
    }

    @Override
    public int getItemViewType(int position) {
        return supabaseMovies.get(position).isPlaceHolder() ? TYPE_LOADING : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.movie_more_list_item, parent, false);
            return new movieViewHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.tmdb_movie_preloader, parent, false);
            return new loadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof movieViewHolder) {
            SupabaseMovie supabaseMovie = supabaseMovies.get(position);
            ((movieViewHolder) holder).bind(supabaseMovie, onMoviePosterClicked);
        } else if (holder instanceof loadingViewHolder) {

        }
    }

    @Override
    public int getItemCount() {
        return supabaseMovies.size();
    }

    public static class loadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        public loadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.tmdbMovieProgressBar);
        }
    }

    public class movieViewHolder extends RecyclerView.ViewHolder {
        TextView title, vj, date;
        ImageView poster;

        public movieViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.moreMovieTitle);
            vj = itemView.findViewById(R.id.movieMorePosterVJ);
            date = itemView.findViewById(R.id.moreMovieReleaseDate);
            poster = itemView.findViewById(R.id.movieMorePoster);
        }

        public void bind(SupabaseMovie supabaseMovie, OnMoviePosterClicked onMoviePosterClicked) {
            title.setText(supabaseMovie.getTitle());
            date.setText(supabaseMovie.getRelease_date());
            vj.setText(supabaseMovie.getVj());

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(poster)
                    .load(supabaseMovie.getPoster_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(poster);

            poster.setOnClickListener( view -> {
                int position = getBindingAdapterPosition();

                if (position != RecyclerView.NO_POSITION) {
                    onMoviePosterClicked.onMoviePosterClick(supabaseMovies.get(position));
                }
            });
        }
    }
}
