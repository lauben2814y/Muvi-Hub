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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.interfaces.OnTvPosterClicked;

public class MoreAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<SupabaseTv> supabaseTvs;
    private final Context context;
    private final OnTvPosterClicked onTvPosterClicked;

    public static final int TYPE_ITEM = 0;
    public static final int TYPE_LOADING = 1;

    public MoreAdapter(List<SupabaseTv> supabaseTvs, Context context, OnTvPosterClicked onTvPosterClicked) {
        this.supabaseTvs = supabaseTvs;
        this.context = context;
        this.onTvPosterClicked = onTvPosterClicked;
    }

    @Override
    public int getItemViewType(int position) {
        return supabaseTvs.get(position).isPlaceHolder() ? TYPE_LOADING : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.tv_more_list_item, parent, false);
            return new tvViewHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.tmdb_movie_preloader, parent, false);
            return new loadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof tvViewHolder) {
            SupabaseTv supabaseTv = supabaseTvs.get(position);
            ((tvViewHolder) holder).bind(supabaseTv, onTvPosterClicked);
        }
    }

    @Override
    public int getItemCount() {
        return supabaseTvs.size();
    }

    public static class loadingViewHolder extends RecyclerView.ViewHolder {

        public loadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class tvViewHolder extends RecyclerView.ViewHolder {
        TextView title, vj, date;
        ImageView poster;

        public tvViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.moreTvTitle);
            vj = itemView.findViewById(R.id.tvMorePosterVJ);
            date = itemView.findViewById(R.id.moreTvReleaseDate);
            poster = itemView.findViewById(R.id.tvMorePoster);
        }

        public void bind(SupabaseTv supabaseTv, OnTvPosterClicked onTvPosterClicked) {
            title.setText(supabaseTv.getName());
            date.setText(supabaseTv.getFirst_air_date());
            vj.setText(supabaseTv.getVj());


            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(poster)
                    .load(supabaseTv.getPoster_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(poster);

            poster.setOnClickListener( view -> {
                int position = getBindingAdapterPosition();

                if (position != RecyclerView.NO_POSITION) {
                    onTvPosterClicked.onTvPosterClick(supabaseTvs.get(position));
                }
            });
        }
    }
}
