package muvi.anime.hub.pages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

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
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.models.Comment;
import muvi.anime.hub.models.CommentShortRequest;
import muvi.anime.hub.models.CommentShortResponse;
import muvi.anime.hub.models.MovieShort;
import muvi.anime.hub.models.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@UnstableApi
public class ShortsActivity extends AppCompatActivity implements OnCommentBtnClicked, OnCommentAdded {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shorts);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.shorts_activity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        shortsViewModel = new ViewModelProvider(this).get(ShortsViewModel.class);
        context = this;

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        userManager = UserManager.getInstance(context);
        shortsLoader = new ShortsVideoLoader(context);

        setUpViews();

        supabaseService = SecureClient.getApi(context);
        shortsAdapter = new ShortsPagerAdapter(context, shortsViewModel, this, this, this);
        shortsPager.setAdapter(shortsAdapter);

        commentsRecycler.setLayoutManager(new LinearLayoutManager(context));
        commentsAdapter = new CommentsAdapter(context);
        commentsRecycler.setAdapter(commentsAdapter);

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
    }

    private void setUpViews() {
        shortsPager = findViewById(R.id.view_pager_main);
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