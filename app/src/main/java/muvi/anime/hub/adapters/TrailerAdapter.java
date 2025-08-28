package muvi.anime.hub.adapters;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.Trailer;

public class TrailerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Fragment fragment;
    private final List<Trailer> trailers;
    private final List<YouTubePlayerView> playerViews;

    private static final int VIEW_TYPE_LOADING = 0;
    private static final int VIEW_TYPE_TRAILER = 1;

    public TrailerAdapter(Fragment fragment) {
        this.fragment = fragment;
        this.trailers = new ArrayList<>();
        this.playerViews = new ArrayList<>();

        // Initially add like 4 place holders
        for (int i = 0; i < 4; i++) {
            trailers.add(new Trailer(true));
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.trailer_item_shimmer, parent, false);
            return new ShimmerViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.trailer_list_item, parent, false);
            return new TrailerViewHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return trailers.get(position).isPlaceholder() ? VIEW_TYPE_LOADING : VIEW_TYPE_TRAILER;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Trailer trailer = trailers.get(position);

        if (holder instanceof ShimmerViewHolder) {
            ((ShimmerViewHolder) holder).shimmerFrameLayout.startShimmer();
        } else if (holder instanceof TrailerViewHolder trailerViewHolder) {
            // Clear any existing listener to prevent issues
            trailerViewHolder.youTubePlayerView.getYouTubePlayerWhenReady(youTubePlayer ->
                    youTubePlayer.cueVideo(trailer.getVideo_id(), 0));

            fragment.getLifecycle().addObserver(trailerViewHolder.youTubePlayerView);

            // Only add to playerViews if not already present
            if (!playerViews.contains(trailerViewHolder.youTubePlayerView)) {
                playerViews.add(trailerViewHolder.youTubePlayerView);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof TrailerViewHolder trailerViewHolder) {
            // Release the player when view is recycled
            playerViews.remove(trailerViewHolder.youTubePlayerView);
        }
    }

    @Override
    public int getItemCount() {
        return trailers.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setTrailers(List<String> videoIds) {
        trailers.clear();
        for (String videoId : videoIds) {
            trailers.add(new Trailer(videoId));
        }
        notifyDataSetChanged();

        Log.d("Muvi-Hub", "Trailers set: " + videoIds);
    }

    static class TrailerViewHolder extends RecyclerView.ViewHolder {
        YouTubePlayerView youTubePlayerView;

        public TrailerViewHolder(@NonNull View itemView) {
            super(itemView);
            youTubePlayerView = itemView.findViewById(R.id.youtube_player_view);
        }
    }

    static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerFrameLayout;

        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerFrameLayout = itemView.findViewById(R.id.shimmerLayout);
        }
    }
}
