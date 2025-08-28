package muvi.anime.hub.tabs.movie;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.adapters.movie.CollectionAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class related extends Fragment {
    private static final String ARG_DATA = "supabasemovie";
    private RecyclerView relatedRecycler;
    private Context context;
    private CollectionAdapter collectionAdapter;

    public related() {
        // Required empty public constructor
    }

    public static related newInstance(SupabaseMovie supabaseMovie) {
        related fragment = new related();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATA, supabaseMovie);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof CollectionAdapter.onCollectionPosterClickedListener)) {
            throw new ClassCastException(context.toString() + " must implement OnEpisodeClickListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_related, container, false);
        relatedRecycler = view.findViewById(R.id.movieRelatedRecycler);
        context = requireContext();
        SupabaseMovie supabaseMovie = null;

        if (getArguments() != null) {
            supabaseMovie = (SupabaseMovie) getArguments().getSerializable(ARG_DATA);
        }

        if (supabaseMovie != null) {
            SecureService movieService = SecureClient.getApi(context);
            Call<List<SupabaseMovie>> call = movieService.getRelatedMovies(
                    1,
                    20,
                    Utils.getMovieFields(),
                    getGenreQuery(supabaseMovie.getGenres()),
                    "release_date.desc"
            );
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<SupabaseMovie>> call, @NonNull Response<List<SupabaseMovie>> response) {
                    if (response.isSuccessful() && response.body() != null) {

                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                List<SupabaseMovie> supabaseRelatedMovies = response.body();

                                collectionAdapter = new CollectionAdapter(supabaseRelatedMovies, context, (CollectionAdapter.onCollectionPosterClickedListener) getActivity(), requireActivity());

                                relatedRecycler.setLayoutManager(new LinearLayoutManager(context));

                                relatedRecycler.setAdapter(collectionAdapter);

                            });
                        }
                    } else {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                System.err.println("Error: " + response.code());
                            });
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<SupabaseMovie>> call, @NonNull Throwable throwable) {
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            System.err.println("Error: " + throwable.getMessage());
                        });
                    }
                }
            });
        }

        return view;
    }

    public String getGenreQuery(List<String> genres) {
        return genres != null ? String.join(",", genres) : null;
    }

    public String getPageQuery() {
        int from = 0;
        int to = from + 10 - 1;

        return from + "-" + to;
    }
}