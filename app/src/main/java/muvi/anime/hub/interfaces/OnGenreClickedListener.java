package muvi.anime.hub.interfaces;

import java.util.List;

import muvi.anime.hub.data.movie.MovieGenre;

public interface OnGenreClickedListener {
    void onGenreClicked(List<MovieGenre> selectedGenres, int sectionPosition);
}
