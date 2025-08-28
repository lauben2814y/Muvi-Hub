package muvi.anime.hub.adapters;

import android.annotation.SuppressLint;
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
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.models.Comment;
import muvi.anime.hub.models.User;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
    private final List<Comment> comments;
    private final Context context;
    private final UserManager userManager;
    private FirebaseUser firebaseUser;

    public CommentsAdapter(Context context) {
        this.comments = new ArrayList<>();
        this.context = context;
        this.userManager = UserManager.getInstance(context);
        this.firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.comment_sheet_item, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(comments.get(position));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setComments(List<Comment> comments) {
        this.comments.clear();
        this.comments.addAll(comments);
        notifyDataSetChanged();
    }

    public class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvComment, tvTime, tvLikeCount;
        ImageView imgProfile, imgLike, imgDislike;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvComment = itemView.findViewById(R.id.tv_comment);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            imgProfile = itemView.findViewById(R.id.img_profile);
        }

        public void bind(Comment comment) {
            tvUsername.setText(comment.getUser_name());
            tvComment.setText(comment.getComment());
            tvTime.setText(getTimeAgo(Long.parseLong(comment.getComment_date())));

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            userManager.getOrCreateUser(firebaseUser, new UserManager.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (user.getProfile_url() != null) {
                        Glide.with(imgProfile)
                                .load(user.getProfile_url())
                                .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                                .centerCrop()
                                .into(imgProfile);
                    }
                }

                @Override
                public void onError(String error) {

                }
            });
        }

        public static String getTimeAgo(long timeInMillis) {
            long now = System.currentTimeMillis();
            long diff = now - timeInMillis;

            if (diff < 0) {
                return "Just now";
            }

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            long weeks = days / 7;
            long months = days / 30;
            long years = days / 365;

            if (seconds < 60) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " min" + (minutes > 1 ? "s" : "") + " ago";
            } else if (hours < 24) {
                return hours + " hr" + (hours > 1 ? "s" : "") + " ago";
            } else if (days < 7) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else if (weeks < 4) {
                return weeks + " week" + (weeks > 1 ? "s" : "") + " ago";
            } else if (months < 12) {
                return months + " month" + (months > 1 ? "s" : "") + " ago";
            } else {
                return years + " year" + (years > 1 ? "s" : "") + " ago";
            }
        }
    }
}
