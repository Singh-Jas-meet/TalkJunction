package net.app.talkjunction

import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.app.talkjunction.databinding.ActivityChatPageBinding
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Activity responsible for managing the chat interface. */
class ChatPageActivity : AppCompatActivity() {

    // Binding for the activity layout.
    private lateinit var binding: ActivityChatPageBinding
    private lateinit var progressDialog: ProgressDialog

    // Coroutine scope for managing coroutines in this activity.
    private var coroutineScope = CoroutineScope(Dispatchers.Main)

    /** Lifecycle method called when the activity is created. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using view binding.
        binding = ActivityChatPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the status bar color.
        window.statusBarColor = ContextCompat.getColor(this, R.color.secondary_theme_color)
        progressDialog = ProgressDialog(this)

        // Initialize UI components.
        val messageBox = binding.messageEditText
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        // Set up coroutine scope.
        coroutineScope = CoroutineScope(Dispatchers.Main)

        // Start checking for pair ID existence periodically.
        checkPairIdPeriodically()

        // Listener for ending the chat session.
        binding.endChat.setOnClickListener {
            progressDialog.setMessage("Ending chat")
            progressDialog.show()
            if (currentUserUid != null) {
                // Retrieve the pair ID for the current user.
                getPairID(currentUserUid) { pairId ->
                    if (pairId.isNotEmpty()) {
                        // Update pairID and isChatting fields for both users in the pair.
                        deletePairAndUsers(pairId) { result ->
                            if (result) {
                                progressDialog.dismiss()
                                // Start ChooseDiscussionActivity if update is successful.
                                NavigationUtils.startNewActivity(
                                    this,
                                    ChooseDiscussionActivity::class.java
                                )
                            }
                        }
                    } else {
                        // Handle the case where pairID is not found.
                        Toast.makeText(this, "No active chat session to end", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

        // Listener for sending a message.
        binding.sendBtn.setOnClickListener {
            val message = messageBox.text.toString().trim()
            if (currentUserUid != null)
                getPairID(currentUserUid) { pairId ->
                    if (pairId.isNotEmpty()) {
                        // If pairID is available, send the message to the pair.
                        sendMessageToPair(pairId, message)
                        messageBox.text.clear()
                    } else {
                        // If pairID is not available, handle the case accordingly.
                        Log.e(TAG, "Pair ID not found for the current user")
                    }
                }
        }

        // Define a function to handle sending the message
        fun sendMessage() {
            val message = messageBox.text.toString().trim()
            if (currentUserUid != null)
                getPairID(currentUserUid) { pairId ->
                    if (pairId.isNotEmpty()) {
                        // If pairID is available, send the message to the pair.
                        sendMessageToPair(pairId, message)
                        messageBox.text.clear()

                        // Hide the keyboard after sending the message
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(messageBox.windowToken, 0)
                    } else {
                        // If pairID is not available, handle the case accordingly.
                        Log.e(TAG, "Pair ID not found for the current user")
                    }
                }
        }

        // Set click listener for sendBtn
        binding.sendBtn.setOnClickListener {
            // Call the sendMessage function when sendBtn is clicked
            sendMessage()
        }

        // Set onKeyListener for messageBox
        messageBox.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                // Call the sendMessage function when Enter key is pressed
                sendMessage()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

        // Start listening for incoming messages.
        if (currentUserUid != null) {
            getPairID(currentUserUid) { pairId ->
                if (pairId.isNotEmpty()) {
                    // If pairID is available, start listening for messages.
                    listenForMessages(pairId)
                } else {
                    // If pairID is not available, handle the case accordingly.
                    Log.e(TAG, "Pair ID not found for the current user")
                }
            }
        }
    }

    /** Lifecycle method called when the activity is destroyed. */
    override fun onDestroy() {
        super.onDestroy()
        // Show a toast message indicating that the user ended the chat
        Toast.makeText(this, "chat ended", Toast.LENGTH_SHORT).show()
        // Start ChooseDiscussionActivity when the activity is destroyed.
        NavigationUtils.startNewActivity(this, ChooseDiscussionActivity::class.java)
    }

