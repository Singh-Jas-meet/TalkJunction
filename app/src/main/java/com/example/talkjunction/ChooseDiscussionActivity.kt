package com.example.talkjunction

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.talkjunction.databinding.ActivityChooseDiscussionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ChooseDiscussionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChooseDiscussionBinding
    private var firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChooseDiscussionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.findChatterBtn.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
        }
    }
}