package net.app.talkjunction

import android.app.Activity
import android.content.Intent
/**
 * This object provides utility functions for navigating between activities.
 */
object NavigationUtils {

    /**
     * Function to start a new activity.
     * @param activity The current activity from which the new activity will be started.
     * @param activityClass The class of the activity to start.
     */
    fun startNewActivity(activity: Activity, activityClass: Class<*>) {
        // Create an Intent to start the new activity.
        val intent = Intent(activity, activityClass)
        // Start the new activity.
        activity.startActivity(intent)
        activity.finish()
    }
}
