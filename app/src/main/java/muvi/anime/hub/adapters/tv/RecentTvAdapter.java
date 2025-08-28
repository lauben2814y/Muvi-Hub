package muvi.anime.hub.adapters.tv;

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
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.pages.PlayerTv;
import muvi.anime.hub.player.tv.VideoMetaData;

public class RecentTvAdapter extends RecyclerView.Adapter<RecentTvAdapter.RecentViewHolder> {
    private final Context context;
    private final List<VideoMetaData> metaData;
    private final Activity activity;

    public RecentTvAdapter(Context context, List<VideoMetaData> metaData, Activity activity) {
        this.context = context;
        this.metaData = metaData;
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
        SupabaseTv supabaseTv = videoMetaData.supabaseTv;
        holder.bind(videoMetaData, supabaseTv);
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

        public void bind(VideoMetaData videoMetaData, SupabaseTv supabaseTv) {
            String title = videoMetaData.supabaseTv.getName() + "S"+ videoMetaData.episode.getSeason_number() + "E" + videoMetaData.episode.getEpisode_number();
            titleTxt.setText(title);

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(poster)
                    .load(videoMetaData.episode.getStill_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(poster);

            playButton.setOnClickListener(view -> {
                Intent intent = new Intent(context, PlayerTv.class);
                intent.putExtra("metadata", videoMetaData);
                intent.putExtra("supabasetv", supabaseTv);
                activity.startActivity(intent);
            });
        }
    }
}
