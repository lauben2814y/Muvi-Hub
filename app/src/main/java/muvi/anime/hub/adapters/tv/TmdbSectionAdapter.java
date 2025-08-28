package muvi.anime.hub.adapters.tv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.tv.TMDBTvSection;

public class TmdbSectionAdapter extends RecyclerView.Adapter<TmdbSectionAdapter.tmdbSectionViewHolder> {
    private final List<TMDBTvSection> tmdbTvSections;
    private final Context context;

    public TmdbSectionAdapter(List<TMDBTvSection> tmdbTvSections, Context context) {
        this.tmdbTvSections = tmdbTvSections;
        this.context = context;
    }

    @NonNull
    @Override
    public tmdbSectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tmdb_tv_header, parent, false);
        return new tmdbSectionViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull tmdbSectionViewHolder holder, int position) {
        TMDBTvSection tmdbTvSection = tmdbTvSections.get(position);

        holder.headerText.setText(tmdbTvSection.getHeader());

        TmdbSectionItemsAdapter tmdbTvItemsAdapter = new TmdbSectionItemsAdapter(tmdbTvSection.getTmdbTvList(), context);
        holder.innerRecyclerView.setLayoutManager(
                new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        holder.innerRecyclerView.setAdapter(tmdbTvItemsAdapter);
    }

    public void addTDMBTvSection(TMDBTvSection tmdbTvSection) {
        tmdbTvSections.add(tmdbTvSection);
        notifyItemInserted(tmdbTvSections.size() - 1);
    }

    @Override
    public int getItemCount() {
        return tmdbTvSections.size();
    }

    static class tmdbSectionViewHolder extends RecyclerView.ViewHolder {
        TextView headerText, viewMoreBtn;
        RecyclerView innerRecyclerView;

        public tmdbSectionViewHolder(@NonNull View itemView, TmdbSectionAdapter tmdbSectionAdapter) {
            super(itemView);
            headerText = itemView.findViewById(R.id.tmdbTvSectionHeader);
            viewMoreBtn = itemView.findViewById(R.id.tmdbTvViewMoreBtn);
            innerRecyclerView = itemView.findViewById(R.id.tmdbTvInnerRecyclerView);
        }
    }
}
