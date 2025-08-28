package muvi.anime.hub.managers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.models.User;
import muvi.anime.hub.models.UserRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserManager {
    private static UserManager instance;
    private final SecureService secureService;
    private User currentUser;
    private boolean isLoading = false;

    // Private constructor for singleton pattern
    private UserManager(Context context) {
        this.secureService = SecureClient.getApi(context);
    }

    // Get singleton instance
    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context);
        }
        return instance;
    }

    // Interface for handling user operations
    public interface UserCallback {
        void onSuccess(User user);

        void onError(String error);
    }

    // Main method to get or create user
    public void getOrCreateUser(FirebaseUser firebaseUser, UserCallback callback) {
        if (firebaseUser == null) {
            callback.onError("Firebase user is null");
            return;
        }

        // If we already have a current user and it matches, return it
        if (currentUser != null && currentUser.getUser_id().equals(firebaseUser.getUid())) {
            Log.d("Muvi-Hub", "User already exists ");
            callback.onSuccess(currentUser);
            return;
        }

        // Prevent multiple simultaneous requests
        if (isLoading) {
            callback.onError("User request already in progress");
            return;
        }

        isLoading = true;

        // Create user request from Firebase user
        UserRequest request = createUserRequestFromFirebase(firebaseUser);

        // Make API call
        Call<User> call = secureService.getInsertUser(request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    initializeUserLists(currentUser);
                    callback.onSuccess(currentUser);
                } else {
                    String error = "Failed to get user: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            error += " - " + response.errorBody().string();
                        } catch (Exception e) {
                            // Ignore error body parsing issues
                        }
                    }
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                isLoading = false;
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Create UserRequest from FirebaseUser
    private UserRequest createUserRequestFromFirebase(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        String userName = firebaseUser.getDisplayName();
        String userEmail = firebaseUser.getEmail();

        // Handle null values
        if (userName == null || userName.isEmpty()) {
            userName = userEmail != null ? userEmail.split("@")[0] : "User_" + userId.substring(0, 8);
        }

        if (userEmail == null) {
            userEmail = "";
        }

        return new UserRequest(userId, userName, userEmail);
    }

    // Initialize lists to prevent null pointer exceptions
    private void initializeUserLists(User user) {
        if (user.getLiked_shorts() == null) {
            user.setLiked_shorts(new ArrayList<>());
        }
        if (user.getFollowers() == null) {
            user.setFollowers(new ArrayList<>());
        }
        if (user.getFavorites() == null) {
            user.setFavorites(new ArrayList<>());
        }
        if (user.getFollowing() == null) {
            user.setFollowing(new ArrayList<>());
        }
        if (user.getProfile_url() == null) {
            user.setProfile_url("");
        }
    }

    public String getUserEmail() {
        return currentUser != null ? currentUser.getUser_email() : "";
    }

    public void updateCurrentUser(User updatedUser) {
        if (updatedUser != null) {
            this.currentUser = updatedUser;
            initializeUserLists(this.currentUser);
        }
    }

    public void updateCurrentUserProfile(String profileUrl) {
        if (currentUser != null) {
            currentUser.setProfile_url(profileUrl);
        }
    }

    public User getCurrentUser() {
        if (currentUser != null) {
            return currentUser;
        } else {
            return null;
        }
    }
}