package muvi.anime.hub.adapters.tv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.tv.Season;
import muvi.anime.hub.interfaces.OnSeasonBtnClickListener;

public class SeasonSheetBtnsAdapter extends RecyclerView.Adapter<SeasonSheetBtnsAdapter.seasonSheetViewHolder> {
    private final List<Season> seasons;
    private final Context context;
    private final OnSeasonBtnClickListener onSeasonBtnClickListener;

    public SeasonSheetBtnsAdapter(List<Season> seasons, Context context, OnSeasonBtnClickListener onSeasonBtnClickListener) {
        this.seasons = seasons;
        this.context = context;
        this.onSeasonBtnClickListener = onSeasonBtnClickListener;
    }

    @NonNull
    @Override
    public seasonSheetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.tv_season_sheet_item, parent, false);

        return new seasonSheetViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull seasonSheetViewHolder holder, int position) {
        holder.bind(seasons.get(position));
    }

    @Override
    public int getItemCount() {
        return seasons.size();
    }

    class seasonSheetViewHolder extends RecyclerView.ViewHolder {
        private final Button seasonSheetBtn;

        public seasonSheetViewHolder(@NonNull View itemView, SeasonSheetBtnsAdapter seasonSheetBtnsAdapter) {
            super(itemView);
            seasonSheetBtn = itemView.findViewById(R.id.seasonSheetBtn);
        }

        public void bind(final Season season) {
            seasonSheetBtn.setText(season.getName());
            seasonSheetBtn.setOnClickListener( view -> {
                if (onSeasonBtnClickListener != null) {
                    onSeasonBtnClickListener.onSeasonBtnClicked(season);
                }
            });
        }
    }
}
