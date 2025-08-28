package muvi.anime.hub.adapters.tv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;

import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.ui.FullScreenPreloader;
import muvi.anime.hub.interfaces.OnTvPosterClicked;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<SupabaseTv> supabaseTvs = new ArrayList<>();
    private final Context context;
    private FullScreenPreloader preloader;
    private final OnTvPosterClicked onTvPosterClicked;

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    public SearchAdapter(Context context, OnTvPosterClicked onTvPosterClicked) {
        this.context = context;
        this.onTvPosterClicked = onTvPosterClicked;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.search_loading, parent, false);
            return new searchLoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.tv_details_collection_item, parent, false);
            return new searchViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof searchViewHolder) {
            SupabaseTv supabaseTv = supabaseTvs.get(position);
            ((searchViewHolder) holder).bind(supabaseTv, onTvPosterClicked);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return supabaseTvs.get(position).isPlaceHolder() ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    public void showLoading() {
        this.supabaseTvs.clear();
        this.supabaseTvs.add(new SupabaseTv(true));
        notifyDataSetChanged();
    }

    public void setSupabaseTvs(List<SupabaseTv> supabaseTvs) {
        this.supabaseTvs.clear();
        this.supabaseTvs.addAll(supabaseTvs);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return supabaseTvs.size();
    }

    public class searchViewHolder extends RecyclerView.ViewHolder {
        TextView name, vj, overview;
        ImageView poster;

        public searchViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.collectionTitle);
            vj = itemView.findViewById(R.id.collectionVj);
            overview = itemView.findViewById(R.id.collectionOverview);
            poster = itemView.findViewById(R.id.collectionPoster);
        }

        public void bind(SupabaseTv supabaseTv, OnTvPosterClicked onTvPosterClicked) {

            name.setText(supabaseTv.getName());
            vj.setText(supabaseTv.getVj());
            overview.setText(supabaseTv.getOverview());

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(poster)
                    .load(supabaseTv.getPoster_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(poster);

            poster.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();

                if (position != RecyclerView.NO_POSITION && onTvPosterClicked != null) {
                    onTvPosterClicked.onTvPosterClick(supabaseTvs.get(position));
                }
            });
        }
    }

    static class searchLoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;
        public searchLoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.loadingProgressSearch);
        }
    }
}
