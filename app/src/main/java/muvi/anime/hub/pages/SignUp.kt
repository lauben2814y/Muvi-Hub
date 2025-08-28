package muvi.anime.hub.pages

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import muvi.anime.hub.MainActivity
import muvi.anime.hub.MainActivity2
import muvi.anime.hub.R
import muvi.anime.hub.api.SecureClient
import muvi.anime.hub.api.SecureService
import muvi.anime.hub.api.kotlin.RetrofitClient
import muvi.anime.hub.data.Utils
import muvi.anime.hub.managers.SyncDialog
import muvi.anime.hub.managers.UserManager
import muvi.anime.hub.models.User
import muvi.anime.hub.storage.PreferenceHelper
import okio.IOException
import retrofit2.HttpException

class SignUp : AppCompatActivity() {
    private lateinit var credentialManager: CredentialManager
    private lateinit var auth: FirebaseAuth
    private val tag = "W&Q"
    private var initialStatus = false

    private lateinit var userManager: UserManager

    // UI elements
    private lateinit var userNameField: TextInputEditText
    private lateinit var emailField: TextInputEditText
    private lateinit var passwordField: TextInputEditText
    private lateinit var signUpButton: MaterialButton
    private lateinit var signInButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // initialize signup sdk
        userManager = UserManager.getInstance(this)
        credentialManager = CredentialManager.create(this)
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        initializeUI()

