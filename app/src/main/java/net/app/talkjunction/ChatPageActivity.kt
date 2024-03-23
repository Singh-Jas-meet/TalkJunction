package net.app.talkjunction

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.app.talkjunction.databinding.ActivityChatPageBinding
import net.app.talkjunction.databinding.ActivityMainBinding

class ChatPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatPageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}