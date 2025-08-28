package muvi.anime.hub.adapters.tv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;

import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.tv.Episode;

public class EpisodesAdapter extends RecyclerView.Adapter<EpisodesAdapter.episodesViewHolder> {
    private List<Episode> episodeList = new ArrayList<>();
    private final Context context;
    public final onPlayEpisodeBtnClickedListener onPlayEpisodeBtnClickedListener;
    public final onDownloadEpisodeBtnClicked onDownloadEpisodeBtnClicked;

    public EpisodesAdapter(Context context, EpisodesAdapter.onPlayEpisodeBtnClickedListener onPlayEpisodeBtnClickedListener, EpisodesAdapter.onDownloadEpisodeBtnClicked onDownloadEpisodeBtnClicked) {
        this.context = context;
        this.onPlayEpisodeBtnClickedListener = onPlayEpisodeBtnClickedListener;
        this.onDownloadEpisodeBtnClicked = onDownloadEpisodeBtnClicked;
    }

    @NonNull
    @Override
    public episodesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.tv_episode_list_item, parent, false);
        return new episodesViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull episodesViewHolder holder, int position) {
        Episode episode = episodeList.get(position);
        holder.bind(episode);
    }

    public void setEpisodeList(List<Episode> newEpisodeList) {
        this.episodeList.clear();
        this.episodeList.addAll(newEpisodeList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return episodeList.size();
    }

    public class episodesViewHolder extends RecyclerView.ViewHolder {
        TextView episodeNo, episodeName, episodeOverview;
        ImageView episodePath, downloadEpisodeBtn;
        ImageButton playEpisodeBtn;

        public episodesViewHolder(@NonNull View itemView, EpisodesAdapter episodesAdapter) {
            super(itemView);
            episodeNo = itemView.findViewById(R.id.episodeNo);
            episodeOverview = itemView.findViewById(R.id.episodeOverview);
            episodeName = itemView.findViewById(R.id.episodeName);
            episodePath = itemView.findViewById(R.id.episodePoster);
            playEpisodeBtn = itemView.findViewById(R.id.playEpisodeBtn);
            downloadEpisodeBtn = itemView.findViewById(R.id.downloadEpisodeBtn);
        }

        public void bind(final Episode episode) {
            String episodeNumber = "Episode " + episode.getEpisode_number();

            episodeNo.setText(episodeNumber);
            episodeName.setText(episode.getName());
            episodeOverview.setText(episode.getOverview());

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(episodePath)
                    .load(episode.getStill_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(episodePath);

            playEpisodeBtn.setOnClickListener(view -> {
                if (onPlayEpisodeBtnClickedListener != null) {
                    onPlayEpisodeBtnClickedListener.onPlayEpisodeClicked(episode);
                }
            });

            downloadEpisodeBtn.setOnClickListener(view -> {
                if (onDownloadEpisodeBtnClicked != null) {
                    onDownloadEpisodeBtnClicked.onDownloadEpisodeClicked(episode);
                }
            });
        }
    }

    public interface onPlayEpisodeBtnClickedListener {
        void onPlayEpisodeClicked(Episode episode);
    }

    public interface onDownloadEpisodeBtnClicked {
        void onDownloadEpisodeClicked(Episode episode);
    }
}
