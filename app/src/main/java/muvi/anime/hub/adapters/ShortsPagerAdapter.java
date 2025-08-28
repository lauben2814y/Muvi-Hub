package muvi.anime.hub.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import muvi.anime.hub.R;
import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.interfaces.OnCommentAdded;
import muvi.anime.hub.interfaces.OnCommentBtnClicked;
import muvi.anime.hub.managers.AuthenticatedDataSourceFactory;
import muvi.anime.hub.managers.ShortsViewModel;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.models.LikeShortRequest;
import muvi.anime.hub.models.LikeShortResponse;
import muvi.anime.hub.models.MovieShort;
import muvi.anime.hub.models.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShortsPagerAdapter extends RecyclerView.Adapter<ShortsPagerAdapter.VideoPagerViewHolder> {
    private final UserManager userManager;
    private final Context context;
    private static final String TAG = "MuviShorts";
    private final ShortsViewModel shortsViewModel;
    private static final int PRELOAD_AHEAD_COUNT = 2; // Number of videos to preload ahead
    private Call<LikeShortResponse> currentLikeCall;
    private final Gson gson;
    private final OnCommentBtnClicked onCommentBtnClicked;
    private final Handler progressHandler;
    private final OnCommentAdded onCommentAdded;
    private final FirebaseUser firebaseUser;
    private final Activity activity;

    public ShortsPagerAdapter(Context context, ShortsViewModel shortsViewModel, OnCommentBtnClicked onCommentBtnClicked,
                              OnCommentAdded onCommentAdded, Activity activity) {
        this.context = context;
        this.shortsViewModel = shortsViewModel;
        this.onCommentBtnClicked = onCommentBtnClicked;
        this.onCommentAdded = onCommentAdded;
        this.userManager = UserManager.getInstance(context);
        gson = new GsonBuilder().setPrettyPrinting().create();
        progressHandler = new Handler(Looper.getMainLooper());
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        this.activity = activity;
    }

    @NonNull
    @Override
    public VideoPagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.shorts_video_item, parent, false);
        return new VideoPagerViewHolder(view);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void preloadAheadVideos(int currentPosition) {
        // Preload next videos up to PRELOAD_AHEAD_COUNT
        for (int i = 1; i <= PRELOAD_AHEAD_COUNT; i++) {
            int positionToPreload = currentPosition + i;

            // Skip if position is out of bounds
            if (positionToPreload >= shortsViewModel.getVideos().size()) {
                continue;
            }

            // Skip if player already exists for this position
            if (shortsViewModel.getPlayer(positionToPreload) != null) {
                continue;
            }

            // Create and prepare player for preloading
            MovieShort videoToPreload = shortsViewModel.getVideos().get(positionToPreload);

            ExoPlayer.Builder builder = new ExoPlayer.Builder(context)
                    .setLoadControl(buildLoadControl());

            ExoPlayer preloadPlayer = builder.build();

            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(videoToPreload.getTranslated_url())
                    .setClippingConfiguration(
                            new MediaItem.ClippingConfiguration.Builder()
                                    .setStartPositionMs(videoToPreload.getStart())
                                    .setEndPositionMs(videoToPreload.getEnd())
                                    .build()
                    )
                    .build();

            preloadPlayer.setMediaItem(mediaItem);
            preloadPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            preloadPlayer.prepare();
            preloadPlayer.setPlayWhenReady(false); // Don't play yet, just prepare

            // Cache the prepared player
            shortsViewModel.cachePlayer(positionToPreload, preloadPlayer);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onBindViewHolder(@NonNull VideoPagerViewHolder holder, int position) {
        ExoPlayer currentPlayer = shortsViewModel.getPlayer(position);
        MovieShort currentVideo = shortsViewModel.getVideos().get(position);
        DataSource.Factory dataSourceFactory = AuthenticatedDataSourceFactory.create(
                userManager.getCurrentUser().getMedia_user_name(),
                userManager.getCurrentUser().getMedia_password()
        );
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);

        // create and store player instance but don't play yet
        if (currentPlayer == null) {
            ExoPlayer.Builder builder = new ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setLoadControl(buildLoadControl());

            currentPlayer = builder.build();

            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(currentVideo.getTranslated_url())
                    .setClippingConfiguration(
                            new MediaItem.ClippingConfiguration.Builder()
                                    .setStartPositionMs(currentVideo.getStart())
                                    .setEndPositionMs(currentVideo.getEnd())
                                    .build()
                    )
                    .build();

            currentPlayer.setMediaItem(mediaItem);
            currentPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            currentPlayer.prepare();
            currentPlayer.setPlayWhenReady(false);
            shortsViewModel.cachePlayer(position, currentPlayer);
        }
        // bind data
        holder.bind(currentVideo, currentPlayer, onCommentBtnClicked, onCommentAdded);

        // Preload videos ahead when binding the current position
        preloadAheadVideos(position);
    }

    @OptIn(markerClass = UnstableApi.class)
    private LoadControl buildLoadControl() {
        return new DefaultLoadControl.Builder()
//                .setBufferDurationsMs(
//                        32 * 1024, // Min buffer
//                        64 * 1024, // Max buffer
//                        1024, // Buffer for playback
//                        1024 // Buffer for rebuffed
//                )
                .setBufferDurationsMs(
                        5_000,  // minBufferMs: 5 seconds
                        15_000, // maxBufferMs: 15 seconds
                        500,    // bufferForPlaybackMs: 0.5 sec
                        500     // bufferForPlaybackAfterRebuffedMs: 0.5 sec
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();
    }

    @Override
    public int getItemCount() {
        return shortsViewModel.getVideos().size();
    }

    public void notifyNewVideosAdded(int previousSize, int newItemsCount) {
        notifyItemRangeInserted(previousSize, newItemsCount);
        Log.d(TAG, "notifyNewVideosAdded: total size now " + shortsViewModel.getVideos().size());
    }

    @Override
    public void onViewRecycled(@NonNull VideoPagerViewHolder holder) {
        super.onViewRecycled(holder);
        int position = holder.getBindingAdapterPosition();

        ExoPlayer oldPlayer = shortsViewModel.getPlayer(position);
        if (oldPlayer != null) {
            Player.Listener oldListener = holder.getListener();
            if (oldListener != null) {
                oldPlayer.removeListener(holder.getListener());
            }
            oldPlayer.release();
            shortsViewModel.removeCachePlayer(position);
            holder.playerView.setPlayer(null);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull VideoPagerViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        int position = holder.getBindingAdapterPosition();

        ExoPlayer player = shortsViewModel.getPlayer(position);
        MovieShort currentVideo = shortsViewModel.getVideos().get(position);

        if (player != null) {
            // handle reaction
            holder.like(currentVideo);

            Log.d(TAG, "onViewAttachedToWindow: " + position + " " + currentVideo.getTitle() + " Current size: " + shortsViewModel.getVideos().size());

            // Set up listener when visible
            Player.Listener playerListener = new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    Player.Listener.super.onPlaybackStateChanged(playbackState);
                    switch (playbackState) {
                        case Player.STATE_BUFFERING:
                            holder.progressBar.setVisibility(View.VISIBLE);
                            break;
                        case Player.STATE_READY:
                            holder.progressBar.setVisibility(View.GONE);
                            break;
                        case Player.STATE_IDLE:
                        case Player.STATE_ENDED:
                            break;
                    }
                }
            };

            // Start progress tracking when player is ready
            holder.startProgressTracking(player, currentVideo, progressHandler);

            // Remove previous if any listeners
            Player.Listener oldListener = holder.getListener();
            if (oldListener != null) {
                player.removeListener(holder.getListener());
            }

            // Attach new listener
            holder.setListener(playerListener);
            player.addListener(playerListener);

            // Check if the player is already ready
            if (player.getPlaybackState() == Player.STATE_READY) {
                holder.progressBar.setVisibility(View.GONE);
            } else if (player.getPlaybackState() == Player.STATE_BUFFERING) {
                holder.progressBar.setVisibility(View.VISIBLE);
            }

            player.setPlayWhenReady(true); // Play when on screen

            // save current position / video id
            shortsViewModel.setCurrentPosition(holder.getBindingAdapterPosition());
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VideoPagerViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        int position = holder.getBindingAdapterPosition();

        ExoPlayer player = shortsViewModel.getPlayer(position);
        if (player != null) {
            player.setPlayWhenReady(false);  // Pause when off-screen
            // Remove listener to prevent memory leaks
            player.removeListener(holder.getListener());
        }
    }

    public class VideoPagerViewHolder extends RecyclerView.ViewHolder {
        PlayerView playerView;
        Player.Listener listener;
        TextView title, overview, no_likes, favorites, no_comments, positionText, durationText;
        ProgressBar progressBar;
        ImageButton likeBtn, favoritesBtn, pausePlayBtn, commentsBtn;
        SeekBar progressSeekBar;
        Runnable progressRunnable;
        ImageView imvAvatar;

        public VideoPagerViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.shorts_player_view);
            title = itemView.findViewById(R.id.shorts_title);
            progressBar = itemView.findViewById(R.id.shorts_buffering_spinner);
            overview = itemView.findViewById(R.id.movie_description);
            no_likes = itemView.findViewById(R.id.likes);
            favorites = itemView.findViewById(R.id.favorites);
            likeBtn = itemView.findViewById(R.id.like_btn);
            favoritesBtn = itemView.findViewById(R.id.favorite_btn);
            pausePlayBtn = itemView.findViewById(R.id.pause_play_btn);
            commentsBtn = itemView.findViewById(R.id.comments_btn);
            no_comments = itemView.findViewById(R.id.comment_no);
            positionText = itemView.findViewById(R.id.position_text);
            durationText = itemView.findViewById(R.id.duration_text);
            progressSeekBar = itemView.findViewById(R.id.seekBar);
            imvAvatar = itemView.findViewById(R.id.imvAvatar);
        }

        private class MediaPlayerGestureListener extends GestureDetector.SimpleOnGestureListener {
            private final ExoPlayer player;

            public MediaPlayerGestureListener(ExoPlayer previousPlayer) {
                this.player = previousPlayer;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                // Check if double tap is on left or right side of screen
                float screenWidth = playerView.getWidth();
                float tapX = e.getX();

                long currentPosition = player.getCurrentPosition();
                if (tapX < screenWidth / 2) {
                    // Double tap on left side
                    player.seekTo(currentPosition - 10000); // Seek back 10 seconds
                    player.setPlayWhenReady(true);
                    Toast.makeText(context, "Rewind 10s", Toast.LENGTH_SHORT).show();
                } else {
                    // Double tap on right side
                    player.seekTo(currentPosition + 10000); // Seek forward 10 seconds
                    player.setPlayWhenReady(true);
                    Toast.makeText(context, "Forward 10s", Toast.LENGTH_SHORT).show();
                }

                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // Detect horizontal slide
                assert e1 != null;
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // Check if it's primarily a horizontal movement
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                        if (diffX > 0) {
                            // Slide right
                            Toast.makeText(context, "Next Media", Toast.LENGTH_SHORT).show();
                        } else {
                            // Slide left
                            Toast.makeText(context, "Previous Media", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                }

                return false;
            }
        }

        public Player.Listener getListener() {
            return listener;
        }

        public void setListener(Player.Listener listener) {
            this.listener = listener;
        }

        // Add methods to start/stop progress tracking
        public void startProgressTracking(ExoPlayer player, MovieShort video, Handler handler) {
            // Cancel any existing runnable first
            stopProgressTracking(handler);

            // Create a new runnable for this holder
            progressRunnable = new Runnable() {
                @Override
                public void run() {
                    updateProgress(player, video);
                    handler.postDelayed(this, 1000);
                }
            };

            // Start tracking
            handler.post(progressRunnable);
        }

        public void stopProgressTracking(Handler handler) {
            if (progressRunnable != null) {
                handler.removeCallbacks(progressRunnable);
                progressRunnable = null;
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        public void bind(MovieShort video, ExoPlayer currentPlayer, OnCommentBtnClicked onCommentBtnClicked, OnCommentAdded onCommentAdded) {
            GestureDetector gestureDetector;
            playerView.setPlayer(currentPlayer);
            gestureDetector = new GestureDetector(context, new MediaPlayerGestureListener(currentPlayer));

            // Bind action data
            title.setText(video.getTitle());
            overview.setText(video.getOverview());
            favorites.setText(String.valueOf(video.getDislikes()));
            no_likes.setText(String.valueOf(video.getLikes()));
            no_comments.setText(String.valueOf(video.getComments() != null ? video.getComments().size() : 0));

            userManager.getOrCreateUser(firebaseUser, new UserManager.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (user.getProfile_url() != null) {
                        Glide.with(context)
                                .load(user.getProfile_url())
                                .into(imvAvatar);
                    } else {
                        Glide.with(context)
                                .load("https://cdn.framework7.io/placeholder/cats-300x300-1.jpg")
                                .into(imvAvatar);
                    }
                }

                @Override
                public void onError(String error) {

                }
            });

            // Attach click listeners
            playerView.setOnTouchListener((view, motionEvent) -> gestureDetector.onTouchEvent(motionEvent));

            playerView.setOnClickListener(v -> {
                if (currentPlayer != null) {
                    boolean isPlaying = currentPlayer.isPlaying();
                    if (isPlaying) {
                        // Currently playing, so pause it
                        currentPlayer.setPlayWhenReady(false);
                        pausePlayBtn.setVisibility(View.VISIBLE);
                        pausePlayBtn.setImageResource(R.drawable.play_arrow_24px);
                        pausePlayBtn.setOnClickListener(view -> {
                            view.setVisibility(View.GONE);
                            currentPlayer.setPlayWhenReady(true);
                        });
                    } else {
                        // Currently paused, so play it
                        pausePlayBtn.setVisibility(View.GONE);
                        currentPlayer.setPlayWhenReady(true);
                    }
                }
            });

            commentsBtn.setOnClickListener(view -> onCommentBtnClicked.onCommentBtnClicked(video));

            favoritesBtn.setOnClickListener(view -> {

            });
        }

        public void updateProgress(ExoPlayer currPlayer, MovieShort currentVideo) {
            if (currPlayer == null || currentVideo == null) return;

            long duration = currPlayer.getDuration();
            long position = currPlayer.getCurrentPosition();

            progressSeekBar.setMax((int) duration);
            progressSeekBar.setProgress((int) position);

            positionText.setText(formatTime(position));
            durationText.setText(formatTime(duration));
        }

        @SuppressLint("DefaultLocale")
        private String formatTime(long millis) {
            // If time is zero or very small, return a default value
            if (millis <= 0) {
                return "00:00";
            }

            // Format the time properly
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(minutes);

            return String.format("%02d:%02d", minutes, seconds);
        }

        // Reaction like
        private void userLikedStatus() {
            likeBtn.setImageResource(R.drawable.favorite_24px_fill);
            likeBtn.setColorFilter(ContextCompat.getColor(context, R.color.liked_color));
            likeBtn.setEnabled(false);
        }

        private void resetReactionStatus() {
            likeBtn.setImageResource(R.drawable.favorite_24px);
            likeBtn.setColorFilter(ContextCompat.getColor(context, R.color.unliked_color));
            likeBtn.setEnabled(true);
        }

        private void like(MovieShort video) {

            resetReactionStatus();

            userManager.getOrCreateUser(firebaseUser, new UserManager.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    Optional<MovieShort> resultShort = user.getLiked_shorts().stream()
                            .filter(movieShort -> movieShort.getId() == video.getId())
                            .findFirst();

                    if (resultShort.isPresent()) {
                        userLikedStatus();
                        Toast.makeText(context, "You liked this short", Toast.LENGTH_SHORT).show();
                    } else {
                        likeBtn.setOnClickListener(view -> {
                            likeBtn.setEnabled(false);

                            LikeShortRequest likeShortRequest = new LikeShortRequest(
                                    user.getUser_id(),
                                    video.getId(),
                                    video
                            );

                            if (currentLikeCall != null && !currentLikeCall.isCanceled()) {
                                currentLikeCall.cancel();
                            }

                            currentLikeCall = SecureClient.getApi(context).likeShort(likeShortRequest);
                            currentLikeCall.enqueue(new Callback<>() {
                                @Override
                                public void onResponse(@NonNull Call<LikeShortResponse> call, @NonNull Response<LikeShortResponse> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        userManager.updateCurrentUser(response.body().getUser());
                                        userLikedStatus();
                                        video.setLikes(response.body().getMovieShort().getLikes());
                                        no_likes.setText(String.valueOf(video.getLikes()));
                                        Log.d("MuviShorts", "New user data: " + gson.toJson(response.body().getUser()));
                                    }
                                }

                                @Override
                                public void onFailure(@NonNull Call<LikeShortResponse> call, @NonNull Throwable throwable) {

                                }
                            });
                        });
                    }
                }

                @Override
                public void onError(String error) {

                }
            });
        }
    }
}