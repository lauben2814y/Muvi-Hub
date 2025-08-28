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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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

import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.TimeUnit;

import muvi.anime.hub.R;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.managers.AuthenticatedDataSourceFactory;
import muvi.anime.hub.managers.RewardedAdManager;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.player.VideoMetaData;
import muvi.anime.hub.player.VideoPlaybackManager;
import muvi.anime.hub.storage.UserStatsManager;

public class PlayerMovie extends AppCompatActivity {
    private UserManager userManager;

    private final Context context = this;
    private PlayerView playerView;
    private ExoPlayer player;
    private ImageButton playPauseButton;
    private ImageButton fullscreenButton;
    private SeekBar seekBar;
    private TextView positionText;
    private TextView durationText;
    private boolean isFullscreen = false;
    private boolean autoPlay = false;
    private View bufferingLayout;
    private TextView videoTitleView;
    private ImageButton closePlayerButton;
    private RelativeLayout controlsParent;
    private boolean areControlsVisible = true;
    private final Handler hideHandler = new Handler();
    private static final long CONTROLS_AUTO_HIDE_TIMEOUT = 3000; // 3 SECONDS
    private ImageButton rewindButton;
    private ImageButton forwardButton;
    private ImageButton nextButton;
    private ImageButton prevButton;
    private boolean firstAdShown;

