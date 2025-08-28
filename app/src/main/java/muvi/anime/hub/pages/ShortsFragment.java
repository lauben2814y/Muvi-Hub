package muvi.anime.hub.pages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import muvi.anime.hub.R;
import muvi.anime.hub.adapters.CommentsAdapter;
import muvi.anime.hub.adapters.ShortsPagerAdapter;
import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.interfaces.OnCommentAdded;
import muvi.anime.hub.interfaces.OnCommentBtnClicked;
import muvi.anime.hub.managers.ShortsVideoLoader;
import muvi.anime.hub.managers.ShortsViewModel;
import muvi.anime.hub.models.Comment;
import muvi.anime.hub.models.MovieShort;
import muvi.anime.hub.models.CommentShortRequest;
import muvi.anime.hub.models.CommentShortResponse;
import muvi.anime.hub.managers.UserManager;

import muvi.anime.hub.models.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShortsFragment extends Fragment implements OnCommentBtnClicked, OnCommentAdded {
    private FirebaseUser firebaseUser;
    private UserManager userManager;
    private ShortsViewModel shortsViewModel;
    private ShortsVideoLoader shortsLoader;
    private ShortsPagerAdapter shortsAdapter;
    private ViewPager2 shortsPager;
    private static final String TAG = "MuviShorts";
    private Context context;
    private BottomSheetDialog commentsSheet;
    private RecyclerView commentsRecycler;
    private TextView commentsCount;
    private ImageButton btn_send, btn_close;
    private CommentsAdapter commentsAdapter;
    private MovieShort currentShort;
    private TextInputEditText commentField;
    private SecureService supabaseService;
    private ImageView profileImage;
    private ProgressBar addingCommentProgress;

    public ShortsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shortsViewModel = new ViewModelProvider(requireActivity()).get(ShortsViewModel.class);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_shorts, container, false);
        context = requireContext();

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        userManager = UserManager.getInstance(context);
        shortsLoader = new ShortsVideoLoader(context);

        setUpViews(view);

        supabaseService = SecureClient.getApi(context);
        shortsAdapter = new ShortsPagerAdapter(context, shortsViewModel, this, this, requireActivity());
        shortsPager.setAdapter(shortsAdapter);

        commentsRecycler.setLayoutManager(new LinearLayoutManager(context));
        commentsAdapter = new CommentsAdapter(context);
        commentsRecycler.setAdapter(commentsAdapter);

        Log.d(TAG, "onCreate ");
        statusLogs();

        if (shortsViewModel.getVideos().isEmpty()) {
            shortsLoader.loadMoreShorts(new ShortsVideoLoader.VideoLoadCallback() {
                @Override
                public void onVideosLoaded(List<MovieShort> movieShorts) {
                    Log.d(TAG, "onVideosLoaded: " + movieShorts.size());
                    int previousSize = shortsViewModel.getVideos().size();
                    shortsViewModel.setVideos(movieShorts);
                    shortsAdapter.notifyNewVideosAdded(previousSize, movieShorts.size());
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "onVideosLoadError: " + message);
                }
            }, 1);
        } else {
            restoreShorts();
        }

        shortsPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Check if were near the end of the list
                if (shortsViewModel.getVideos() != null && position >= shortsViewModel.getVideos().size() - 5) {
                    loadMoreShorts();
                }
            }

            private void loadMoreShorts() {
                Log.d(TAG, "loadMoreShorts: " + "Starting to load more shorts ...");

                if (shortsLoader.isLoading) {
                    Log.d(TAG, "loadMoreShorts: " + "Already loading ...");
                    return;
                }

                shortsLoader.loadMoreShorts(new ShortsVideoLoader.VideoLoadCallback() {
                    @Override
                    public void onVideosLoaded(List<MovieShort> movieShorts) {
                        Log.d(TAG, "onVideosLoaded: " + movieShorts.size());
                        int previousSize = shortsViewModel.getVideos().size();
                        shortsViewModel.setVideos(movieShorts);
                        shortsAdapter.notifyNewVideosAdded(previousSize, movieShorts.size());
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "onVideosLoadedError " + message);
                    }
                }, 1);
            }
        });

        // Handle send comment
        btn_send.setOnClickListener(btn -> {
            String comment = Objects.requireNonNull(commentField.getText()).toString();
            if (!comment.isEmpty()) {
                btn.setVisibility(View.GONE);
                addingCommentProgress.setVisibility(View.VISIBLE);
                sendComment(comment);
            }
        });

        return view;
    }

    private void restoreShorts() {
        statusLogs();
        MovieShort currShort = shortsViewModel.getVideos().get(shortsViewModel.getCurrentPosition());

        if (currShort != null) {
            int currPosition = shortsViewModel.getVideos().indexOf(currShort);
            if (currPosition > 0 && currPosition < shortsViewModel.getVideos().size()) {
                shortsPager.post(() -> {
                    shortsPager.setCurrentItem(currPosition, false);
                    shortsViewModel.maintainPlayerCache(currPosition);
                });
            }
        }
    }

    @SuppressLint("InflateParams")
    private void setUpViews(View view) {
        shortsPager = view.findViewById(R.id.view_pager_main);
        shortsPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        commentsSheet = new BottomSheetDialog(context);
        View commentsSheetView = getLayoutInflater().inflate(R.layout.comments_sheet, null);
        commentsSheet.setContentView(commentsSheetView);
        commentsRecycler = commentsSheetView.findViewById(R.id.recycler_comments);
        commentsCount = commentsSheetView.findViewById(R.id.tv_comment_count);
        btn_send = commentsSheetView.findViewById(R.id.btn_send);
        profileImage = commentsSheetView.findViewById(R.id.img_user_profile);
        btn_close = commentsSheetView.findViewById(R.id.btn_close);
        commentField = commentsSheetView.findViewById(R.id.edit_comment);
        addingCommentProgress = commentsSheetView.findViewById(R.id.adding_comment_progress);
    }

    private void statusLogs() {
        Log.d(TAG, "Cached Player size " + shortsViewModel.cacheSize());
        Log.d(TAG, "Videos Size " + shortsViewModel.getVideos().size());
        Log.d(TAG, "  ------------------- END ------------------------  ");
    }

    @Override
    public void onPause() {
        super.onPause();
        shortsViewModel.pauseAllPlayers();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroy called ...");
        statusLogs();
        shortsViewModel.pauseAllPlayers();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onCommentBtnClicked(MovieShort movieShort) {
        currentShort = movieShort;
        List<Comment> comments = currentShort.getComments() != null ? currentShort.getComments() : new ArrayList<>();
        int no_comments = comments != null ? comments.size() : 0;
        commentsCount.setText(no_comments + " comments");
        commentsAdapter.setComments(comments);
        commentsSheet.show();
    }

    private void sendComment(String comment) {
        userManager.getOrCreateUser(firebaseUser, new UserManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                Call<CommentShortResponse> call = supabaseService.commentShort(
                        new CommentShortRequest(
                                user.getUser_id(),
                                user.getUser_name(),
                                currentShort.getId(),
                                comment,
                                String.valueOf(System.currentTimeMillis())
                        )
                );
                call.enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<CommentShortResponse> call, @NonNull Response<CommentShortResponse> response) {
                        if (response.body() != null && response.isSuccessful()) {
                            List<Comment> newComments = response.body().getMovie_short().getComments();
                            Log.d(TAG, "onResponse: " + newComments.size());
                            Log.d(TAG, "onResponse: " + newComments.get(0).getComment());
                            currentShort.setComments(newComments);
                            commentsAdapter.setComments(newComments);
                            commentField.setText("");
                            btn_send.setVisibility(View.VISIBLE);
                            addingCommentProgress.setVisibility(View.GONE);
                        } else {
                            Log.d(TAG, "onResponse: " + response.message());

                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<CommentShortResponse> call, @NonNull Throwable throwable) {
                        Log.d(TAG, "onFailure: " + throwable.getMessage());
                        throwable.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {

            }
        });
    }

    @Override
    public void onCommentAdded(List<Comment> comments) {

    }
}