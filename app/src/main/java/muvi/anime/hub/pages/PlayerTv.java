package muvi.anime.hub.pages;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import muvi.anime.hub.R;
import muvi.anime.hub.adapters.tv.PlayerEpisodesAdapter;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.tv.Episode;
import muvi.anime.hub.data.tv.Season;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.gestures.VideoGestureController;
import muvi.anime.hub.managers.AuthenticatedDataSourceFactory;
import muvi.anime.hub.managers.RewardedAdManager;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.player.tv.VideoMetaData;
import muvi.anime.hub.player.tv.VideoTvPlaybackManager;
import muvi.anime.hub.storage.UserStatsManager;

public class PlayerTv extends AppCompatActivity implements PlayerEpisodesAdapter.onPlayBtnClickedListener {
    private final Context context = this;
    private SupabaseTv supabaseTv;
    private final String TAG = Utils.getTag();
    private UserManager userManager;

    private RecyclerView playerEpisodesRecycler;
    private PlayerView playerView;
    private PlayerEpisodesAdapter playerEpisodesAdapter;
    private ExoPlayer player;
    private ImageButton playPauseButton;
    private ImageButton nextButton;
    private ImageButton prevButton;
    private ImageButton rewindButton;
    private ImageButton forwardButton;
    private SeekBar seekBar;
    private TextView positionText;
    private TextView durationText;
    private boolean isFullscreen = false;
    private Handler handler;
    private Runnable updateProgressRunnable;
    private boolean autoPlay = false;
    private View bufferingLayout;
    private TextView videoTitleView;
    private ImageButton closePlayerButton;
    private RelativeLayout controlsParent;
    private boolean areControlsVisible = true;
    private final Handler hideHandler = new Handler();
    private static final long CONTROLS_AUTO_HIDE_TIMEOUT = 3000; // 3 seconds

    private boolean isShowingAd = false;
    private long initialPosition = 0;
    private VideoTvPlaybackManager playbackManager;
    private VideoMetaData currentVideo;
    private static final long PROGRESS_SAVE_INTERVAL = 5000;
    private long lastSaveTime = 0;
    private RewardedAdManager rewardedAdManager;

    private UserStatsManager statsManager;

