package muvi.anime.hub.adapters.movie;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.movie.SupabaseMovie;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.collectionViewHolder> {
    private final List<SupabaseMovie> supabaseMovies;
    private final Context context;
    public final onCollectionPosterClickedListener onCollectionPosterClickedListener;
    private final Activity activity;

    public CollectionAdapter(List<SupabaseMovie> supabaseMovies, Context context, onCollectionPosterClickedListener onCollectionPosterClickedListener, Activity activity) {
        this.supabaseMovies = supabaseMovies;
        this.context = context;
        this.onCollectionPosterClickedListener = onCollectionPosterClickedListener;
        this.activity = activity;
    }

    @NonNull
    @Override
    public collectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.movie_details_collection_item, parent, false);
        return new collectionViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull collectionViewHolder holder, int position) {
        SupabaseMovie supabaseMovie = supabaseMovies.get(position);
        holder.bind(supabaseMovie);
    }

    @Override
    public int getItemCount() {
        return supabaseMovies.size();
    }

    public class collectionViewHolder extends RecyclerView.ViewHolder {
        private final TextView movieTitle, movieOverView, movieVj;
        private final ImageView moviePoster;

        public collectionViewHolder(@NonNull View itemView, CollectionAdapter collectionAdapter) {
            super(itemView);
            movieTitle = itemView.findViewById(R.id.movieCollectionTitle);
            movieOverView = itemView.findViewById(R.id.movieCollectionOverview);
            moviePoster = itemView.findViewById(R.id.movieCollectionPoster);
            movieVj = itemView.findViewById(R.id.movieCollectionVj);
        }

        public void bind(final SupabaseMovie supabaseMovie) {
            movieOverView.setText(supabaseMovie.getOverview());
            movieTitle.setText(supabaseMovie.getTitle());
            movieVj.setText(supabaseMovie.getVj());

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(moviePoster)
                    .load(supabaseMovie.getPoster_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(moviePoster);

            moviePoster.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();

                if (onCollectionPosterClickedListener != null && position != RecyclerView.NO_POSITION) {
                    onCollectionPosterClickedListener.onPosterClicked(supabaseMovies.get(position));
                }
            });
        }
    }

    public interface onCollectionPosterClickedListener {
        void onPosterClicked(SupabaseMovie supabaseMovie);
    }
}
