package muvi.anime.hub.adapters.movie;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.pages.PlayerMovie;
import muvi.anime.hub.player.VideoMetaData;

public class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.RecentViewHolder> {
    private final Context context;
    private final List<VideoMetaData> metaData;
    private final Activity activity;

    public RecentAdapter(Context context, List<VideoMetaData> videoMetaData, Activity activity) {
        this.context = context;
        this.metaData = videoMetaData;
        this.activity = activity;
    }

    @NonNull
    @Override
    public RecentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.library_video_item, parent, false);
        return new RecentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentViewHolder holder, int position) {
        VideoMetaData videoMetaData = metaData.get(position);
        holder.bind(videoMetaData);
    }

    @Override
    public int getItemCount() {
        return metaData.size();
    }

    public class RecentViewHolder extends RecyclerView.ViewHolder {
        ImageView poster;
        ImageButton playButton;
        ProgressBar progressBar;
        TextView positionTxt, titleTxt;

        public RecentViewHolder(@NonNull View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.episodePoster);
            playButton = itemView.findViewById(R.id.playEpisode);
            progressBar = itemView.findViewById(R.id.downloadProgressBar);
            positionTxt = itemView.findViewById(R.id.position_text);
            titleTxt = itemView.findViewById(R.id.episodeNo);
        }

        public void bind(VideoMetaData videoMetaData) {
            titleTxt.setText(videoMetaData.supabaseMovie.getTitle());

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(poster)
                    .load(videoMetaData.supabaseMovie.getBackdrop_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(poster);

            playButton.setOnClickListener(view -> {
                Intent intent = new Intent(context, PlayerMovie.class);
                intent.putExtra("metadata", videoMetaData);
                activity.startActivity(intent);
            });
        }
    }
}
