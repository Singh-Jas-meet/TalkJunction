package net.app.talkjunction

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity

import net.app.talkjunction.NavigationUtils.googleSignInClient
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import net.app.talkjunction.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Declare variables
    private lateinit var binding: ActivityMainBinding
    private lateinit var facebookAuthentication: FacebookAuthentication
    private lateinit var emailAuthentication: EmailAuthentication
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleAuthentication: GoogleAuthentication
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up Facebook SDK
        FacebookSdk.setAutoInitEnabled(true)

        // Inflate layout and set content view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Facebook login button click listener
        binding.facebookloginBtn.setOnClickListener {
            LoginManager.getInstance()
                .logInWithReadPermissions(this, listOf("email", "public_profile"))
        }

        // Initialize Facebook authentication handler
        facebookAuthentication = FacebookAuthentication(this)

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
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .build()

        // Initialize Firebase authentication
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Google sign-in client
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Google authentication handler
        googleAuthentication = GoogleAuthentication(firebaseAuth, googleSignInClient!!, this)

        // Create an ActivityResultLauncher to handle the result of Google sign-in
        signInLauncher = googleAuthentication.createSignInLauncher(this)

        // Set up click listener for Google sign-in button
        binding.googleLoginBtn.setOnClickListener {
            // Initiate Google sign-in process using the GoogleAuthentication handler
            googleAuthentication.signIn(signInLauncher)
        }
    }
}
