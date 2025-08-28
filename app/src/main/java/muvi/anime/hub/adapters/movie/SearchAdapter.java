package muvi.anime.hub.adapters.movie;

import android.annotation.SuppressLint;
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

import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.ui.FullScreenPreloader;
import muvi.anime.hub.interfaces.OnMoviePosterClicked;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<SupabaseMovie> supabaseMovies = new ArrayList<>();
    private final Context context;
    private FullScreenPreloader preloader;
    private final OnMoviePosterClicked onMoviePosterClicked;

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    public SearchAdapter(Context context, OnMoviePosterClicked onMoviePosterClicked) {
        this.context = context;
        this.onMoviePosterClicked = onMoviePosterClicked;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.search_loading, parent, false);
            return new searchLoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.movie_details_collection_item, parent, false);
            return new searchViewHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return supabaseMovies.get(position).isPlaceHolder() ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void showLoading() {
        this.supabaseMovies.clear();
        this.supabaseMovies.add(new SupabaseMovie(true));
        notifyDataSetChanged();
    }

    public void setSupabaseMovies(List<SupabaseMovie> searchMovies) {
        this.supabaseMovies.clear();
        this.supabaseMovies.addAll(searchMovies);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof searchViewHolder) {
            SupabaseMovie supabaseMovie = supabaseMovies.get(position);
            ((searchViewHolder) holder).bind(supabaseMovie ,onMoviePosterClicked);
        }
    }

    @Override
    public int getItemCount() {
        return supabaseMovies.size();
    }

    public class searchViewHolder extends RecyclerView.ViewHolder {
        TextView title, vj, overview;
        ImageView poster;

        public searchViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.movieCollectionTitle);
            vj = itemView.findViewById(R.id.movieCollectionVj);
            overview = itemView.findViewById(R.id.movieCollectionOverview);
            poster = itemView.findViewById(R.id.movieCollectionPoster);
        }

        public void bind(SupabaseMovie supabaseMovie, OnMoviePosterClicked onMoviePosterClicked) {
            title.setText(supabaseMovie.getTitle());
            vj.setText(supabaseMovie.getVj());
            overview.setText(supabaseMovie.getOverview());

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

                if (position != RecyclerView.NO_POSITION && onMoviePosterClicked != null) {
                    onMoviePosterClicked.onMoviePosterClick(supabaseMovies.get(position));
                }
            });
        }
    }

    static class searchLoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        public searchLoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.loadingProgressSearch);
        }
    }
}
