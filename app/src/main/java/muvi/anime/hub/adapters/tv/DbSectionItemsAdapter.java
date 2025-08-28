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

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.interfaces.OnTvPosterClicked;

public class DbSectionItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<SupabaseTv> supabaseTvs;
    private final Context context;
    private final OnTvPosterClicked onTvPosterClicked;

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    public DbSectionItemsAdapter(List<SupabaseTv> supabaseTvs, Context context, OnTvPosterClicked onTvPosterClicked) {
        this.supabaseTvs = supabaseTvs;
        this.context = context;
        this.onTvPosterClicked = onTvPosterClicked;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.supabase_movie_preloader, parent, false);
            return new dbItemsLoadingViewHolder(view);

        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.supabase_tv_main_item, parent, false);
            return new dbSectionItemsViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
       if (holder instanceof dbSectionItemsViewHolder) {
           SupabaseTv supabaseTv = supabaseTvs.get(position);
           ((dbSectionItemsViewHolder) holder).bind(supabaseTv, onTvPosterClicked);
       }
    }

    public void addSupabaseTvs(List<SupabaseTv> newTvs) {
        int previousSize = supabaseTvs.size();
        supabaseTvs.addAll(newTvs);
        notifyItemRangeInserted(previousSize, newTvs.size());
    }

    @Override
    public int getItemViewType(int position) {
        return supabaseTvs.get(position).isPlaceHolder() ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return supabaseTvs.size();
    }

    public void showLoading() {
        supabaseTvs.add(new SupabaseTv(true));
        notifyItemInserted(supabaseTvs.size() - 1);
    }

    public void hideLoading() {
        supabaseTvs.remove(supabaseTvs.size() - 1);
        notifyItemRemoved(supabaseTvs.size());
    }

    public class dbSectionItemsViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvFirstAirDate, tvVj;
        ImageView tvPoster;

        public dbSectionItemsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.dbTvName);
            tvFirstAirDate = itemView.findViewById(R.id.dbTvFirstAirDate);
            tvVj = itemView.findViewById(R.id.dbTvPosterVJ);
            tvPoster = itemView.findViewById(R.id.dbTvPoster);

        }

        public void bind(SupabaseTv supabaseTv, OnTvPosterClicked onTvPosterClicked) {
            tvName.setText(supabaseTv.getName());
            tvVj.setText(supabaseTv.getVj());
            tvFirstAirDate.setText(supabaseTv.getFirst_air_date());

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(tvPoster)
                    .load(supabaseTv.getPoster_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(tvPoster);
            tvPoster.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();

                if (onTvPosterClicked != null && position != RecyclerView.NO_POSITION) {
                    onTvPosterClicked.onTvPosterClick(supabaseTvs.get(position));
                }
            });
        }
    }

    static class dbItemsLoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        public dbItemsLoadingViewHolder(@NonNull View itemView) {
            super(itemView);

        }
    }
}
