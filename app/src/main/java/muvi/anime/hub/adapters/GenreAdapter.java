package muvi.anime.hub.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import muvi.anime.hub.R;
import muvi.anime.hub.data.movie.MovieGenre;
import muvi.anime.hub.interfaces.OnGenreClickedListener;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {
    private final List<MovieGenre> genreList;
    private final Context context;
    private final OnGenreClickedListener onGenreClickedListener;
    private int sectionPosition;
    private List<String> genreListQuery;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GenreAdapter(Context context, List<MovieGenre> genreList, OnGenreClickedListener onGenreClickedListener, int sectionPosition) {
        this.context = context;
        this.genreList = genreList;
        sortGenresBySelection(); // sort genres initially
        this.onGenreClickedListener = onGenreClickedListener;
        this.sectionPosition = sectionPosition;
    }

    private void sortGenresBySelection() {
        // Create a new sorted list
        List<MovieGenre> sortedList = new ArrayList<>(genreList);
        Collections.sort(sortedList, (g1, g2) -> {
            if (g1.isSelected() && !g2.isSelected()) return -1;
            if (!g1.isSelected() && g2.isSelected()) return 1;
            return g1.getName().compareTo(g2.getName());
        });

        // Update the list and notify changes safely
        mainHandler.post(() -> {
            genreList.clear();
            genreList.addAll(sortedList);
            notifyDataSetChanged();
        });
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.genre_list_item, parent, false);
        return new GenreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        MovieGenre genre = genreList.get(position);
        holder.chip.setText(genre.getName());

        // Set the chip's checked state from the genre object
        holder.chip.setOnCheckedChangeListener(null);
        holder.chip.setChecked(genre.isSelected());

        holder.chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            genre.setSelected(isChecked);

            // Post the sort and notification to the main thread
            mainHandler.post(() -> {
                sortGenresBySelection();

                List<MovieGenre> selectedGenres = genreList.stream()
                        .filter(MovieGenre::isSelected)
                        .collect(Collectors.toList());

                if (onGenreClickedListener != null) {
                    onGenreClickedListener.onGenreClicked(
                            selectedGenres,
                            sectionPosition);
                }
            });
        });

    }

    @Override
    public int getItemCount() {
        return genreList.size();
    }

    static class GenreViewHolder extends RecyclerView.ViewHolder {
        Chip chip;

        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            chip = itemView.findViewById(R.id.genre_chip);
            chip.setChecked(true);
        }
    }
}
