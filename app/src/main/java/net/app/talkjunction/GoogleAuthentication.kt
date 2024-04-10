package net.app.talkjunction

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

/**
 * Helper class for Google authentication.
 * Manages sign-in process using Google Sign-In API and Firebase Authentication.
 * @property firebaseAuth Firebase authentication instance.
 * @property googleSignInClient Google Sign-In client instance.
 * @property activity Reference to the calling activity.
 */
class GoogleAuthentication(
    private var firebaseAuth: FirebaseAuth,
    private var googleSignInClient: GoogleSignInClient,
    private var activity: AppCompatActivity
) {

    /**
     * Creates a launcher for starting the sign-in activity.
     * @param activity The activity registering the launcher.
     * @return ActivityResultLauncher<Intent> The launcher for starting the sign-in activity.
     */
    fun createSignInLauncher(activity: AppCompatActivity): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                if (result.resultCode == Activity.RESULT_OK) {
                    // Retrieve signed-in account information from the result intent
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        // Authenticate with Firebase using Google credentials
                        firebaseAuthWithGoogle(account.idToken!!, activity)
                    } catch (e: ApiException) {
                        // Handle Google sign-in failure
                        Log.w("Test", "Google sign-in failed", e)
                        Toast.makeText(
                            activity,
                            "Google sign-in failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                // Handle sign-in errors
                Log.e("Test", "Error during sign-in: ${e.message}", e)
                Toast.makeText(
                    activity,
                    "Error during sign-in: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Initiates the sign-in process by launching the sign-in activity.
     * @param signInLauncher The launcher for starting the sign-in activity.
     */
    fun signIn(signInLauncher: ActivityResultLauncher<Intent>) {
        try {
            // Start the sign-in activity using the launcher
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            // Handle sign-in errors
            Log.e("Sign In Status: ", "Error during sign-in: ${e.message}", e)
            Toast.makeText(activity, "Error during sign-in: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Authenticates with Firebase using Google credentials.
     * @param idToken The Google ID token obtained during sign-in.
     * @param activity The activity initiating the authentication process.
     */
    private fun firebaseAuthWithGoogle(idToken: String, activity: AppCompatActivity) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                try {
                    if (task.isSuccessful) {
                        // Start the main activity after successful authentication
                        NavigationUtils.startNewActivity(
                            activity,
                            ChooseDiscussionActivity::class.java
                        )
                    } else {
                        // Firebase authentication failed
                        Log.d(
                            "Firebase/Google Authentication Status",
                            "signInWithCredential:failure - ${task.exception?.message}"
                        )
                        Toast.makeText(
                            activity, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    // Handle authentication errors
                    Log.e(
                        "Firebase/Google Authentication error: ",
                        "Authentication error: ${e.message}",
                        e
                    )
                    Toast.makeText(
                        activity, "Authentication error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

}
