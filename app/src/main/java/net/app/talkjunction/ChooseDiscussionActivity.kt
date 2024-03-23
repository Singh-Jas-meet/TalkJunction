package net.app.talkjunction

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.google.android.material.divider.MaterialDivider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import net.app.talkjunction.databinding.ActivityChooseDiscussionBinding


class ChooseDiscussionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChooseDiscussionBinding
    private var firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChooseDiscussionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addTopicImgBtn.setOnClickListener {
            addCheckBoxAndDivider()
        }

        binding.logoutBtn.setOnClickListener {
            NavigationUtils.googleSignInClient?.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
        }

        binding.updateFirebaseDatabase.setOnClickListener {
            val topics: String = grabCheckedCheckBoxesText()
            if (topics.isNotEmpty()) {
                saveCheckedCheckboxToFirebase(topics, this)
            } else {
                Toast.makeText(this, "Please make a selection", Toast.LENGTH_SHORT).show()
            }
        }

        binding.connectBtn.setOnClickListener {
            NavigationUtils.startNewActivity(this, ChatPageActivity::class.java)
        }
    }

    private fun addCheckBoxAndDivider() {

        val text = binding.addTopicEditText.text.toString()
        val capitalizedText = text.substring(0, 1).uppercase() + text.substring(1)
        // Create CheckBox
        val checkBox = CheckBox(this)
        checkBox.text = capitalizedText
        checkBox.textSize = 20F
        checkBox.setTextColor(Color.BLACK)
        checkBox.setPadding(20)
        checkBox.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.topicsLinearLayout.addView(checkBox)

        // Create MaterialDivider
        val divider = MaterialDivider(this)
        divider.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        divider.dividerInsetEnd = 16
        divider.dividerInsetStart = 16

        binding.topicsLinearLayout.addView(divider)

        binding.addTopicEditText.setText("")
    }


    private fun grabCheckedCheckBoxesText(): String {
        var userChoicesCombined: String = ""
        for (i in 0 until binding.topicsLinearLayout.childCount) {
            val childView = binding.topicsLinearLayout.getChildAt(i)
            if (childView is CheckBox) {
                if (childView.isChecked) {
                    if (userChoicesCombined.isNotEmpty()) {
                        userChoicesCombined += ", "
                    }
                    userChoicesCombined += childView.text.toString()
                }
            }
        }
        return userChoicesCombined
    }


    private fun saveCheckedCheckboxToFirebase(topic: String, activity: AppCompatActivity) {
        // Get the current user from Firebase Authentication
        val user = FirebaseAuth.getInstance().currentUser

        // Ensure user is not null before proceeding
        user?.let { currentUser ->
            // Get a reference to the Firestore database
            val database = FirebaseFirestore.getInstance()

            // Define the data to be added
            val userData = hashMapOf(
                "Email" to currentUser.email.toString(), // Store user's email address
                "TopicsChosen" to topic // Store topics chosen as a list
            )

            // Add the data to the Firestore database under the "Users" collection
            database.collection("Users")
                .add(userData)
                .addOnSuccessListener { documentReference ->
                    // Data added successfully
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    Toast.makeText(
                        this,
                        "Interests saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { e ->
                    // Error adding data
                    Log.e(TAG, "Error adding document", e)
                    Toast.makeText(
                        this,
                        "Error saving interests",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: Log.e(TAG, "Current user is null")
    }

}