    private static final long AD_INTERVAL = 20 * 60 * 1000; // 20 minutes in milliseconds
    private long lastAdTime = 0;
    private Handler adHandler;
    private Runnable adCheckRunnable;
    private boolean adScheduled = false;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_player_tv);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.playerTv), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, 0);
            return insets;
        });

        setUpViews();

        userManager = UserManager.getInstance(this);

        // Initialize ad handler
        adHandler = new Handler(Looper.getMainLooper());

        // Player config
        rewardedAdManager = RewardedAdManager.getInstance(context);
        handler = new Handler(Looper.getMainLooper());
        playbackManager = new VideoTvPlaybackManager(context);
        playerEpisodesAdapter = new PlayerEpisodesAdapter(this, this);
        statsManager = new UserStatsManager(this);

        // set up controls
        setUpCustomControls();

        setupControlsVisibility();

        addPlayerScrollListener();

        // Data config
        supabaseTv = (SupabaseTv) getIntent().getSerializableExtra("supabasetv");
        VideoMetaData videoMetaData = (VideoMetaData) getIntent().getSerializableExtra("metadata");

        if (videoMetaData != null && supabaseTv != null) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            prepareAndPlay(videoMetaData);
        }

    }

    private void prepareAd() {
        if (rewardedAdManager.isAdLoaded()) {
            showAdAndPlay();
        }
    }

    private boolean shouldShowAd() {
        long currentTime = System.currentTimeMillis();

        // Don't show ad if one was recently shown
        if (currentTime - lastAdTime < AD_INTERVAL) {
            return false;
        }

        // Don't show ad if player is not playing
        if (player == null || !player.isPlaying()) {
            return false;
        }

        // Don't show ad if already showing one
        return !isShowingAd;
    }

    private void scheduleAdCheck() {
        if (adScheduled) return;

        adCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (shouldShowAd()) {
                    Log.d(TAG, "20 minutes elapsed, showing ad");
                    prepareAd();
                    lastAdTime = System.currentTimeMillis();
                }

                // Schedule next check in 30 seconds
                if (player != null && player.isPlaying()) {
                    adHandler.postDelayed(this, 30000); // Check every 30 seconds
                } else {
                    adScheduled = false;
                }
            }
        };

        adHandler.postDelayed(adCheckRunnable, AD_INTERVAL);
        adScheduled = true;
    }

    // New method to stop ad scheduling
    private void stopAdScheduling() {
        if (adHandler != null && adCheckRunnable != null) {
            adHandler.removeCallbacks(adCheckRunnable);
            adScheduled = false;
        }
    }

    private void addPlayerScrollListener() {
        playerEpisodesRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                resetHideTimer();
            }
        });
    }

    private void setupControlsVisibility() {
        // Set click listener on playerView
        playerView.setOnClickListener(v -> toggleControlsVisibility());

        // Optional: Set click listener on controls parent to prevent clicks from reaching playerView
        controlsParent.setOnClickListener(v -> {
            // Reset auto-hide timer when user interacts with controls
            hideControls();
        });
    }

    private void toggleControlsVisibility() {
        if (areControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        if (!areControlsVisible) {
            // Fade in animation
            controlsParent.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            controlsParent.setVisibility(View.VISIBLE);
                        }
                    });
            areControlsVisible = true;

            // Auto-hide controls after timeout
            resetHideTimer();
        }
    }

    private void setUpViews() {
        playerView = findViewById(R.id.player_view);
        playPauseButton = findViewById(R.id.play_pause_button);
        nextButton = findViewById(R.id.nextBtn);
        prevButton = findViewById(R.id.prevBtn);
        seekBar = findViewById(R.id.seek_bar);
        positionText = findViewById(R.id.position_text);
        durationText = findViewById(R.id.duration_text);
        bufferingLayout = findViewById(R.id.buffering_layout);
        videoTitleView = findViewById(R.id.video_title);
        closePlayerButton = findViewById(R.id.close_player_button);
        controlsParent = findViewById(R.id.controlsParent);
        rewindButton = findViewById(R.id.rewind_button);
        forwardButton = findViewById(R.id.forward_button);
        playerEpisodesRecycler = findViewById(R.id.playerEpisodesRecycler);
    }

    // Controls and helpers
    private void hideControls() {
        if (areControlsVisible) {
            // Fade out animation
            controlsParent.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            controlsParent.setVisibility(View.GONE);
                        }
                    });
            areControlsVisible = false;

            // Remove any pending hide operations
            hideHandler.removeCallbacks(hideRunnable);
        }
    }

    private final Runnable hideRunnable = this::hideControls;

    private void resetHideTimer() {
        // Remove any pending hide operations
        hideHandler.removeCallbacks(hideRunnable);

        // Start the countdown to hide controls
        hideHandler.postDelayed(hideRunnable, CONTROLS_AUTO_HIDE_TIMEOUT);
    }

    private Season getSeason(VideoMetaData videoMetaData) {
        int seasonNo = videoMetaData.episode.getSeason_number();

        return supabaseTv.getSeasons()
                .stream()
                .filter(season -> seasonNo == season.getSeason_number())
                .findFirst()
                .orElse(null);
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

    private void resumePlayback() {
        if (player != null && !player.isPlaying()) {
            player.play();
            // Resume ad scheduling after playback resumes
            scheduleAdCheck();
        }
    }

    private void showAdAndPlay() {
        isShowingAd = true;

        rewardedAdManager.showRewardedAd(this, new RewardedAdManager.OnAdRewardedCallback() {
            @Override
            public void onRewarded() {
                statsManager.addCoins(3);
            }

            @Override
            public void onAdFailedToShow() {
                isShowingAd = false;
                resumePlayback();
            }

            @Override
            public void onAdNotAvailable() {
                isShowingAd = false;
                resumePlayback();
            }

            @Override
            public void onAdDismissed() {
                isShowingAd = false; // Reset flag after ad is closed
                resumePlayback();
            }

            @Override
            public void onAdShowed() {
                if (player != null && player.isPlaying()) {
                    player.pause();
                }
                // Stop ad scheduling while ad is showing
                stopAdScheduling();
            }

            @Override
            public void onAdBlockerDetected() {

            }
        });
    }


    private void updateProgress() {
        if (player == null || currentVideo == null) return;

        long currentTime = System.currentTimeMillis();
        long duration = player.getDuration();
        long position = player.getCurrentPosition();

        // save video periodically
        if (currentTime - lastSaveTime >= PROGRESS_SAVE_INTERVAL) {
            // only save if we are not near the end
            if (position < duration - 10000) {
                currentVideo.lastPosition = position;
                playbackManager.saveVideoProgress(currentVideo);
            } else {
                String id = !Objects.equals(currentVideo.episode.getTranslated_url(), "") ? currentVideo.episode.getTranslated_url() : currentVideo.episode.getNon_translated_url();

                playbackManager.clearVideoProgress(id);
            }
            lastSaveTime = currentTime;
        }

        // Update seekbar
        seekBar.setMax((int) duration);
        seekBar.setProgress((int) position);
        // Update text views
        positionText.setText(formatTime(position));
        durationText.setText(formatTime(duration));
    }

    private void setUpCustomControls() {
        // Close player
        closePlayerButton.setOnClickListener(view -> closePlayer());

        // Play/Pause Button
        // Enhanced Play/Pause Button
        playPauseButton.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                playPauseButton.setImageResource(R.drawable.play_arrow_player);
                stopAdScheduling(); // Stop ad scheduling when paused
            } else {
                player.play();
                playPauseButton.setImageResource(R.drawable.pause_24px);
                scheduleAdCheck(); // Resume ad scheduling when playing
            }
            resetHideTimer();
        });

        // play prev
        prevButton.setOnClickListener(view -> {
            VideoMetaData videoMetaData = currentVideo;
            // grab season
            Season foundSeason = getSeason(videoMetaData);
            if (foundSeason != null) {
                // get index of the current video
                int currentVideoPosition = foundSeason.getEpisodes().indexOf(videoMetaData.episode);

                // Ensure currentVideoPosition is valid and not the first episode
                if (currentVideoPosition > 0) {
                    Episode prevEpisode = foundSeason.getEpisodes().get(currentVideoPosition - 1);

                    if (!prevEpisode.getTranslated_url().isEmpty()) {
                        prevEpisode.setCurrentStreamUrl(prevEpisode.getTranslated_url());
                        prepareAndPlay(new VideoMetaData(prevEpisode, 0, System.currentTimeMillis(), supabaseTv));
                    } else if (!prevEpisode.getNon_translated_url().isEmpty()) {
                        prevEpisode.setCurrentStreamUrl(prevEpisode.getNon_translated_url());
                        prepareAndPlay(new VideoMetaData(prevEpisode, 0, System.currentTimeMillis(), supabaseTv));
                    }
                } else {
                    // Optionally handle the case where there's no previous episode
                    Log.d("PrevButton", "No previous episode available.");
                }
            }
        });

        // play next
        nextButton.setOnClickListener(view -> {
            VideoMetaData videoMetaData = currentVideo;
            // grab season
            Season foundSeason = getSeason(videoMetaData);
            if (foundSeason != null) {
                // get index of the current video
                int currentVideoPosition = foundSeason.getEpisodes().indexOf(videoMetaData.episode);

                // Ensure currentVideoPosition is valid and not the last episode
                if (currentVideoPosition != -1 && currentVideoPosition < foundSeason.getEpisodes().size() - 1) {
                    Episode nextEpisode = foundSeason.getEpisodes().get(currentVideoPosition + 1);

                    if (!nextEpisode.getTranslated_url().isEmpty()) {
                        nextEpisode.setCurrentStreamUrl(nextEpisode.getTranslated_url());
                        prepareAndPlay(new VideoMetaData(nextEpisode, 0, System.currentTimeMillis(), supabaseTv));
                    } else if (!nextEpisode.getNon_translated_url().isEmpty()) {
                        nextEpisode.setCurrentStreamUrl(nextEpisode.getNon_translated_url());
                        prepareAndPlay(new VideoMetaData(nextEpisode, 0, System.currentTimeMillis(), supabaseTv));
                    }
                } else {
                    // Optionally handle the case where there's no next episode
                    Log.d("NextButton", "No next episode available.");
                }
            }
        });

        // Seekbar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    resetHideTimer();
                    player.seekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                hideHandler.removeCallbacks(hideRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                resetHideTimer();
            }
        });

        // forward button
        forwardButton.setOnClickListener(view -> {
            // Skip forward 10 seconds (or desired duration)
            long currentPosition = player.getCurrentPosition();
            long seekForwardPosition = Math.min(player.getDuration(), currentPosition + 10000);
            player.seekTo(seekForwardPosition);
            resetHideTimer();
        });

        //prev button
        rewindButton.setOnClickListener(view -> {
            // Skip backward 10 seconds (or desired duration)
            long currentPosition = player.getCurrentPosition();
            long seekBackwardPosition = Math.max(0, currentPosition - 10000);
            player.seekTo(seekBackwardPosition);
            resetHideTimer();
        });

        // Update progress periodically
        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                updateProgress();
                handler.postDelayed(this, 1000);
            }
        };
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void closePlayer() {
        if (player != null) {
            stopAdScheduling(); // Stop ad scheduling before closing
            releasePlayer();
            finish();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            hideHandler.removeCallbacks(hideRunnable);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer() {
        if (player == null) {
            DataSource.Factory dataSourceFactory = AuthenticatedDataSourceFactory.create(
                    userManager.getCurrentUser().getMedia_user_name(),
                    userManager.getCurrentUser().getMedia_password()
            );
            DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);

            // Create player instance with custom configuration
            player = new ExoPlayer.Builder(this)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build();

            // configure playerView
            playerView.setPlayer(player);
            playerView.setUseController(false);

            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    switch (playbackState) {
                        case Player.STATE_READY:
                            bufferingLayout.setVisibility(View.GONE);
                            playPauseButton.setEnabled(true);
                            rewindButton.setEnabled(true);
                            forwardButton.setEnabled(true);
                            nextButton.setEnabled(true);
                            prevButton.setEnabled(true);

                            if (autoPlay) {
                                player.play();
                                playPauseButton.setImageResource(R.drawable.pause_24px);
                                scheduleAdCheck();
                            }
                            updateProgress(); // update time displays
                            break;

                        case Player.STATE_BUFFERING:
                            // Show loading indicator if needed
                            bufferingLayout.setVisibility(View.VISIBLE);
                            playPauseButton.setEnabled(false);
                            rewindButton.setEnabled(false);
                            forwardButton.setEnabled(false);
                            nextButton.setEnabled(false);
                            prevButton.setEnabled(false);
                            break;

                        case Player.STATE_ENDED:
                            // Stop ad scheduling when video ends
                            stopAdScheduling();
                            bufferingLayout.setVisibility(View.GONE);
                            playPauseButton.setImageResource(R.drawable.play_arrow_player);
                            autoPlay = false;
                            break;

                        case Player.STATE_IDLE:
                            bufferingLayout.setVisibility(View.GONE);
                            playPauseButton.setImageResource(R.drawable.play_arrow_player);
                            autoPlay = false;
                            break;
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (isPlaying) {
                        // Resume ad scheduling when playback starts
                        scheduleAdCheck();
                    } else {
                        // Stop ad scheduling when playback is paused
                        stopAdScheduling();
                    }
                }

                @Override
                public void onPlayerErrorChanged(@Nullable PlaybackException error) {
                    // show error message to user
                    showErrorMessage();
                }
            });
        }
    }

    private void showErrorMessage() {
        // Show error in a Toast or Snack bar
        Snackbar.make(playerView, "The Link appears to be broken please report this Episode", Snackbar.LENGTH_LONG).show();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    // Initialize And Prepare Player
    private void startVideoPlayback(VideoMetaData videoMetaData, long startPosition) {
        // set video title
        String episodeNumber = "Episode " + videoMetaData.episode.getEpisode_number();
        videoTitleView.setText(episodeNumber);

        // Start ad scheduling when video starts playing
        scheduleAdCheck();

        // initialize player
        initializePlayer();

        MediaItem mediaItem = MediaItem.fromUri(videoMetaData.episode.getCurrentStreamUrl());

        // grab seasons
        int seasonNo = videoMetaData.episode.getSeason_number();
        Season foundSeason = supabaseTv.getSeasons()
                .stream()
                .filter(season -> seasonNo == season.getSeason_number())
                .findFirst()
                .orElse(null);

        // clear previous and set a new one
        player.clearMediaItems();
        player.setMediaItem(mediaItem);
        // prepare player
        player.prepare();

        // seek to the last position
        if (startPosition > 0) {
            player.seekTo(startPosition);
        }

        hideSystemUI();
        player.play();
        playPauseButton.setImageResource(R.drawable.pause_24px);

        // show episodes list
        playerEpisodesRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        playerEpisodesRecycler.setAdapter(playerEpisodesAdapter);

        if (foundSeason != null) {
            playerEpisodesAdapter.setEpisodes(foundSeason.getEpisodes());
        } else {
            Toast.makeText(context, "No season found", Toast.LENGTH_SHORT).show();
        }
    }

    private void prepareAndPlay(VideoMetaData videoMetaData) {
        this.autoPlay = true;
        this.currentVideo = videoMetaData;

        // check for saved progress
        VideoMetaData savedProgress = playbackManager.getVideoProgress(videoMetaData.episode.getCurrentStreamUrl());

        if (savedProgress != null && savedProgress.lastPosition > 0) {
            this.initialPosition = savedProgress.lastPosition; // Store initial position
            startVideoPlayback(videoMetaData, savedProgress.lastPosition);
        } else {
            this.initialPosition = 0; // Reset if starting from beginning
            startVideoPlayback(videoMetaData, 0);
        }
    }

    @Override
    public void onPlayBtnClicked(Episode episode) {
        Toast.makeText(context, "Episode play btn clicked", Toast.LENGTH_SHORT).show();

        // Helper method to check if string is null or empty
        boolean hasTranslatedUrl = !isNullOrEmpty(episode.getTranslated_url());
        boolean hasNonTranslatedUrl = !isNullOrEmpty(episode.getNon_translated_url());

        if (hasTranslatedUrl && hasNonTranslatedUrl) {
            // streamOptions(episode);
        } else {
            if (hasTranslatedUrl) {
                episode.setCurrentStreamUrl(episode.getTranslated_url());
                prepareAndPlay(new VideoMetaData(episode, 0, System.currentTimeMillis(), supabaseTv));
            } else if (hasNonTranslatedUrl) {
                episode.setCurrentStreamUrl(episode.getNon_translated_url());
                prepareAndPlay(new VideoMetaData(episode, 0, System.currentTimeMillis(), supabaseTv));
            } else {
                // Handle case where both URLs are null/empty
                Toast.makeText(context, "No stream available for this episode", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;

        if (isFullscreen) {
            // hide system ui
            hideSystemUI();
        } else {
            // show system ui
            showSystemUI();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        super.onResume();
        if (!isShowingAd && player == null) {
            initializePlayer();
        }

        handler.post(updateProgressRunnable);

        // Resume ad scheduling if player is playing
        if (player != null && player.isPlaying()) {
            scheduleAdCheck();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateProgressRunnable);
        hideHandler.removeCallbacks(hideRunnable);

        // Stop ad scheduling when activity is paused
        stopAdScheduling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        hideHandler.removeCallbacks(hideRunnable);

        // Clean up ad scheduling
        stopAdScheduling();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
            playerView.setPlayer(null);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Automatically enter fullscreen in landscape
            if (!isFullscreen) {
                toggleFullscreen();
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Exit fullscreen in portrait if it was enabled
            if (isFullscreen) {
                toggleFullscreen();
            }
        }
    }
}