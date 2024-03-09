package com.example.talkjunction

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


class GoogleAuthentication(
    private var firebaseAuth: FirebaseAuth,
    private var googleSignInClient: GoogleSignInClient,
    private var activity: AppCompatActivity
) {

    fun createSignInLauncher(activity: AppCompatActivity): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        firebaseAuthWithGoogle(account.idToken!!, activity)
                        Log.d("NAME", "${account.givenName} ${account.displayName}")
                    } catch (e: ApiException) {
                        Log.w("Test", "Google sign-in failed", e)
                        Toast.makeText(
                            activity,
                            "Google sign-in failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Test", "Error during sign-in: ${e.message}", e)
                Toast.makeText(
                    activity,
                    "Error during sign-in: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun signIn(signInLauncher: ActivityResultLauncher<Intent>) {

        try {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e("Sign In Status: ", "Error during sign-in: ${e.message}", e)
            Toast.makeText(activity, "Error during sign-in: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, activity: AppCompatActivity) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                try {
                    if (task.isSuccessful) {
                        Log.d(
                            "Firebase/Google Authentication Status:",
                            "signInWithCredential:success"
                        )
                        Toast.makeText(
                            activity, "Logged in",
                            Toast.LENGTH_SHORT
                        ).show()
                        NavigationUtils.startNewActivity(
                            activity,
                            ChooseDiscussionActivity::class.java
                        )
                    } else {
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
