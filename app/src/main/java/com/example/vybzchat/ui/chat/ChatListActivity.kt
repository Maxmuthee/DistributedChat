package com.example.vybzchat.ui.chat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vybzchat.databinding.ActivityChatListBinding
import com.example.vybzchat.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.example.vybzchat.ui.auth.ChatActivity

class ChatListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatListBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // For the assignment: keep one default global chat room id "global_chat"
        binding.btnOpenChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("chatId", "global_chat")
            startActivity(intent)
        }

        binding.btnSignOut.setOnClickListener {
            val uid = FirebaseHelper.currentUid()
            if (uid != null) {
                FirebaseHelper.getUserDoc(uid).update(mapOf("online" to false, "lastSeen" to System.currentTimeMillis()))
            }
            FirebaseHelper.auth.signOut()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        // mark online
        FirebaseHelper.currentUid()?.let {
            FirebaseHelper.getUserDoc(it).update("online", true)
        }
    }

    override fun onStop() {
        super.onStop()
        FirebaseHelper.currentUid()?.let {
            FirebaseHelper.getUserDoc(it).update("online", false)
            FirebaseHelper.getUserDoc(it).update("lastSeen", System.currentTimeMillis())
        }
    }
}
