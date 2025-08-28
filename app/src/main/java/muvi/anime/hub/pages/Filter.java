package muvi.anime.hub.pages;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import muvi.anime.hub.R;

import muvi.anime.hub.data.FilterData;
import muvi.anime.hub.data.Utils;

public class Filter extends AppCompatActivity {
    private Button submitFilterBtn;
    private ChipGroup genreChipGroup;
    private ChipGroup vjChipGroup;
    private ChipGroup countryChipGroup;
    private String orderBy;
    private MaterialToolbar toolbar;
    private final String TAG = "Muvi-Hub";
    private FilterData initialFilterData;
    private TextInputEditText searchField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_filter);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });
        submitFilterBtn = findViewById(R.id.submit_btn);
        genreChipGroup = findViewById(R.id.genreChipGroup);
        vjChipGroup = findViewById(R.id.vjChipGroup);
        countryChipGroup = findViewById(R.id.countryChipGroup);
        toolbar = findViewById(R.id.moviesToolbar);
        searchField = findViewById(R.id.movieVjChipSearch);

        // Load previous filters
        populateVjChipGroup(Utils.getVjs());

        initialFilterData = (FilterData) getIntent().getSerializableExtra("initialData");

        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                filterVjChips(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }

            private void filterVjChips(String query) {
                List<String> filtered = new ArrayList<>();
                for (String chip : Utils.getVjs()) {
                    if (chip.toLowerCase().contains(query.toLowerCase())) {
                        filtered.add(chip);
                    }
                }
                populateVjChipGroup(filtered);
            }
        });

        if (initialFilterData != null) {
            if (initialFilterData.getGenres() != null) {
                selectGenreChips(genreChipGroup, initialFilterData.getGenres());
            }

            if (initialFilterData.getVjs() != null) {
                selectVjChips(vjChipGroup, initialFilterData.getVjs());
            }

            if (initialFilterData.getCountries() != null) {
                selectCountryChips(countryChipGroup, initialFilterData.getCountries());
            }
        }

        // get Genres first
        submitFilterBtn.setOnClickListener(view -> {
            // Prepare filter data
            List<String> genres = !getSelectedGenreChips().isEmpty() ? getSelectedGenreChips() : null;
            List<String> vjs = !getSelectedVjs().isEmpty() ? getSelectedVjs() : null;
            List<String> countries = !getSelectedCountries().isEmpty() ? getSelectedCountries() : null;
            FilterData filterData = new FilterData(orderBy, 1, genres, vjs, countries);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("resultFilterData", filterData);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        // Go back
        toolbar.setNavigationOnClickListener(view -> finish());

    }

    private void populateVjChipGroup(List<String> vjLabels) {
        vjChipGroup.removeAllViews();

        for (String label : vjLabels) {
            Chip chip = new Chip(this);
            chip.setChipDrawable(ChipDrawable.createFromAttributes(this,
                    null,
                    0,
                    com.google.android.material.R.style.Widget_Material3_Chip_Filter
            ));
            chip.setId(View.generateViewId());
            chip.setText(label);
            chip.setCheckable(true);
            chip.setClickable(true);
            vjChipGroup.addView(chip);
        }
    }

    // Get methods
    public List<String> getSelectedGenreChips() {
        List<String> selectedChipsList = new ArrayList<>();

        // Loop through the chips in the ChipGroup
        for (int i = 0; i < genreChipGroup.getChildCount(); i++) {
            View chipView = genreChipGroup.getChildAt(i);
            if (chipView instanceof Chip chip) {
                // check if chip is selected
                if (chip.isChecked()) {
                    selectedChipsList.add(String.valueOf(chip.getText()));
                }
            }
        }

        return selectedChipsList;
    }

    public List<String> getSelectedVjs() {
        List<String> selectedChipsList = new ArrayList<>();

        // Loop through the chips in the ChipGroup
        for (int i = 0; i < vjChipGroup.getChildCount(); i++) {
            View chipView = vjChipGroup.getChildAt(i);
            if (chipView instanceof Chip chip) {
                // check if chip is selected
                if (chip.isChecked()) {
                    selectedChipsList.add(String.valueOf(chip.getText()));
                }
            }
        }

        return selectedChipsList;
    }

    public List<String> getSelectedCountries() {
        List<String> selectedChipsList = new ArrayList<>();

        // Loop through the chips in the ChipGroup
        for (int i = 0; i < countryChipGroup.getChildCount(); i++) {
            View chipView = countryChipGroup.getChildAt(i);
            if (chipView instanceof Chip) {
                Chip chip = (Chip) chipView;
                // check if chip is selected
                if (chip.isChecked()) {
                    int resourceId = chip.getId();

                    String resourceName = countryChipGroup.getResources().getResourceEntryName(resourceId);

                    // Split the resource name by underscores
                    String[] parts = resourceName.split("_");
//                    String formattedId = resourceName.replace("_", ",");

                    // Add each part to the list
                    selectedChipsList.addAll(Arrays.asList(parts));
                }
            }
        }

        return selectedChipsList;
    }

    // Set methods
    public void selectGenreChips(ChipGroup genreChipGroup, List<String> genreChipList) {
        for (int i = 0; i < genreChipGroup.getChildCount(); i++) {
            View chipView = genreChipGroup.getChildAt(i);
            if (chipView instanceof Chip) {
                Chip chip = (Chip) chipView;

                String chipName = String.valueOf(chip.getText());

                if (genreChipList.contains(chipName)) {
                    chip.setChecked(true);
                }
            }
        }
    }

    public void selectVjChips(ChipGroup vjChipGroup, List<String> vjChipList) {
        for (int i = 0; i < vjChipGroup.getChildCount(); i++) {
            View chipView = vjChipGroup.getChildAt(i);
            if (chipView instanceof Chip) {
                Chip chip = (Chip) chipView;

                String chipName = String.valueOf(chip.getText());

                if (vjChipList.contains(chipName)) {
                    chip.setChecked(true);
                }
            }
        }
    }

    public void selectCountryChips(ChipGroup countryChipGroup, List<String> countryChipList) {
        for (int i = 0; i < countryChipGroup.getChildCount(); i++) {
            View chipView = countryChipGroup.getChildAt(i);

            if (chipView instanceof Chip) {
                Chip chip = (Chip) chipView;

                // Get the resource ID name (e.g., "KP_KR")
                String resourceName = countryChipGroup.getResources().getResourceEntryName(chip.getId());

                // Check if the resource name can be split into parts
                String[] chipParts = resourceName.split("_");

                // Check if all parts of the chip are in the selectedParts list
                boolean shouldSelect = true;
                for (String part : chipParts) {
                    if (!countryChipList.contains(part)) {
                        shouldSelect = false;
                        break;
                    }
                }

                // If all parts match, programmatically select the chip
                chip.setChecked(shouldSelect);
            }
        }
    }

}