package muvi.anime.hub.adapters.tv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.tv.TMDBTv;

public class TmdbSectionItemsAdapter extends RecyclerView.Adapter<TmdbSectionItemsAdapter.tmdbSectionItemsViewHolder> {
    private final List<TMDBTv> tmdbTvList;
    private final Context context;

    public TmdbSectionItemsAdapter(List<TMDBTv> tmdbTvList, Context context) {
        this.tmdbTvList = tmdbTvList;
        this.context = context;
    }

    @NonNull
    @Override
    public tmdbSectionItemsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tmdb_tv_main_item, parent, false);
        return new tmdbSectionItemsViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull tmdbSectionItemsViewHolder holder, int position) {
        TMDBTv tmdbTv = tmdbTvList.get(position);

        holder.tvTitle.setText(tmdbTv.getName());
        holder.tvReleaseDate.setText(tmdbTv.getFirst_air_date());

        Glide.with(holder.tvPoster)
                .load("http://image.tmdb.org/t/p/w500" + tmdbTv.getPoster_path())
                .centerCrop()
                .into(holder.tvPoster);
    }

    @Override
    public int getItemCount() {
        return tmdbTvList.size();
    }

    static class tmdbSectionItemsViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvReleaseDate;
        ImageView tvPoster;

        public tmdbSectionItemsViewHolder(@NonNull View itemView, TmdbSectionItemsAdapter tmdbSectionItemsAdapter) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.TMDBTvTitle);
            tvReleaseDate = itemView.findViewById(R.id.TMDBTvReleaseDate);
            tvPoster = itemView.findViewById(R.id.TMDBTvPoster);
        }
    }
}
