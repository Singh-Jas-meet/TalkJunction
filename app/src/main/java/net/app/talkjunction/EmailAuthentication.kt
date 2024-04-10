package net.app.talkjunction

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

// This class handles email/password authentication.
class EmailAuthentication(private val activity: AppCompatActivity) {

    /**
     * Function to sign up a user with email and password.
     * @param email The email address of the user.
     * @param password The password of the user.
     */
    fun signUpUser(email: String, password: String) {
        // Check if email and password are not empty
        if (email.isNotEmpty() && password.isNotEmpty()) {
            // Create user with email and password using Firebase Auth
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // If sign-up is successful, navigate to ChooseDiscussionActivity
                        NavigationUtils.startNewActivity(
                            activity,
                            ChooseDiscussionActivity::class.java
                        )
                        // Show a toast indicating sign-up success
                        Toast.makeText(activity, "Sign Up Successful", Toast.LENGTH_SHORT).show()
                    } else {
                        // If sign-up fails, show an error message
                        Toast.makeText(activity, "Sign Up Failed", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // If email or password is empty, show an error message
            Toast.makeText(activity, "Email and password must not be empty", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Function to log in a user with email and password.
     * @param email The email address of the user.
     * @param password The password of the user.
     */
    fun logInUser(email: String, password: String) {
        // Check if email and password are not empty
        if (email.isNotEmpty() && password.isNotEmpty()) {
            // Sign in user with email and password using Firebase Auth
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // If login is successful, navigate to ChooseDiscussionActivity
                        NavigationUtils.startNewActivity(
                            activity,
                            ChooseDiscussionActivity::class.java
                        )
                    } else {
                        // If login fails, show an error message
                        Toast.makeText(activity, "Login Failed: Please check credentials", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // If email or password is empty, show an error message
            Toast.makeText(activity, "Email and password must not be empty", Toast.LENGTH_SHORT).show()
        }
    }

}