        // Set up click listeners
        setUpClickListeners()
    }

    // Register for activity result
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            handleSignInResult(result.data)
        }
    }

    private fun handleSignInResult(data: Intent?) {

    }

    private fun validateFields(): Boolean {
        var isValid = true

        // Validate username
        val username = userNameField.text.toString().trim()
        if (username.isEmpty()) {
            userNameField.error = "Username is required"
            isValid = false
        } else if (username.length < 3) {
            userNameField.error = "Username must be at least 3 characters"
            isValid = false
        }

        // Validate email
        val email = emailField.text.toString().trim()
        if (email.isEmpty()) {
            emailField.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.error = "Please enter a valid email address"
            isValid = false
        }

        // Validate password
        val password = passwordField.text.toString()
        if (password.isEmpty()) {
            passwordField.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordField.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun updateUserProfile(user: FirebaseUser?, username: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build()

        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(tag, "User profile updated.")
                    updateUI(user)
                } else {
                    updateUI(user)
                }
            }
    }

    private fun showSignInDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Email Already Registered")
            .setMessage("This email is already registered. Would you like to continue with Google instead?")
            .setPositiveButton("Continue with Google") { dialog, _ ->
                dialog.dismiss()
                signIn()
            }
            .setNegativeButton("Try Another Email") { dialog, _ ->
                dialog.dismiss()
                emailField.requestFocus()
            }
            .show()
    }

    private fun handleSignUpError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthWeakPasswordException -> {
                // Password is too weak
                passwordField.error = "Password is too weak. ${exception.reason}"
            }

            is FirebaseAuthInvalidCredentialsException -> {
                // Invalid email format
                emailField.error = "Invalid email format"
            }

            is FirebaseAuthUserCollisionException -> {
                // Email already exists
                emailField.error = "This email is already registered"

                // Show dialog asking user if they want to sign in instead
                showSignInDialog()
            }

            else -> {
                // Generic error message for other cases
                Toast.makeText(
                    this,
                    "Sign up failed: ${exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Reset button state
        signUpButton.isEnabled = true
        signUpButton.text = "Sign Up"
    }

    private fun signUpWithEmailPassword() {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString()
        val username = userNameField.text.toString().trim()

        // Show loading state
        signUpButton.isEnabled = false
        signUpButton.text = "Creating Account..."

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            run {
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUserProfile(user, username)

                } else {
                    handleSignUpError(task.exception)
                }
            }
        }
    }

    private fun setUpClickListeners() {
        // Email / Password Sign In Button
        signUpButton.setOnClickListener {
            if (validateFields()) {
                signUpWithEmailPassword()
            }
        }

        // Go to login page
        signInButton.setOnClickListener {
            startActivity(Intent(this, LogIn::class.java))
        }

        findViewById<android.widget.Button>(R.id.google_sign_in_button).setOnClickListener {
            signIn()
        }
    }

    private fun initializeUI() {
        userNameField = findViewById(R.id.username_field)
        emailField = findViewById(R.id.email_field)
        passwordField = findViewById(R.id.password_field)
        signUpButton = findViewById(R.id.signup_button)
        signInButton = findViewById(R.id.login_button)
    }

    private fun signIn() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = getSignInCredential()
                handleCredential(result)
            } catch (e: GetCredentialCancellationException) {
                Log.d(tag, "Sign-in cancelled by user", e)
                Toast.makeText(
                    this@SignUp,
                    "Sign-in cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: GetCredentialException) {
                Log.e(tag, "Sign-in failed", e)

                // Enhanced error handling for Android 15+
                val errorMessage = when (e) {
                    is NoCredentialException -> "No Google accounts found"
                    is GetCredentialProviderConfigurationException -> "Error connecting to Google services"
                    is GetCredentialUnsupportedException -> {
                        // This is common on Android 15+ when credential manager is not properly configured
                        Log.e(tag, "Credential Manager not supported or misconfigured", e)
                        "Google Sign-In not available on this device"
                    }
                    else -> {
                        // Log additional details for debugging
                        Log.e(tag, "Detailed error: ${e.javaClass.simpleName} - ${e.message}", e)
                        "Sign-in failed: ${e.message}"
                    }
                }

                Toast.makeText(this@SignUp, errorMessage, Toast.LENGTH_LONG).show()

                // On Android 15+, if credential manager fails, you might want to fallback
                // to the legacy Google Sign-In method (though this is not recommended)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    Log.w(tag, "Consider implementing fallback for Android 15+")
                }
            } catch (e: Exception) {
                Log.e(tag, "Unexpected error during sign-in", e)
                Toast.makeText(
                    this@SignUp,
                    "Unexpected error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun getSignInCredential(): GetCredentialResponse {
        // Enhanced credential request for Android 15+ compatibility
        val googleIdOption = GetSignInWithGoogleOption.Builder(
            serverClientId = this.getString(R.string.default_web_client_id)
        ).apply {

        }.build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .apply {
                // Set origin for Android 15+ (important for security)
                // setOrigin("your_app_origin") // Usually your package name

                // Add preference for newer Android versions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    // Any Android 15+ specific configurations
                }
            }
            .build()

        return credentialManager.getCredential(
            context = this,
            request = request
        )
    }

    private suspend fun handleCredential(result: GetCredentialResponse) {
        // Handle the successfully returned credential
        val credential = result.credential

        Log.d(tag, "Received credential type: ${credential.javaClass.simpleName}")
        Log.d(tag, "Credential data: ${credential.data}")

        when (credential) {
            is GoogleIdTokenCredential -> {
                try {
                    val idToken = credential.idToken
                    Log.d(tag, "Received Google ID token")

                    // Validate token before using it (important for Android 15+)
                    if (idToken.isBlank()) {
                        throw IllegalStateException("Received empty ID token")
                    }

                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                    // Add timeout for Android 15+ compatibility
                    val authResult = withTimeout(30000) { // 30 second timeout
                        auth.signInWithCredential(firebaseCredential).await()
                    }

                    // Sign in success
                    val user = authResult.user
                    Log.d(tag, "Firebase authentication successful for user: ${user?.email}")

                    withContext(Dispatchers.Main) {
                        updateUI(user)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(tag, "Firebase authentication timed out", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SignUp,
                            "Authentication timed out. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI(null)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Firebase authentication failed", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SignUp,
                            "Authentication failed: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI(null)
                    }
                }
            }
            is CustomCredential -> {
                // Handle CustomCredential - this is what's being returned on Android 15+
                Log.d(tag, "Received CustomCredential with type: ${credential.type}")

                when (credential.type) {
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                        try {
                            // Extract GoogleIdTokenCredential from CustomCredential
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val idToken = googleIdTokenCredential.idToken
                            Log.d(tag, "Extracted Google ID token from CustomCredential")

                            if (idToken.isBlank()) {
                                throw IllegalStateException("Received empty ID token from CustomCredential")
                            }

                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                            val authResult = withTimeout(30000) {
                                auth.signInWithCredential(firebaseCredential).await()
                            }

                            val user = authResult.user
                            Log.d(tag, "Firebase authentication successful for user: ${user?.email}")

                            withContext(Dispatchers.Main) {
                                updateUI(user)
                            }
                        } catch (e: TimeoutCancellationException) {
                            Log.e(tag, "Firebase authentication timed out", e)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@SignUp,
                                    "Authentication timed out. Please try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                updateUI(null)
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to process CustomCredential", e)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@SignUp,
                                    "Authentication failed: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                updateUI(null)
                            }
                        }
                    }
                    else -> {
                        Log.w(tag, "Received CustomCredential with unknown type: ${credential.type}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@SignUp,
                                "Received unknown credential type: ${credential.type}",
                                Toast.LENGTH_SHORT
                            ).show()
                            updateUI(null)
                        }
                    }
                }
            }
            else -> {
                Log.w(tag, "Received unexpected credential type: ${credential.javaClass.simpleName}")
                Log.w(tag, "Available credential data keys: ${credential.data.keySet()}")

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SignUp,
                        "Received unexpected credential type: ${credential.javaClass.simpleName}",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }
            }
        }
    }

    private suspend fun getStatus(): Boolean {
        return try {
            RetrofitClient.apiService.checkStatus()
        } catch (e: HttpException) {
            Log.e("Retrofit", "HTTP error: ${e.message()}")
            false
        } catch (e: IOException) {
            Log.e("Retrofit", "Network error: ${e.message}")
            false
        }
    }

    private fun emailCheck(currentUser: FirebaseUser) {
        val email = currentUser.email?.trim()?.lowercase()

        if (email != null && email.endsWith("@gmail.com") && email != "fav2814z@gmail.com") {
            // Gmail user (but not the special one)
            navigateToMain(currentUser)
        } else {
            // All others: null email, non-Gmail, or fav2815@gmail.com
            Toast.makeText(
                this@SignUp,
                "Signed in successfully as: ${currentUser.displayName}",
                Toast.LENGTH_SHORT
            ).show()
            startActivity(Intent(this@SignUp, MainActivity2::class.java))
            finish()
        }
    }

    private fun navigateToMain(currentUser: FirebaseUser) {
        currentUser.let { user ->
            val syncDialog = SyncDialog(this)

            syncDialog.onRetryCallback = {
                navigateToMain(currentUser)
            }

            syncDialog.show()
            syncDialog.setDialogPositionPercentage(0.25f) // 25% from top

            userManager.getOrCreateUser(user, object : UserManager.UserCallback {
                override fun onSuccess(user: User) {
                    // Update to success
                    syncDialog.updateStatus(
                        SyncDialog.SyncStatus.SUCCESS,
                        "Welcome back, ${userManager.userEmail}"
                    )

                    // Wait a bit to show success, then dismiss and navigate
                    Handler(Looper.getMainLooper()).postDelayed({
                        syncDialog.dismiss()

                        // Navigate immediately after dismissing dialog
                        if (!isFinishing && !isDestroyed) {
                            startActivity(Intent(this@SignUp, MainActivity::class.java))
                            finish()
                        }
                    }, 2000)
                }

                override fun onError(error: String) {
                    Log.e(tag, "Error getting user: $error")
                    syncDialog.updateStatus(
                        SyncDialog.SyncStatus.ERROR,
                        "Sync failed: $error"
                    )
                }
            })
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            if (initialStatus) {
                // ready to go
                navigateToMain(user)
            } else {
                lifecycleScope.launch {
                    val status = getStatus()
                    Log.d(tag, "Status got: $status")

                    if (!status) {
                        Toast.makeText(
                            this@SignUp,
                            "Signed in successfully as: ${user.displayName}",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@SignUp, MainActivity2::class.java))
                        finish()
                    } else {
                        emailCheck(user)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }
    }
}