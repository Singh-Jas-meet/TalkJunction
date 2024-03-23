package net.app.talkjunction

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInClient

// This object provides utility functions for navigating between activities.
object NavigationUtils {

    var googleSignInClient: GoogleSignInClient? = null

    // Function to start a new activity.
    // Parameters:
    // - activity: The current activity from which the new activity will be started.
    // - activityClass: The class of the activity to start.
    fun startNewActivity(activity: Activity, activityClass: Class<*>) {
        // Create an Intent to start the new activity.
        val intent = Intent(activity, activityClass)
        // Start the new activity.
        activity.startActivity(intent)
    }
}