    // Timeout
    private String videoUrl;
    private VideoMetaData currentVideo;
    private RewardedAdManager rewardedAdManager;
    private static final String TAG = Utils.getTag();
    private boolean isShowingAd = false;
    private VideoPlaybackManager playbackManager;
    private static final long PROGRESS_SAVE_INTERVAL = 5000;
    private long lastSaveTime = 0;
    private Handler handler;
    private Runnable updateProgressRunnable;

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
        setContentView(R.layout.activity_player_movie);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPlayerMovie), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, 0);
            return insets;
        });

        setUpViews();

        userManager = UserManager.getInstance(this);

        // Initialize ad handler
        adHandler = new Handler(Looper.getMainLooper());

        // Player Config
        handler = new Handler(Looper.getMainLooper());
        playbackManager = new VideoPlaybackManager(this);
        rewardedAdManager = RewardedAdManager.getInstance(context);
        statsManager = new UserStatsManager(this);

        // Check for saved progress
        setUpCustomControls();

        setupControlsVisibility();

        // Check for saved progress
        VideoMetaData videoMetaData = (VideoMetaData) getIntent().getSerializableExtra("metadata");

        if (videoMetaData != null) {
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

    private void showErrorMessage(String message) {
        // Show error in a Toast or Snack bar
        Snackbar.make(playerView, message, Snackbar.LENGTH_LONG).show();
    }

    private void showStartOver() {
        // Show Snackbar with action
        Snackbar snackbar = Snackbar.make(playerView, "Resumed playback start Over ?", Snackbar.LENGTH_LONG)
                .setAction("Okay", v -> player.seekTo(0));

        snackbar.show(); // Display the Snackbar
    }

    private void prepareAndPlay(VideoMetaData videoMetaData) {
        this.videoUrl = videoMetaData.supabaseMovie.getCurrentStreamUrl();
        this.autoPlay = true;
        this.currentVideo = videoMetaData;
        this.firstAdShown = false; // Reset for new video;

        // check for saved progress
        VideoMetaData savedProgress = playbackManager.getVideoProgress(String.valueOf(videoMetaData.supabaseMovie.getId()));
        if (savedProgress != null && savedProgress.lastPosition > 0) {
            startVideoPlayBack(videoMetaData, savedProgress.lastPosition);
            showStartOver();
        } else {
            startVideoPlayBack(videoMetaData, 0);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @SuppressLint("ClickableViewAccessibility")
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

            // Configure playerView
            playerView.setPlayer(player);
            playerView.setUseController(false);

            // Set up player listeners
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    switch (playbackState) {
                        case Player.STATE_READY:
                            // Update UI when ready
                            bufferingLayout.setVisibility(View.GONE);
                            playPauseButton.setEnabled(true);
                            forwardButton.setEnabled(true);
                            rewindButton.setEnabled(true);
                            prevButton.setEnabled(true);
                            nextButton.setEnabled(true);
                            if (autoPlay) {
                                player.play();
                                playPauseButton.setImageResource(R.drawable.pause_24px);
                                scheduleAdCheck();
                            }
                            updateProgress(); // Update time displays
                            break;

                        case Player.STATE_BUFFERING:
                            // Show loading indicator if needed
                            bufferingLayout.setVisibility(View.VISIBLE);
                            playPauseButton.setEnabled(false);
                            forwardButton.setEnabled(false);
                            rewindButton.setEnabled(false);
                            prevButton.setEnabled(false);
                            nextButton.setEnabled(false);
                            break;

                        case Player.STATE_ENDED:
                            // Stop ad scheduling when video ends
                            stopAdScheduling();
                            break;

                        case Player.STATE_IDLE:
                            // Handle playback completion
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
                public void onPlayerError(PlaybackException error) {
                    // Handle errors
                    Log.e("PlayerActivity", "Player error: " + error.getMessage());
                    // Show error message to user
                    showErrorMessage(error.getMessage());
                }
            });
        }
    }

    private void startVideoPlayBack(VideoMetaData videoMetaData, long startPosition) {
        // Set video title
        videoTitleView.setText(videoMetaData.supabaseMovie.getTitle());

        // Start ad scheduling when video starts playing
        scheduleAdCheck();

        // initialize player
        initializePlayer();

        String streamUrl = videoMetaData.supabaseMovie.getCurrentStreamUrl();

        if (streamUrl != null && !streamUrl.isEmpty()) {
            // create a media item
            MediaItem mediaItem = MediaItem.fromUri(videoMetaData.supabaseMovie.getCurrentStreamUrl());

            // clear previous media and set a new one
            player.clearMediaItems();
            player.setMediaItem(mediaItem);
            // prepare player
            player.prepare();

            // seek to the last position
            if (startPosition > 0) {
                player.seekTo(startPosition);
            }

            player.play();
            playPauseButton.setImageResource(R.drawable.pause_24px);
        } else {
            Toast.makeText(context, "Error playing video: Please report this movie to the community", Toast.LENGTH_SHORT).show();
        }
    }

    private void closePlayer() {
        if (player != null) {
            releasePlayer();
            finish();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            hideHandler.removeCallbacks(hideRunnable);
        }
    }

    private void resetHideTimer() {
        // Remove any pending hide operations
        hideHandler.removeCallbacks(hideRunnable);

        // Start the countdown to hide controls
        hideHandler.postDelayed(hideRunnable, CONTROLS_AUTO_HIDE_TIMEOUT);
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
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

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;

        if (isFullscreen) {
            // hide system ui
            hideSystemUI();
            fullscreenButton.setImageResource(R.drawable.fullscreen_exit_24px);
        } else {
            // show system ui
            showSystemUI();
            fullscreenButton.setImageResource(R.drawable.fullscreen_24px);
        }
    }

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

    private static int convertToMinutes(long timeMs) {
        return (int) (timeMs / 60000); // Convert milliseconds to minutes
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
                isShowingAd = false; // Reset flag if ad fails to show
                resumePlayback();
            }

            @Override
            public void onAdNotAvailable() {
                isShowingAd = false; // Reset flag if ad fails to show
                resumePlayback();
            }

            @Override
            public void onAdDismissed() {
                isShowingAd = false; // Reset flag after ad is closed
                resumePlayback();
            }

            @Override
            public void onAdShowed() {
                if (player != null) {
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
                playbackManager.clearVideoProgress(String.valueOf(currentVideo.supabaseMovie.getId()));
            }
            lastSaveTime = currentTime;
        }

        // Update seekbar
        seekBar.setMax((int) duration);
        seekBar.setProgress((int) position);
        // Update text views
        positionText.setText(formatTime(position));
        durationText.setText(formatTime(duration));

        // In your method
        int elapsedMinutes = convertToMinutes(position);

    }

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

    private final Runnable hideRunnable = this::hideControls;

    private void setUpCustomControls() {
        // Close player
        closePlayerButton.setOnClickListener(view -> closePlayer());

        // Play/Pause Button
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

        // Full Screen button
        fullscreenButton.setOnClickListener(view -> {
            toggleFullscreen();
            resetHideTimer();
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

        // next button
        nextButton.setOnClickListener(view -> {
            Toast.makeText(context, "No next video", Toast.LENGTH_SHORT).show();
        });

        prevButton.setOnClickListener(view -> {
            Toast.makeText(context, "No next video", Toast.LENGTH_SHORT).show();
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

    private void setUpViews() {
        playerView = findViewById(R.id.player_view);
        playPauseButton = findViewById(R.id.play_pause_button);
        fullscreenButton = findViewById(R.id.fullscreen_button);
        seekBar = findViewById(R.id.seek_bar);
        positionText = findViewById(R.id.position_text);
        durationText = findViewById(R.id.duration_text);
        bufferingLayout = findViewById(R.id.buffering_layout);
        videoTitleView = findViewById(R.id.video_title);
        closePlayerButton = findViewById(R.id.close_player_button);
        controlsParent = findViewById(R.id.controlsParent);
        forwardButton = findViewById(R.id.forward_button);
        rewindButton = findViewById(R.id.rewind_button);
        nextButton = findViewById(R.id.nextBtn);
        prevButton = findViewById(R.id.prevBtn);
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
            playerView.setPlayer(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isShowingAd && player == null) {
            initializePlayer();
        }
        handler.post(updateProgressRunnable);

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        hideHandler.removeCallbacks(hideRunnable);

        // Clean up ad scheduling
        stopAdScheduling();
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