package muvi.anime.hub.tabs.movie;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import muvi.anime.hub.R;
import muvi.anime.hub.adapters.TrailerAdapter;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.movie.TMDBMovieDetails;
import muvi.anime.hub.data.movie.TMDBMovieVideo;

public class overview extends Fragment {
    private static final String ARG_DATA  = "data";

    public overview() {
        // Required empty public constructor

    }
    public static overview newInstance(TMDBMovieDetails movieDetails) {
        overview fragment = new overview();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATA, movieDetails);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_overview, container, false);

        TMDBMovieDetails tmdbMovieDetails = null;
        ViewPager2 trailerPager = view.findViewById(R.id.trailerPager);
        TextView textView = view.findViewById(R.id.movieOverview);

        TrailerAdapter trailerAdapter = new TrailerAdapter(this);
        trailerPager.setAdapter(trailerAdapter);

        if (getArguments() != null) {
            tmdbMovieDetails = (TMDBMovieDetails) getArguments().getSerializable(ARG_DATA);
        }

        if (tmdbMovieDetails != null) {
            textView.setText(tmdbMovieDetails.getOverview());
            List<String> trailerIDs = new ArrayList<>();

            if (tmdbMovieDetails.getVideos() != null) {
                List<TMDBMovieVideo> movieVideos = tmdbMovieDetails.getVideos().getResults();
                for (TMDBMovieVideo video: movieVideos) {
                    if (Objects.equals(video.getType(), "Trailer")) {
                        trailerIDs.add(video.getKey());
                    }
                }
            }

            trailerAdapter.setTrailers(trailerIDs);

        }
        return view;
    }
}