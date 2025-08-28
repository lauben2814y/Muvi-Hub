package muvi.anime.hub.adapters.tv;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import muvi.anime.hub.R;
import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.data.FilterData;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.movie.MovieGenre;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.data.tv.SupabaseTvSection;
import muvi.anime.hub.interfaces.OnGenreClickedListener;
import muvi.anime.hub.interfaces.OnTvPosterClicked;
import muvi.anime.hub.pages.TvMore;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DbSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnGenreClickedListener {
    private final List<SupabaseTvSection> supabaseTvSections;
    private final Context context;
    private boolean isLoading = false;
    private final OnTvPosterClicked onTvPosterClicked;
    private FilterData filterData;
    private String order;
    private int page = 2;
    private final Map<Integer, List<MovieGenre>> sectionGenres = new HashMap<>();

    private static final int VIEW_TYPE_PRELOADER = 0;
    private static final int VIEW_TYPE_SECTION = 1;

    public DbSectionAdapter(List<SupabaseTvSection> supabaseTvSections, Context context, OnTvPosterClicked onTvPosterClicked) {
        this.supabaseTvSections = supabaseTvSections;
        this.context = context;
        this.onTvPosterClicked = onTvPosterClicked;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_PRELOADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.loading_main_row_tv, parent, false);
            return new dbSectionPreloaderViewHolder(view);
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.supabase_tv_header, parent, false);
        return new dbSectionAdapterViewHolder(view, this);
    }

    private void initializeOrder(String header) {
        switch (header) {
            case "Recently Uploaded":
                order = "created_at.desc";
                break;
            case "Latest on Muvi":
                order = "first_air_date.desc";
                break;
            case "Popular":
                order = "popularity.desc,first_air_date.desc";
                break;
            case "Top Rated":
                order = "vote_count.desc,vote_average.desc";
                break;
            default:
                order = null;
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return supabaseTvSections.get(position).isPlaceHolder() ? VIEW_TYPE_PRELOADER : VIEW_TYPE_SECTION;
    }

    public void addPreloader() {
        supabaseTvSections.add(new SupabaseTvSection(true));
        notifyItemInserted(supabaseTvSections.size() - 1);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof dbSectionPreloaderViewHolder) {

        } else if (holder instanceof dbSectionAdapterViewHolder) {
            SupabaseTvSection supabaseTvSection = supabaseTvSections.get(position);
            filterData = new FilterData(supabaseTvSection.getHeader(), 2, null, null, null);
            ((dbSectionAdapterViewHolder) holder).bind(position, supabaseTvSection, onTvPosterClicked);
        }
    }

    @Override
    public int getItemCount() {
        return supabaseTvSections.size();
    }

    public void addSupabaseTvSection(SupabaseTvSection supabaseTvSection) {
        for (int i = 0; i < supabaseTvSections.size(); i++) {
            if (supabaseTvSections.get(i).isPlaceHolder()) {
                supabaseTvSections.set(i, supabaseTvSection);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public String getGenreQuery(String genreNames) {
        return genreNames != null ? "cs.{" + genreNames + "}" : null;
    }

    @Override
    public void onGenreClicked(List<MovieGenre> selectedGenres, int sectionPosition) {
        SupabaseTvSection section = supabaseTvSections.get(sectionPosition);

        String genreNames = selectedGenres.stream()
                .map(MovieGenre::getName)
                .collect(Collectors.joining(","));

        Log.d(Utils.getTag(), "Selected genres " + genreNames + " for " + Utils.getTvOrder(section.getHeader()));

        List<SupabaseTv> initialTvs = section.getSupabaseTvs();
        initialTvs.clear();
        notifyDataSetChanged();

        // show loading
        initialTvs.add(new SupabaseTv(true));
        notifyItemInserted(initialTvs.size() - 1);

        SecureService tvService = SecureClient.getApi(context);
        Call<List<SupabaseTv>> call = tvService.getFilteredTvs(
                1,
                20,
                Utils.getTvFields(),
                Utils.getTvOrder(section.getHeader()),
                getGenreQuery(genreNames),
                null,
                null
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseTv>> call, @NonNull Response<List<SupabaseTv>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    List<SupabaseTv> newTvs = response.body();

                    // hide loading
                    initialTvs.remove(initialTvs.size() - 1);
                    notifyItemRemoved(initialTvs.size());

                    new Handler(Looper.getMainLooper()).post(() -> {
                        section.setSupabaseTvs(newTvs);
                        notifyItemChanged(sectionPosition);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseTv>> call, @NonNull Throwable throwable) {

            }
        });
    }

    static class dbSectionPreloaderViewHolder extends RecyclerView.ViewHolder {

        public dbSectionPreloaderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public List<MovieGenre> getTvGenres(Context context) {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.tv_genres);
            InputStreamReader reader = new InputStreamReader(inputStream);

            Gson gson = new Gson();
            Type typeList = new TypeToken<List<MovieGenre>>() {
            }.getType();
            return gson.fromJson(reader, typeList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public class dbSectionAdapterViewHolder extends RecyclerView.ViewHolder {
        TextView sectionHeader;
        RecyclerView innerRecycleView, genreRecyclerView;
        ImageButton viewMoreBtn;

        public dbSectionAdapterViewHolder(@NonNull View itemView, DbSectionAdapter dbSectionAdapter) {
            super(itemView);
            sectionHeader = itemView.findViewById(R.id.dbTvSectionHeader);
            viewMoreBtn = itemView.findViewById(R.id.dbTvViewMoreBtn);
            innerRecycleView = itemView.findViewById(R.id.dbTvInnerRecyclerView);
            genreRecyclerView = itemView.findViewById(R.id.genreRecycler);

            viewMoreBtn.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    dbSectionAdapter.viewMoreBtnListener(position);
                }
            });
        }

        public void bind(int position, SupabaseTvSection supabaseTvSection, OnTvPosterClicked onTvPosterClicked) {
            sectionHeader.setText(supabaseTvSection.getHeader());

            // Set up inner recycler
            DbSectionItemsAdapter dbSectionItemsAdapter = new DbSectionItemsAdapter(
                    supabaseTvSection.getSupabaseTvs(),
                    context,
                    onTvPosterClicked);

            innerRecycleView.setLayoutManager(
                    new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            innerRecycleView.setAdapter(dbSectionItemsAdapter);

            innerRecycleView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                    if (layoutManager != null) {
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                        if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                            isLoading = true;
                            // show loading indicator
                            dbSectionItemsAdapter.showLoading();
                            fetchMoreTvs(dbSectionItemsAdapter, supabaseTvSection);
                        }
                    }
                }
            });

            // Get or create genre list for thi section
            List<MovieGenre> genres = sectionGenres.computeIfAbsent(position, k -> {
                List<MovieGenre> newGenres = getTvGenres(context);
                // Initialize all genres as unselected
                newGenres.forEach(genre -> genre.setSelected(false));
                return newGenres;
            });

            // Set up genres recyclerview
//            GenreAdapter genreAdapter = new GenreAdapter(
//                    context,
//                    genres,
//                    DbSectionAdapter.this,
//                    position
//            );
//            genreRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
//            genreRecyclerView.setAdapter(genreAdapter);
        }
    }

    private void fetchMoreTvs(DbSectionItemsAdapter dbSectionItemsAdapter, SupabaseTvSection supabaseTvSection) {
        initializeOrder(supabaseTvSection.getHeader());
        filterData.setOrderBy(supabaseTvSection.getHeader());

        SecureService tvService = SecureClient.getApi(context);
        Call<List<SupabaseTv>> call = tvService.getFilteredTvs(
                page,
                20,
                Utils.getTvFields(),
                order,
                null,
                null,
                null
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<SupabaseTv>> call, Response<List<SupabaseTv>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(Utils.getTag(), "Title " + supabaseTvSection.getHeader());
                    Log.d(Utils.getTag(), "Response got for " + order);

                    List<SupabaseTv> newTvs = response.body();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        new Handler().postDelayed(() -> {
                            dbSectionItemsAdapter.hideLoading();
                            dbSectionItemsAdapter.addSupabaseTvs(newTvs);
                            page = page + 1;
                            isLoading = false;
                        }, 2000);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseTv>> call, @NonNull Throwable throwable) {

            }
        });
    }

    private void viewMoreBtnListener(int position) {
        if (position != RecyclerView.NO_POSITION) {
            SupabaseTvSection supabaseTvSection = supabaseTvSections.get(position);

            Intent intent = new Intent(context, TvMore.class);
            intent.putExtra("sectiondata", supabaseTvSection);
            context.startActivity(intent);
        }
    }
}
