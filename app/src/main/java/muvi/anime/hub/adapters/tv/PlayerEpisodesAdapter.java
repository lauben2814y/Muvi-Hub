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

public class PlayerEpisodesAdapter extends RecyclerView.Adapter<PlayerEpisodesAdapter.playerEpisodesViewHolder> {
    private final List<Episode> episodes = new ArrayList<>();
    private final Context context;
    public final onPlayBtnClickedListener onPlayBtnClickedListener;

    public PlayerEpisodesAdapter(Context context, onPlayBtnClickedListener onPlayBtnClickedListener) {
        this.context = context;
        this.onPlayBtnClickedListener = onPlayBtnClickedListener;
    }

    @NonNull
    @Override
    public playerEpisodesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.player_episode_item, parent, false);
        return new playerEpisodesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull playerEpisodesViewHolder holder, int position) {
        Episode episode = episodes.get(position);
        holder.bind(episode, onPlayBtnClickedListener);
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    public void setEpisodes(List<Episode> newEpisodes) {
        this.episodes.clear();
        this.episodes.addAll(newEpisodes);
        notifyDataSetChanged();
    }


    public static class playerEpisodesViewHolder extends RecyclerView.ViewHolder {
        TextView episodeNo;
        ImageButton playBtn;
        ImageView episodesPoster;

        public playerEpisodesViewHolder(@NonNull View itemView) {
            super(itemView);
            episodeNo = itemView.findViewById(R.id.episodeNo);
            playBtn = itemView.findViewById(R.id.playEpisode);
            episodesPoster = itemView.findViewById(R.id.episodePoster);
        }

        public void bind(Episode episode, onPlayBtnClickedListener listener) {
            String episodeNumber = "Episode " + episode.getEpisode_number();
            episodeNo.setText(episodeNumber);

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(episodesPoster)
                    .load(episode.getStill_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(episodesPoster);

            playBtn.setOnClickListener( view -> {
                if (listener != null) {
                    listener.onPlayBtnClicked(episode);
                }
            });
        }
    }

    public interface onPlayBtnClickedListener {
        void onPlayBtnClicked(Episode episode);
    }
 }
