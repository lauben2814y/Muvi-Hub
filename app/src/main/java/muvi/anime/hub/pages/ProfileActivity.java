package muvi.anime.hub.pages;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.loader.content.CursorLoader;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;

import muvi.anime.hub.R;
import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.models.ProfileImageResponse;
import muvi.anime.hub.models.UpdateProfileRequest;
import muvi.anime.hub.models.User;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {
    private UserManager userManager;
    private Context context;
    private ImageButton uploadImageButton;
    private SecureService secureService;
    private Uri imageUri;
    private ImageView previewImageView;
    private static final String TAG = "MuviShorts";
    private String FOLDER_PATH;
    private static final String BUCKET_NAME = "shorts-profile";
    private static final String BASE_URL = "https://muvihub.iqube.sbs/";
    private Gson gson;
    private MaterialButton uploadProfileBtn;

    // Register activity launcher for modern image picking
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    uploadProfileBtn.setVisibility(View.VISIBLE);
                    // Use Glide to load the image instead of manually loading the bitmap
                    Glide.with(this)
                            .load(imageUri)
                            .into(previewImageView);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        setUpViews();

        context = this;
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        secureService = SecureClient.getApi(this);
        userManager = UserManager.getInstance(this);
        gson = new GsonBuilder().setPrettyPrinting().create();

        userManager.getOrCreateUser(firebaseUser, new UserManager.UserCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(User user) {
                FOLDER_PATH = user.getUser_id() + "/";

                uploadImageButton.setOnClickListener(btn -> openImagePicker());

                if (user.getProfile_url() != null) {
                    Glide.with(ProfileActivity.this)
                            .load(user.getProfile_url())
                            .into(previewImageView);
                } else {
                    Glide.with(ProfileActivity.this)
                            .load("https://cdn.framework7.io/placeholder/cats-300x300-1.jpg")
                            .into(previewImageView);
                }

                uploadProfileBtn.setOnClickListener(btn -> {
                    if (imageUri != null) {
                        uploadProfileBtn.setText("Uploading...");
                        uploadProfileBtn.setEnabled(false);
                        uploadImageToSupabase(imageUri, user);
                    } else {
                        Toast.makeText(ProfileActivity.this, "Please select an image first", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {

            }
        });
    }

    // Helper method to get real path from URI
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        assert cursor != null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    // Generate a public URL for the uploaded file
    private String getPublicUrl(String path) {
        return BASE_URL + "storage/v1/object/public/" + BUCKET_NAME + "/" + path;
    }

    @SuppressLint("SetTextI18n")
    private void uploadImageToSupabase(Uri imageUri, User currentUser) {
        try {
            String filePath = getRealPathFromURI(imageUri);
            File imageFile = new File(filePath);

            if (!imageFile.exists()) {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = "image_" + System.currentTimeMillis() + ".jpg";
            String path = FOLDER_PATH + fileName;

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);

            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestFile);

            Call<ProfileImageResponse> profileCall = secureService.uploadFile(
                    BUCKET_NAME,
                    path,
                    body
            );

            profileCall.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<ProfileImageResponse> call, @NonNull Response<ProfileImageResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, gson.toJson(response.body()));
                        String publicUrl = getPublicUrl(path);

                        Call<User> userCall = secureService.updateUserProfile(new UpdateProfileRequest(
                                currentUser.getUser_id(),
                                publicUrl
                        ));
                        userCall.enqueue(new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    uploadProfileBtn.setText("Uploaded");
                                    uploadProfileBtn.setVisibility(View.GONE);
                                    userManager.updateCurrentUserProfile(publicUrl);
                                    Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<User> call, @NonNull Throwable throwable) {

                            }
                        });
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ?
                                    response.errorBody().string() : "No error body";
                            uploadProfileBtn.setText("Upload");
                            Log.e(TAG, "Upload failed: " + response.code() +
                                    " Error: " + errorBody);
                            Toast.makeText(context, "Upload failed: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ProfileImageResponse> call, @NonNull Throwable throwable) {
                    uploadProfileBtn.setText("Upload");
                    Log.e(TAG, "Upload exception", throwable);
                    Toast.makeText(context, "Upload failed: " + throwable.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("MuviShorts", "Error in upload method", e);
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Image"));
    }

    private void setUpViews() {
        // Set up views and click listeners here
        uploadImageButton = findViewById(R.id.add_profile_btn);
        previewImageView = findViewById(R.id.profile_image);
        uploadProfileBtn = findViewById(R.id.upload_profile_btn);
    }
}