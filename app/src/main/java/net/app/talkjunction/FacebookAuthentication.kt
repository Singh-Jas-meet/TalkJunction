package net.app.talkjunction

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

// This class handles Facebook authentication.
class FacebookAuthentication(private val activity: AppCompatActivity) {

    // CallbackManager to handle Facebook login callbacks
    private var callbackManager: CallbackManager = CallbackManager.Factory.create()

    init {
        // Initialize callback manager
        // Register callback for Facebook login
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    // Handle successful login
                    showToast("Login Successful")
                }

                override fun onCancel() {
                    // Handle login cancellation
                    showToast("Login Cancelled")
                }

                override fun onError(error: FacebookException) {
                    // Handle login error
                    showToast("Error Occurred")
                }
            })
    }

    // This function is called to handle the result of the Facebook login activity.
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    /* Helper method to show a toast during the signing-in process to indicate whether the process
    was successful or not with an appropriate message
    */
    private fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}
