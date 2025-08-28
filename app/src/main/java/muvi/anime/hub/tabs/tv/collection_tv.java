package muvi.anime.hub.tabs.tv;

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
import muvi.anime.hub.adapters.tv.CollectionAdapterTv;
import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.tv.SupabaseTv;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class collection_tv extends Fragment {
    private static final String ARG_DATA = "supabasetv";
    private RecyclerView relatedRecycler;
    private Context context;
    private CollectionAdapterTv collectionAdapterTv;

    public collection_tv() {
        // Required empty public constructor
    }

    public static collection_tv newInstance(SupabaseTv supabaseTv) {
        collection_tv fragment = new collection_tv();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATA, supabaseTv);
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
        if (!(context instanceof CollectionAdapterTv.onCollectionTvPosterClickedListener)) {
            throw new ClassCastException(context.toString() + " must implement OnEpisodeClickListener");
        }
    }

    public String getGenreQuery(List<String> genres) {
        return genres != null ? String.join(",", genres) : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_related_tv, container, false);
        relatedRecycler = view.findViewById(R.id.tvRelatedRecycler);
        context = requireContext();
        SupabaseTv supabaseTv = null;

        if (getArguments() != null) {
            supabaseTv = (SupabaseTv) getArguments().getSerializable(ARG_DATA);
        }

        if (supabaseTv != null) {
            SecureService tvService = SecureClient.getApi(context);

            tvService.getRelatedTvs(
                    1,
                    20,
                    Utils.getTvFields(),
                    getGenreQuery(supabaseTv.getGenres()),
                    "first_air_date.desc"
            ).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<SupabaseTv>> call, @NonNull Response<List<SupabaseTv>> response) {
                    if (response.isSuccessful() && response.body() != null) {

                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                List<SupabaseTv> relatedTvs = response.body();

                                collectionAdapterTv = new CollectionAdapterTv(relatedTvs, context, (CollectionAdapterTv.onCollectionTvPosterClickedListener) getActivity(), requireActivity());

                                relatedRecycler.setLayoutManager(new LinearLayoutManager(context));

                                relatedRecycler.setAdapter(collectionAdapterTv);
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
                public void onFailure(@NonNull Call<List<SupabaseTv>> call, @NonNull Throwable throwable) {
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
}