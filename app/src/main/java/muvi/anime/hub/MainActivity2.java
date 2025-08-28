package muvi.anime.hub;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jackandphantom.carouselrecyclerview.CarouselLayoutManager;
import com.jackandphantom.carouselrecyclerview.CarouselRecyclerview;

import java.util.List;

import muvi.anime.hub.adapters.DummyCarouselAdapter;
import muvi.anime.hub.adapters.DummyListAdapter;
import muvi.anime.hub.data.DummyMedia;
import muvi.anime.hub.managers.AdManagerCoordinator;
import muvi.anime.hub.pages.DummyDetails;
import muvi.anime.hub.ui.UI;

public class MainActivity2 extends AppCompatActivity {
    private CarouselRecyclerview carouselRecyclerview;
    private final Context context = this;
    private DummyCarouselAdapter dummyCarouselAdapter;
    private DummyListAdapter dummyListAdapter;
    private UI ui;
    private RecyclerView dummyRecycler;
    private Button previewBtn;

    private TextView dummy_carousel_poster_title, dummy_carousel_poster_genres;

    private DummyMedia currentDummyMedia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main2), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        previewBtn = findViewById(R.id.dummyBtn);
        dummy_carousel_poster_title = findViewById(R.id.dummy_carousel_poster_title);
        dummy_carousel_poster_genres = findViewById(R.id.dummy_carousel_poster_genres);

        previewBtn.setOnClickListener(view -> {
            Intent intent = new Intent(context, DummyDetails.class);
            intent.putExtra("dummymedia", currentDummyMedia);
            this.startActivity(intent);
        });

        AdManagerCoordinator adManagerCoordinator = new AdManagerCoordinator(this);
        adManagerCoordinator.initialize(this);

        ui = new UI();
        carouselRecyclerview = findViewById(R.id.moviesCarouselDummy);
        dummyRecycler = findViewById(R.id.dummyRecycler);

        List<DummyMedia> dummyMedia = ui.getDummyMedia(context);
        currentDummyMedia = dummyMedia.get(0);
        dummy_carousel_poster_title.setText(currentDummyMedia.getTitle());
        dummy_carousel_poster_genres.setText(currentDummyMedia.getGenres());

        dummyCarouselAdapter = new DummyCarouselAdapter(context, dummyMedia, this);
        carouselRecyclerview.setAdapter(dummyCarouselAdapter);
        carouselRecyclerview.setInfinite(true);

        carouselRecyclerview.setItemSelectListener(new CarouselLayoutManager.OnSelected() {
            @Override
            public void onItemSelected(int i) {
                updateCarouselDetails(i);
            }

            private void updateCarouselDetails(int i) {
                currentDummyMedia = dummyMedia.get(i);
                dummy_carousel_poster_title.setText(currentDummyMedia.getTitle());
                dummy_carousel_poster_genres.setText(currentDummyMedia.getGenres());
            }
        });

        dummyListAdapter = new DummyListAdapter(context, dummyMedia, this);
        dummyRecycler.setLayoutManager(new LinearLayoutManager(context));
        dummyRecycler.setAdapter(dummyListAdapter);
    }
}