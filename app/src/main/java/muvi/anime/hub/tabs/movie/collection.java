package muvi.anime.hub.tabs.movie;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.Objects;

import muvi.anime.hub.R;
import muvi.anime.hub.adapters.movie.CollectionAdapter;
import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.data.movie.TMDBMovieCollection;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class collection extends Fragment {
    private static final String ARG_DATA = "collection";
    private Context context;
    private CollectionAdapter collectionAdapter;
    private String TAG = "Muvi-Hub";

    public collection() {
        // Required empty public constructor
    }

    public static collection newInstance(TMDBMovieCollection tmdbMovieCollection) {
        collection fragment = new collection();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATA, tmdbMovieCollection);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        View view = inflater.inflate(R.layout.fragment_collection, container, false);
        context = requireContext();

        TMDBMovieCollection tmdbMovieCollection = null;
        RecyclerView collectionRecycler = view.findViewById(R.id.collectionRecycler);

        if (getArguments() != null) {
            tmdbMovieCollection = (TMDBMovieCollection) getArguments().getSerializable(ARG_DATA);
        }

        if (tmdbMovieCollection != null) {
            SecureService movieService = SecureClient.getApi(context);

            Call<List<SupabaseMovie>> call = movieService.getCollectionMovies(
                    String.valueOf(tmdbMovieCollection.getId()),
                    Utils.getMovieFields(),
                    "release_date.desc"
            );
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<SupabaseMovie>> call, @NonNull Response<List<SupabaseMovie>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                List<SupabaseMovie> supabaseMovieCollectionList = response.body();

                                Log.d(TAG, "Length: " + supabaseMovieCollectionList.size());

                                collectionAdapter = new CollectionAdapter(supabaseMovieCollectionList, context, (CollectionAdapter.onCollectionPosterClickedListener) getActivity(), requireActivity());

                                collectionRecycler.setLayoutManager(new LinearLayoutManager(context));

                                collectionRecycler.setAdapter(collectionAdapter);
                            });
                        }
                    } else {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                getActivity().runOnUiThread(() -> Log.e(TAG, "Response body is either empty or null"));
                            });
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<SupabaseMovie>> call, @NonNull Throwable throwable) {
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> Log.e(TAG, Objects.requireNonNull(throwable.getMessage())));
                    }
                }
            });
        }
        return view;
    }
}