    /**
     * Function to send a message to the pair identified by pairId.
     *
     * @param pairId The ID of the pair to send the message to.
     * @param message The message to send.
     */
    private fun sendMessageToPair(pairId: String, message: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val pairRef = FirebaseDatabase.getInstance().getReference("Pairs/$pairId")

        pairRef.addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val user1 = dataSnapshot.child("user1").getValue(String::class.java)
                    val user2 = dataSnapshot.child("user2").getValue(String::class.java)

                    val toUser = if (currentUserUid == user1) user2 else user1

                    // Create the message object.
                    val messageData =
                        mapOf("from" to currentUserUid, "to" to toUser, "message" to message)

                    // Save the message to the Messages node under the pair.
                    val messagesRef = pairRef.child("Messages").push()
                    messagesRef
                        .setValue(messageData)
                        .addOnSuccessListener {
                            // Message sent successfully.
                            Log.d(TAG, "Message sent successfully")
                        }
                        .addOnFailureListener { e ->
                            // Handle the error.
                            Log.e(TAG, "Error sending message: ${e.message}")
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error.
                    Log.e(TAG, "Database error: ${databaseError.message}")
                }
            }
        )
    }

    /**
     * Function to retrieve the pair ID associated with the given user.
     *
     * @param currentUser The ID of the current user.
     * @param onComplete Callback function to be called with the retrieved pair ID.
     */
    private fun getPairID(currentUser: String, onComplete: (String) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("AppUsers").child(currentUser)

        userRef.addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Get the pairID from the user's data.
                    val pairID = dataSnapshot.child("pairID").getValue(String::class.java)

                    // Call onComplete callback with the pairID.
                    onComplete(pairID ?: "No pairID")
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error.
                    Log.e(TAG, "Database error: ${databaseError.message}")
                    // Call onComplete callback with an empty string in case of error.
                    onComplete("")
                }
            }
        )
    }

    /**
     * Function to listen for incoming messages for the specified pair.
     *
     * @param pairId The ID of the pair for which to listen for messages.
     */
    private fun listenForMessages(pairId: String) {
        val pairRef = FirebaseDatabase.getInstance().getReference("Pairs/$pairId/Messages")

        pairRef.addChildEventListener(
            object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = snapshot.getValue(Message::class.java)
                    if (message != null) {
                        displayMessage(message)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    // No action needed for child changes in this context.
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // No action needed for child removals in this context.
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // No action needed for child movements in this context.
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to listen for messages.", error.toException())
                }
            }
        )
    }

    /**
     * Function to display a message in the chat interface.
     *
     * @param message The message object to display.
     */
    private fun displayMessage(message: Message) {
        val isCurrentUser = message.from == FirebaseAuth.getInstance().currentUser?.uid

        val messageTextView =
            TextView(this).apply {
                text = message.message
                layoutParams =
                    ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        .apply { bottomMargin = 10.dpToPx() }
                textSize = 20f

                // Set background and text color based on the sender
                if (isCurrentUser) {
                    setBackgroundResource(R.drawable.round_corner_message_bg_green)
                    setTextColor(Color.parseColor("#FFFFED"))
                } else {
                    setBackgroundResource(R.drawable.round_corner_message_bg_white)
                    setTextColor(Color.parseColor("#2D5F4C"))
                }

                setPadding(22, 11, 22, 11)
            }

        binding.messageContainer.addView(messageTextView)

        binding.messageContainer.post {
            binding.messageScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    /**
     * Extension function to convert dp to pixels.
     *
     * @return The pixel value equivalent to the given dp value.
     */
    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    /**
     * Deletes the pair and associated users from the database.
     *
     * @param pairId The ID of the pair containing the users to be deleted.
     * @param onComplete Callback function invoked upon completion, with a boolean indicating
     *   success or failure.
     */
    private fun deletePairAndUsers(pairId: String, onComplete: (Boolean) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val pairsRef = database.getReference("Pairs").child(pairId)

        // Retrieve user IDs from the pair
        pairsRef.addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val user1Id = dataSnapshot.child("user1").value.toString()
                    val user2Id = dataSnapshot.child("user2").value.toString()

                    val user1Ref = database.getReference("AppUsers").child(user1Id)
                    val user2Ref = database.getReference("AppUsers").child(user2Id)

                    // Delete the pair
                    pairsRef
                        .removeValue()
                        .addOnSuccessListener {
                            Log.d(TAG, "Pair $pairId deleted successfully")

                            // Delete user1
                            user1Ref
                                .removeValue()
                                .addOnSuccessListener {
                                    Log.d(TAG, "User $user1Id deleted successfully")

                                    // Delete user2
                                    user2Ref
                                        .removeValue()
                                        .addOnSuccessListener {
                                            Log.d(TAG, "User $user2Id deleted successfully")
                                            onComplete(true)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Error deleting user $user2Id: ${e.message}")
                                            onComplete(false)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error deleting user $user1Id: ${e.message}")
                                    onComplete(false)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error deleting pair $pairId: ${e.message}")
                            onComplete(false)
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Failed to read value.", databaseError.toException())
                    onComplete(false)
                }
            }
        )
    }

    /**
     * Data class representing a message.
     *
     * @param from The ID of the sender.
     * @param to The ID of the recipient.
     * @param message The content of the message.
     */
    data class Message(
        val from: String? = null,
        val to: String? = null,
        val message: String? = null
    )

    /**
     * Function to check if a pair ID exists.
     *
     * @param pairId The ID of the pair to check.
     * @return True if the pair ID exists, false otherwise.
     */
    private suspend fun checkPairIdExists(pairId: String): Boolean =
        withContext(Dispatchers.IO) {
            val database = FirebaseDatabase.getInstance()
            val pairRef = database.getReference("Pairs/$pairId")

            return@withContext suspendCoroutine<Boolean> { continuation ->
                pairRef.addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            continuation.resume(dataSnapshot.exists())
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e(TAG, "Error checking pair ID existence: ${databaseError.message}")
                            continuation.resume(false)
                        }
                    }
                )
            }
        }

    /** Function to periodically check for pair ID existence. */
    private fun checkPairIdPeriodically() {
        coroutineScope.launch {
            while (isActive) {
                delay(5000)
                val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserUid != null) {
                    getPairID(currentUserUid) { pairId ->
                        if (pairId.isNotEmpty()) {
                            coroutineScope.launch {
                                val exists = checkPairIdExists(pairId)
                                if (!exists) {
                                    NavigationUtils.startNewActivity(
                                        this@ChatPageActivity,
                                        ChooseDiscussionActivity::class.java
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Lifecycle method called when the activity is paused. */
    override fun onPause() {
        super.onPause()
        coroutineScope.coroutineContext.cancel()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Exit Chat")
            .setMessage("Are you sure you want to leave the chat?")
            .setPositiveButton("Yes") { _, _ ->
                // Call super method to allow the activity to be finished
                FirebaseDatabase.getInstance().reference.removeValue()
                super.onBackPressed()
            }
            .setNegativeButton("No", null) // Dismiss dialog if "No" is clicked
            .show()
    }
}
