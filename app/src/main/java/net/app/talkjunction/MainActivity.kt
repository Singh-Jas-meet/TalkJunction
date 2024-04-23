package net.app.talkjunction

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import net.app.talkjunction.databinding.ActivityMainBinding

/** This activity represents the main screen of the application. */
class MainActivity : AppCompatActivity() {

    // Declare variables
    private lateinit var binding: ActivityMainBinding
    private lateinit var emailAuthentication: EmailAuthentication
    private lateinit var googleAuthentication: GoogleAuthentication
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.secondary_theme_color)

        // Inflate layout and set content view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize email/password authentication handler
        emailAuthentication = EmailAuthentication(this)

        // Set up sign up button click listener for email/password authentication
        binding.signupBtn.setOnClickListener {
            val email = binding.emailTextField.text.toString().trim()
            val password = binding.passwordTextField.text.toString().trim()
            emailAuthentication.signUpUser(email, password)
        }

        // Set up login button click listener for email/password authentication
        binding.loginBtn.setOnClickListener {
            val email = binding.emailTextField.text.toString().trim()
            val password = binding.passwordTextField.text.toString().trim()
            emailAuthentication.logInUser(email, password)
        }

        // Set up Google sign-in options
        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build()

        // Initialize Google sign-in client
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Google authentication handler
        googleAuthentication = GoogleAuthentication(firebaseAuth, googleSignInClient, this)

        // Create an ActivityResultLauncher to handle the result of Google sign-in
        signInLauncher = googleAuthentication.createSignInLauncher(this)

        // Set up click listener for Google sign-in button
        binding.googleLoginBtn.setOnClickListener {
            // Initiate Google sign-in process using the GoogleAuthentication handler
            googleAuthentication.signIn(signInLauncher)
        }
    }
}
