package muvi.anime.hub.pages;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import muvi.anime.hub.BuildConfig;
import muvi.anime.hub.R;
import muvi.anime.hub.data.DummyMedia;

public class DummyDetails extends AppCompatActivity {
    private DummyMedia dummyMedia;
    private ImageView poster, backdrop;
    private TextView title, watchOn, overview;
    private Button bookBtn, learnMore;
    private final Context context = this;

    private View likeBtn, shareBtn, reportBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dummy_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dummy_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        dummyMedia = (DummyMedia) getIntent().getSerializableExtra("dummymedia");

        setUpViews();

        if (dummyMedia != null) {

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            // Load image

            Glide.with(poster)
                    .load(dummyMedia.getPoster())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(poster);

            Glide.with(backdrop)
                    .load(dummyMedia.getBackdrop_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(backdrop);

            title.setText(dummyMedia.getTitle());
            watchOn.setText(dummyMedia.getWatch_on());
            overview.setText(dummyMedia.getOverview());

            bookBtn.setOnClickListener(view -> bookedMovie());

            learnMore.setOnClickListener(view -> visitTelegram());

            likeBtn.setOnClickListener(view -> {
                Toast.makeText(context, "You liked this movie thanks for the feedback", Toast.LENGTH_SHORT).show();
            });

            shareBtn.setOnClickListener(view -> {
                share();
            });

            reportBtn.setOnClickListener(view -> {
                visitTelegram();
            });
        }
    }

    private void share() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out " + dummyMedia.getTitle() + " on Muvi Hub");

        // Show chooser dialog
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void visitTelegram() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://t.me/muvi_telegram_te"));
        intent.setPackage("org.telegram.messenger");

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Telegram is not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void bookedMovie() {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Booked 😂😂")
                .setMessage("Thanks for booking this movie u will be notified when the program starts on any channel")
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setUpViews() {
        poster = findViewById(R.id.dummy_poster);
        backdrop = findViewById(R.id.dummy_backdrop);
        title = findViewById(R.id.dummy_title);
        watchOn = findViewById(R.id.details_watch_on_txt);
        overview = findViewById(R.id.details_overview_txt);
        bookBtn = findViewById(R.id.dummy_book_btn);
        learnMore = findViewById(R.id.dummy_more_btn);

        likeBtn = findViewById(R.id.like_dummy_btn);
        shareBtn = findViewById(R.id.share_dummy_btn);
        reportBtn = findViewById(R.id.report_dummy_btn);
    }
}