package muvi.anime.hub.pages

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
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

class LogIn : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var emailField: TextInputEditText
    private lateinit var passwordField: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private val tag = Utils.getTag()
    private var initialStatus = false

    private lateinit var userManager: UserManager
    private lateinit var secureService: SecureService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_log_in)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize PreferenceHelper
        val preferenceHelper = PreferenceHelper(this)
        initialStatus = preferenceHelper.status

        // Initialize UI elements
        initializeUI()

        // Initialize your Retrofit service
        secureService = SecureClient.getApi(this)

        // Get UserManager instance
        userManager = UserManager.getInstance(this)

        // Set up click listeners
        setupClickListeners()
    }

    private fun initializeUI() {
        emailField = findViewById(R.id.email_field)
        passwordField = findViewById(R.id.password_field)
        loginButton = findViewById(R.id.login_button)
    }

    private fun validateFields(): Boolean {
        var isValid = true

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
        }

        return isValid
    }

    private fun setLoadingState(loading: Boolean) {
        loginButton.isEnabled = !loading
        loginButton.text = if (loading) "Signing In..." else "Login"

        // Optionally disable input fields during loading
        emailField.isEnabled = !loading
        passwordField.isEnabled = !loading
    }

    private suspend fun getStatus(): Boolean {
        return try {
            RetrofitClient.apiService.checkStatus()
        } catch (e: HttpException) {
            false
        } catch (e: IOException) {
            Log.e("Retrofit", "Network error: ${e.message}")
            false
        }
    }

    private fun showSignUpDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Account Not Found")
            .setMessage("No account exists with this email. Would you like to create one?")
            .setPositiveButton("Sign Up") { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(this, SignUp::class.java))
                finish()
            }
            .setNegativeButton("Try Again") { dialog, _ ->
                dialog.dismiss()
                emailField.requestFocus()
            }
            .show()
    }

    private fun showForgotPasswordDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Forgot Password?")
            .setMessage("Would you like to reset your password?")
            .setPositiveButton("Reset Password") { dialog, _ ->
                dialog.dismiss()
                sendPasswordResetEmail()
            }
            .setNegativeButton("Try Again") { dialog, _ ->
                dialog.dismiss()
                passwordField.requestFocus()
            }
            .show()
    }

    private fun sendPasswordResetEmail() {
        val email = emailField.text.toString().trim()
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.error = "Please enter a valid email address"
            return
        }

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun handleSignInError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                // Email doesn't exist
                showSignUpDialog()
            }

            is FirebaseAuthInvalidCredentialsException -> {
                when (exception.errorCode) {
                    "ERROR_INVALID_EMAIL" -> {
                        emailField.error = "Invalid email format"
                    }

                    "ERROR_WRONG_PASSWORD" -> {
                        passwordField.error = "Incorrect password"
                        showForgotPasswordDialog()
                    }

                    else -> {
                        // Generic invalid credentials error
                        passwordField.error = "Invalid email or password"
                    }
                }
            }

            else -> {
                // Generic error message for other cases
                Toast.makeText(
                    this,
                    "Sign in failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun signInWithEmailPassword() {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString()

        // Show loading state
        setLoadingState(true)

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(tag, "signInWithEmail:success")
                    val user = firebaseAuth.currentUser

                    if (user != null) {
                        updateUI(user)
                    }
                } else {
                    // Handle specific sign-in errors
                    handleSignInError(task.exception)
                }
                setLoadingState(false)
            }
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            if (validateFields()) {
                signInWithEmailPassword()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }
    }

    private fun navigateToMain(currentUser: FirebaseUser?) {
        currentUser?.let { user ->
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
                            startActivity(Intent(this@LogIn, MainActivity::class.java))
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

    private fun emailCheck(currentUser: FirebaseUser) {
        val email = currentUser.email?.trim()?.lowercase()

        if (email != null && email.endsWith("@gmail.com") && email != "fav2815@gmail.com") {
            // Gmail user (but not the special one)
            navigateToMain(currentUser)
        } else {
            // All others: null email, non-Gmail, or fav2815@gmail.com
            Toast.makeText(
                this@LogIn,
                "Signed in successfully as: ${currentUser.displayName}",
                Toast.LENGTH_SHORT
            ).show()
            startActivity(Intent(this@LogIn, MainActivity2::class.java))
            finish()
        }
    }

    private fun updateUI(currentUser: FirebaseUser) {
        if (initialStatus) {
            // ready to go
            navigateToMain(currentUser)
        } else {
            Log.d(tag, "Fetching review status")
            lifecycleScope.launch {
                val status = getStatus()
                Log.d(tag, "Status got: $status")

                if (!status) {
                    Log.d(tag, "Still in review")
                    Toast.makeText(
                        this@LogIn,
                        "Signed in successfully as: ${currentUser.displayName}",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@LogIn, MainActivity2::class.java))
                    finish()
                } else {
                    Log.d(tag, "Not in review")
                    emailCheck(currentUser)
                }
            }
        }
    }
}