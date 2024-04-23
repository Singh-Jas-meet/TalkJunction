package net.app.talkjunction

import android.content.ContentValues.TAG
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.divider.MaterialDivider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.app.talkjunction.databinding.ActivityChooseDiscussionBinding

class ChooseDiscussionActivity : AppCompatActivity() {

    // Binding instance to access views in the layout
    private lateinit var binding: ActivityChooseDiscussionBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setting the status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.secondary_theme_color)

        // Inflating the layout using ViewBinding
        binding = ActivityChooseDiscussionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up RadioGroup listener
        setupRadioGroupListener()

        // Initializing FirebaseAuth instance
        firebaseAuth = FirebaseAuth.getInstance()

        //         Set click listeners for buttons
        //        image button to add new radio button
        binding.addTopicImgBtn.setOnClickListener { addRadioButtonAndDivider() }

        //      connect btn to handle finding a pair user and starting the chat activity
        binding.connectBtn.setOnClickListener { handleConnectButtonClick() }

        //      logout btn removes user from AppUsers node
        binding.logoutBtn.setOnClickListener {
            val database = FirebaseDatabase.getInstance()
            val appUsersRef = database.getReference("AppUsers")
            val currentUser = firebaseAuth.currentUser?.uid

            // Get reference to the specific child node under "AppUsers" and delete it
            if (currentUser != null) {
                val userToDeleteRef = appUsersRef.child(currentUser)
                userToDeleteRef
                    .removeValue()
                    .addOnSuccessListener {
                        Log.d(TAG, "User deleted successfully")
                        // Optionally, show a message to the user or perform any other actions
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error deleting user: ${e.message}")
                        // Handle the error, show an error message, etc.
                    }
            }

            // Revoke Google sign-in access and sign out the user
            GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).revokeAccess()
            firebaseAuth.signOut()

            // Navigate to the main activity
            NavigationUtils.startNewActivity(this, MainActivity::class.java)
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // Function to set up the RadioGroup listener to update user fields whenever he clicks on a
    // radio button
    private fun setupRadioGroupListener() {
        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            //            find selected radio button
            val selectedRadioButton = group.findViewById<RadioButton>(checkedId)
            val selectedText = selectedRadioButton?.text?.toString()?.lowercase() ?: ""
            if (selectedText.isNotEmpty()) {
                //                call the method below to update the database
                saveCheckedRadioButtonToFirebase(selectedText)
            }
        }
    }

    // Function to dynamically add a RadioButton and MaterialDivider
    private fun addRadioButtonAndDivider() {
        val text = binding.addTopicEditText.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter a topic", Toast.LENGTH_SHORT).show()
        } else {
            //          extract text from editText
            val capitalizedText = text.substring(0, 1).uppercase() + text.substring(1)
            val hasSelectedRadioButton = binding.radioGroup.checkedRadioButtonId != -1

            //            creating the radio button with new interest as it's text
            val radioButton =
                RadioButton(this).apply {
                    this.text = capitalizedText
                    this.textSize = 20F
                    this.setTextColor(Color.BLACK)
                    buttonTintList = ColorStateList.valueOf(Color.parseColor("#2d5f4c"))
                    this.setPadding(0, 20, 0, 20)
                    this.layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                }

            if (hasSelectedRadioButton) {
                clearRadioGroupSelection(binding.radioGroup)
            }

            //            add the new button to the radio group
            binding.radioGroup.addView(radioButton)
            radioButton.isChecked = true

            //            add the material divider for visual purposes
            val divider =
                MaterialDivider(this).apply {
                    this.layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    this.dividerInsetEnd = 16
                    this.dividerInsetStart = 16
                }

            binding.radioGroup.addView(divider)
            binding.addTopicEditText.setText("")
        }
    }

    // Function to clear the selection of all radio buttons in a radio group
    private fun clearRadioGroupSelection(radioGroup: RadioGroup) {
        //        clears the radio group
        radioGroup.clearCheck()
    }

    // Function to retrieve the text of the checked RadioButton
    private fun grabCheckedRadioButtonText(): String {
        val selectedRadioButtonId = binding.radioGroup.checkedRadioButtonId
        if (selectedRadioButtonId != -1) {
            val selectedRadioButton = binding.root.findViewById<RadioButton>(selectedRadioButtonId)
            return selectedRadioButton.text.toString().lowercase()
        }
        return ""
    }

    // Function to save the checked RadioButton to Firebase Realtime Database
    private fun saveCheckedRadioButtonToFirebase(topic: String) {
        val user = firebaseAuth.currentUser
        user?.let { currentUser ->
            val database = FirebaseDatabase.getInstance()
            //            get AppUser node reference
            val usersRef = database.getReference("AppUsers")
            val currentTimeStamp = System.currentTimeMillis()

            //            organize data in a hashmap where key's are field names and values are
            // current user's value for those fields
            val userData =
                hashMapOf(
                    "Uid" to user.uid,
                    "Email" to user.email.toString(),
                    "Interest" to topic,
                    "Timestamp" to currentTimeStamp,
                    "isChatting" to false,
                    "pairID" to "null"
                )

            //            save it to the database
            usersRef
                .child(currentUser.uid)
                .setValue(userData)
                .addOnSuccessListener {
                    Toast.makeText(applicationContext, "Interests saved", Toast.LENGTH_SHORT).show()
                    // checking for pairs here(if the current user exists in a pair start the chat
                    // activity)
                    checkUserInPair(user.uid, this) { userInPair ->
                        if (userInPair) {
                            NavigationUtils.startNewActivity(
                                this@ChooseDiscussionActivity,
                                ChatPageActivity::class.java
                            )
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding document", e)
                    Toast.makeText(applicationContext, "Error saving interests", Toast.LENGTH_SHORT)
                        .show()
                }
        } ?: Log.e(TAG, "Current user is null")
    }

    // Function to generate a unique chatID to save multiple pairs in Pairs node of the database
    private fun generateChatID(): String {
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..5).map { allowedChars.random() }.joinToString("")
    }

    /**
     * Fetches a user with the specified interest from the database and sets up a chat pair if
     * found. Calls getRandomUser if no suitable user with the same interest is found.
     *
     * @param interest The interest to search for in users.
     * @param onSuccess Callback function invoked when a matching user is found with their user ID.
     * @param onFailure Callback function invoked when no suitable user is found or an error occurs.
     */
    private fun getUserWithInterest(
        interest: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("AppUsers")

        // Listener for querying users with the specified interest
        val queryListener =
            object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val users =
                        dataSnapshot.children.filter { userSnapshot ->
                            val userInterest =
                                userSnapshot.child("Interest").getValue(String::class.java)
                            val isUserChatting =
                                userSnapshot.child("isChatting").getValue(Boolean::class.java)
                            userInterest == interest &&
                                isUserChatting == false &&
                                userSnapshot.key != currentUserUid
                        }
                    if (users.isNotEmpty()) {
                        // Select any user with the same interest
                        val matchedUser = users.random()
                        val matchedUserId = matchedUser.key ?: ""

                        // Proceed with pair creation
                        val chatID = generateChatID()
                        val batchUpdates = HashMap<String, Any>()
                        batchUpdates["$matchedUserId/pairID"] = chatID
                        batchUpdates["$matchedUserId/isChatting"] = true
                        currentUserUid?.let { uid ->
                            batchUpdates["$uid/pairID"] = chatID
                            batchUpdates["$uid/isChatting"] = true
                        }

                        usersRef
                            .updateChildren(batchUpdates)
                            .addOnSuccessListener { onSuccess(matchedUserId) }
                            .addOnFailureListener { exception -> onFailure(exception) }
                    } else {
                        // If no suitable user found, call getRandomUser
                        getRandomUser(onSuccess, onFailure)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    onFailure(databaseError.toException())
                }
            }

        usersRef.addListenerForSingleValueEvent(queryListener)
    }

    // Function to get a random user(for cases where user with matching interests does not exist)
    private fun getRandomUser(onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("AppUsers")

        val queryListener =
            object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val availableUsers =
                        dataSnapshot.children.filter { userSnapshot ->
                            val isUserChatting =
                                userSnapshot.child("isChatting").getValue(Boolean::class.java)
                            val userId = userSnapshot.key
                            userId != currentUserUid && (isUserChatting == false)
                        }
                    if (availableUsers.isNotEmpty()) {
                        val randomUser = availableUsers.random()
                        val randomUserId = randomUser.key ?: ""

                        // Generate a unique chatID
                        val chatID = generateChatID()

                        // Update chatID and isChatting fields for both users
                        val batchUpdates = HashMap<String, Any>()
                        batchUpdates["$randomUserId/pairID"] = chatID
                        batchUpdates["$randomUserId/isChatting"] = true
                        currentUserUid?.let { uid ->
                            batchUpdates["$uid/pairID"] = chatID
                            batchUpdates["$uid/isChatting"] = true
                        }

                        // Update the "chatID" field for both users
                        usersRef
                            .updateChildren(batchUpdates)
                            .addOnSuccessListener { onSuccess(randomUserId) }
                            .addOnFailureListener { exception -> onFailure(exception) }
                    } else {
                        onFailure(Exception("No suitable user found in the database"))
                        Toast.makeText(
                                this@ChooseDiscussionActivity,
                                "No pairs available",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    onFailure(databaseError.toException())
                }
            }

        usersRef.addListenerForSingleValueEvent(queryListener)
    }

    // Function to handle the connect button click
    private fun handleConnectButtonClick() {
        val interest = grabCheckedRadioButtonText().trim()
        if (interest.isNotEmpty()) {
            //            start finding the pairing user
            getUserWithInterest(
                interest = interest,
                onSuccess = { matchedUserId ->
                    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                    currentUserUid?.let { uid ->
                        val database = FirebaseDatabase.getInstance()
                        val usersRef = database.getReference("AppUsers")
                        val currentUserRef =
                            usersRef.child(uid) // Reference to the current user's data
                        currentUserRef.addListenerForSingleValueEvent(
                            object : ValueEventListener {
                                override fun onDataChange(currentUserSnapshot: DataSnapshot) {
                                    if (currentUserSnapshot.exists()) {

                                        // on success start the
                                        // pair creation process with the two matched users
                                        createPairDocument(
                                            currentUserRef,
                                            usersRef.child(matchedUserId)
                                        )
                                        checkUserInPair(
                                            currentUserUid,
                                            this@ChooseDiscussionActivity
                                        ) { userInPair ->
                                            //                                        if user in a
                                            // pair, start the chat activity
                                            if (userInPair) {
                                                NavigationUtils.startNewActivity(
                                                    this@ChooseDiscussionActivity,
                                                    ChatPageActivity::class.java
                                                )
                                            } else {
                                                Log.e(TAG, "User not in pair")
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Current user data not found in the database")
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    Log.e(TAG, "Database error: ${databaseError.message}")
                                }
                            }
                        )
                    } ?: Log.e(TAG, "Current user UID is null")
                },
                onFailure = { exception -> Log.e(TAG, "Error getting user: ${exception.message}") }
            )
        } else {
            Toast.makeText(this, "Please make a selection", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to create a pair document
    private fun createPairDocument(
        user1Reference: DatabaseReference,
        user2Reference: DatabaseReference
    ) {
        val database = FirebaseDatabase.getInstance()
        val pairsRef = database.getReference("Pairs")

        // Get the chatID from user1Reference
        user1Reference.addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(dataSnapshot1: DataSnapshot) {
                    val user1Data = dataSnapshot1.value as? Map<*, *>
                    val chatID1 = user1Data?.get("pairID") as? String

                    // Proceed only if chatID1 is not null
                    if (chatID1 != null) {
                        //                    create a hashmap with pair data
                        val pairData =
                            mapOf(
                                "user1" to user1Reference.key,
                                "user2" to user2Reference.key,
                                "pairID" to chatID1 // Save chatID from user1
                            )

                        // add the data to the Pairs node
                        pairsRef
                            .child(chatID1)
                            .setValue(pairData)
                            .addOnSuccessListener { Log.d("Pair creation: ", "Successful") }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error creating pair document", e)
                                Toast.makeText(
                                        this@ChooseDiscussionActivity,
                                        "No user available",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }
                    } else {
                        Log.e(TAG, "ChatID not found in user1Reference")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Failed to read value.", databaseError.toException())
                }
            }
        )
    }

    // Function to check if a user is in a pair
    private fun checkUserInPair(
        currentUserUid: String,
        activity: AppCompatActivity,
        onComplete: (Boolean) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("AppUsers")

        if (activity is ChatPageActivity) {
            // Don't proceed if the current activity is ChatPageActivity
            onComplete(false)
            return
        }

        fun checkInPair() {
            //            get the pairID from current user
            val userChatIDRef = usersRef.child(currentUserUid).child("pairID")

            userChatIDRef.addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val userChatID = dataSnapshot.getValue(String::class.java)

                        if (userChatID != null) {

                            //  get Pairs node reference and start comparing
                            // it's child with current user's pair
                            val pairsRef = database.getReference("Pairs")
                            pairsRef.addListenerForSingleValueEvent(
                                object : ValueEventListener {
                                    override fun onDataChange(pairsSnapshot: DataSnapshot) {
                                        var userInPair = false
                                        for (pairSnapshot in pairsSnapshot.children) {
                                            //                                    if a child
                                            // matches, meaning the user exists in a pair
                                            val pairName = pairSnapshot.key
                                            if (pairName == userChatID) {
                                                userInPair = true
                                                break
                                            }
                                        }
                                        // if user is in a pair, then
                                        // returns true
                                        if (userInPair) {
                                            onComplete(true)
                                        } else {
                                            // otherwise retry after a delay using coroutines
                                            CoroutineScope(Dispatchers.Main).launch {
                                                delay(1000) // Delay in milliseconds
                                                checkInPair()
                                            }
                                        }
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        onComplete(false)
                                    }
                                }
                            )
                        } else {
                            onComplete(false)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        onComplete(false)
                    }
                }
            )
        }

        checkInPair()
    }
}
