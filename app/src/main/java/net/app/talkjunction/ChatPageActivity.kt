package net.app.talkjunction

import android.content.ContentValues.TAG
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
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

class ChatPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatPageBinding
    private var coroutineScope = CoroutineScope(Dispatchers.Main)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.secondary_theme_color)

        val messageBox = binding.messageEditText
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        coroutineScope = CoroutineScope(Dispatchers.Main)

        // Start checking for pair ID existence
        checkPairIdPeriodically()

        binding.endChat.setOnClickListener {
            if (currentUserUid != null) {
                // Retrieve the pair ID for the current user
                getPairID(currentUserUid) { pairId ->
                    if (pairId.isNotEmpty()) {
                        // Update pairID and isChatting fields for both users in the pair
                        updatePairFields(pairId) { result ->
                            if (result) {
                                NavigationUtils.startNewActivity(
                                    this,
                                    ChooseDiscussionActivity::class.java
                                )
                            }
                        }
                    } else {
                        // Handle the case where pairID is not found
                        Toast.makeText(this, "No active chat session to end", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }



        binding.sendBtn.setOnClickListener {
            val message = messageBox.text.toString().trim()
            if (currentUserUid != null)
                getPairID(currentUserUid) { pairId ->
                    if (pairId.isNotEmpty()) {
                        // If pairID is available, send the message to the pair
                        sendMessageToPair(pairId, message)
                        messageBox.text.clear()
                    } else {
                        // If pairID is not available, handle the case accordingly
                        Log.e(TAG, "Pair ID not found for the current user")
                    }
                }
        }

        if (currentUserUid != null) {
            getPairID(currentUserUid) { pairId ->
                if (pairId.isNotEmpty()) {
                    // If pairID is available, start listening for messages
                    listenForMessages(pairId)
                } else {
                    // If pairID is not available, handle the case accordingly
                    Log.e(TAG, "Pair ID not found for the current user")
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        NavigationUtils.startNewActivity(this, ChooseDiscussionActivity::class.java)
    }


    private fun sendMessageToPair(pairId: String, message: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val pairRef = FirebaseDatabase.getInstance().getReference("Pairs/$pairId")

        pairRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user1 = dataSnapshot.child("user1").getValue(String::class.java)
                val user2 = dataSnapshot.child("user2").getValue(String::class.java)

                val toUser = if (currentUserUid == user1) user2 else user1

                // Create the message object
                val messageData = mapOf(
                    "from" to currentUserUid,
                    "to" to toUser,
                    "message" to message
                )

                // Save the message to the Messages node under the pair
                val messagesRef = pairRef.child("Messages").push()
                messagesRef.setValue(messageData)
                    .addOnSuccessListener {
                        // Message sent successfully
                        Log.d(TAG, "Message sent successfully")
                    }
                    .addOnFailureListener { e ->
                        // Handle the error
                        Log.e(TAG, "Error sending message: ${e.message}")
                    }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
                Log.e(TAG, "Database error: ${databaseError.message}")
            }
        })
    }

    private fun getPairID(currentUser: String, onComplete: (String) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("AppUsers").child(currentUser)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get the pairID from the user's data
                val pairID = dataSnapshot.child("pairID").getValue(String::class.java)

                // Call onComplete callback with the pairID
                onComplete(pairID ?: "No pairID")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
                Log.e(TAG, "Database error: ${databaseError.message}")
                // Call onComplete callback with an empty string in case of error
                onComplete("")
            }
        })
    }

    private fun listenForMessages(pairId: String) {
        val pairRef = FirebaseDatabase.getInstance().getReference("Pairs/$pairId/Messages")

        // Listen for new messages
        pairRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    displayMessage(message)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // This method is called when a child node in the database is updated.
                // In a chat application where users can't edit their messages, and messages are displayed as they are received,
                // there is currently no need to handle child node updates.
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // This method is called when a child node is removed from the database.
                // In this chat application, users don't have the ability to delete their messages,
                // so there's no need to handle child node removals.
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // This method is called when a child node is moved to a different location within the database.
                // Since messages are displayed in the order they were sent and there's no need to rearrange them,
                // handling child node movements is not necessary in this context.
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to listen for messages.", error.toException())
            }
        })
    }

    private fun displayMessage(message: Message) {
        // Determine if the message is sent by the current user or the other user
        val isCurrentUser = message.from == FirebaseAuth.getInstance().currentUser?.uid

        // Determine the appropriate gravity for the message based on the sender
        val gravity = if (isCurrentUser) Gravity.END else Gravity.START

        // Create a new TextView to display the message
        val messageTextView = TextView(this).apply {
            text = message.message
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setGravity(gravity)
                bottomMargin = 10.dpToPx() // Convert dp to pixels
            }
            textSize = 20f // Set the text size to 20sp
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.rounded_corner_bg)
            setPadding(22, 11, 22, 11) // Add padding for better readability
        }

        // Add the TextView to the appropriate side of the layout
        binding.messageContainer.addView(messageTextView)

        // Optionally, scroll the message container to the bottom to show the latest message
        binding.messageContainer.post {
            binding.messageScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun updatePairFields(pairId: String, onComplete: (Boolean) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val pairsRef = database.getReference("Pairs").child(pairId)

        // Get the pair data
        pairsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user1 = dataSnapshot.child("user1").value.toString()
                val user2 = dataSnapshot.child("user2").value.toString()

                val user1Ref = database.getReference("AppUsers/$user1")
                val user2Ref = database.getReference("AppUsers/$user2")


                // Update pairID and isChatting fields to null and false, respectively for user 1
                user1Ref.removeValue()
                .addOnSuccessListener {
                    Log.d(TAG,  "$user1 removed from database")
                    // Success for user 1, proceed with updating user 2
                    user2Ref.removeValue(
                    ).addOnSuccessListener {
                        Log.d(TAG, "$user2 removed from database")
                        // Success for both users, delete the pair node
                        pairsRef.removeValue()
                            .addOnSuccessListener {
                                Log.d(TAG, "Pair deleted successfully")
                                onComplete(true) // Indicate success
                            }.addOnFailureListener { e ->
                                Log.e(TAG, "Error deleting pair: ${e.message}")
                                onComplete(false) // Indicate failure
                            }
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Error deleting $user2: ${e.message}")
                        onComplete(false) // Indicate failure
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error deleting $user1: ${e.message}")
                    onComplete(false) // Indicate failure
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Failed to read value.", databaseError.toException())
                onComplete(false) // Indicate failure
            }
        })
    }

    // Define the Message data class representing a single message
    data class Message(
        val from: String? = null,
        val to: String? = null,
        val message: String? = null
    )

    private suspend fun checkPairIdExists(pairId: String): Boolean = withContext(Dispatchers.IO) {
        val database = FirebaseDatabase.getInstance()
        val pairRef = database.getReference("Pairs/$pairId")

        return@withContext suspendCoroutine<Boolean> { continuation ->
            pairRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    continuation.resume(dataSnapshot.exists())
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Error checking pair ID existence: ${databaseError.message}")
                    continuation.resume(false) // Resume with false when cancelled
                }
            })
        }
    }

    // Function to periodically check for pair ID existence
    private fun checkPairIdPeriodically() {
        coroutineScope.launch {
            while (isActive) {
                delay(5000) // Check every 5 seconds

                val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserUid != null) {
                    // Retrieve the pair ID for the current user
                    getPairID(currentUserUid) { pairId ->
                        if (pairId.isNotEmpty()) {
                            // Check if the pair ID exists
                            coroutineScope.launch {
                                val exists = checkPairIdExists(pairId)
                                if (!exists) {
                                    // Start login activity if pair ID doesn't exist
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

    override fun onPause() {
        super.onPause()
        // Cancel the coroutine when the activity is not visible
        coroutineScope.coroutineContext.cancel()
    }

}


