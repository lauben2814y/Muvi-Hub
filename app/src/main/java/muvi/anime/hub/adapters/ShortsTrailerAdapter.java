package muvi.anime.hub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.Trailer;

public class ShortsTrailerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Fragment fragment;
    private List<Trailer> trailers;
    private List<YouTubePlayerView> playerViews;

    private static final int VIEW_TYPE_LOADING = 0;
    private static final int VIEW_TYPE_TRAILER = 1;

    public ShortsTrailerAdapter(Fragment fragment) {
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
                    .inflate(R.layout.trailer_shorts_shimmer_item, parent, false);
            return new TrailerAdapter.ShimmerViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.trailer_shorts_list_item, parent, false);
            return new TrailerAdapter.TrailerViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Trailer trailer = trailers.get(position);

        if (holder instanceof TrailerAdapter.ShimmerViewHolder) {
            ((TrailerAdapter.ShimmerViewHolder) holder).shimmerFrameLayout.startShimmer();
        } else if (holder instanceof TrailerAdapter.TrailerViewHolder) {
            TrailerAdapter.TrailerViewHolder trailerViewHolder = (TrailerAdapter.TrailerViewHolder) holder;
            fragment.getLifecycle().addObserver(trailerViewHolder.youTubePlayerView);
            playerViews.add(trailerViewHolder.youTubePlayerView);

            trailerViewHolder.youTubePlayerView.addYouTubePlayerListener(new YouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                    youTubePlayer.cueVideo(String.valueOf(trailer.getVideo_id()), 0);
                }

                @Override
                public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState playerState) {

                }

                @Override
                public void onPlaybackQualityChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlaybackQuality playbackQuality) {

                }

                @Override
                public void onPlaybackRateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlaybackRate playbackRate) {

                }

                @Override
                public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError playerError) {

                }

                @Override
                public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float v) {

                }

                @Override
                public void onVideoDuration(@NonNull YouTubePlayer youTubePlayer, float v) {

                }

                @Override
                public void onVideoLoadedFraction(@NonNull YouTubePlayer youTubePlayer, float v) {

                }

                @Override
                public void onVideoId(@NonNull YouTubePlayer youTubePlayer, @NonNull String s) {

                }

                @Override
                public void onApiChange(@NonNull YouTubePlayer youTubePlayer) {

                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return trailers.get(position).isPlaceholder() ? VIEW_TYPE_LOADING : VIEW_TYPE_TRAILER;
    }

    @Override
    public int getItemCount() {
        return trailers.size();
    }

    public void setTrailers(List<String> videoIds) {
        trailers.clear();
        for (String videoId : videoIds) {
            trailers.add(new Trailer(videoId));
        }
        notifyDataSetChanged();
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
