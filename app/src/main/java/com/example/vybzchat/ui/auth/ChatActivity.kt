package com.example.vybzchat.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vybzchat.adapter.MessageAdapter
import com.example.vybzchat.databinding.ActivityChatBinding
import com.example.vybzchat.model.Message
import com.example.vybzchat.utils.FirebaseHelper
import com.google.firebase.firestore.ListenerRegistration

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var chatId: String
    private var messagesListener: ListenerRegistration? = null
    private val PICK_IMAGE = 1234

    private val messageList = mutableListOf<Message>()
    private var currentUserName: String? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeChat()
        setupClickListeners()
    }

    private fun initializeChat() {
        chatId = intent.getStringExtra("chatId") ?: "global_chat"
        currentUserId = FirebaseHelper.currentUid()

        setupRecyclerView()
        loadCurrentUser()

        // Set up back button if you added one
        binding.btnBack?.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(messageList)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun loadCurrentUser() {
        val uid = currentUserId ?: return
        FirebaseHelper.firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("username") ?: "Anonymous"
            }
            .addOnFailureListener {
                currentUserName = "Anonymous"
            }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.btnImage.setOnClickListener {
            pickImageFromGallery()
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) {
            binding.etMessage.error = "Message cannot be empty"
            return
        }

        val userId = currentUserId ?: run {
            Toast.makeText(this, "You must be logged in to send messages", Toast.LENGTH_SHORT).show()
            return
        }

        val senderName = currentUserName ?: "Anonymous"

        val msg = Message(
            senderId = userId,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        // Show sending state
        binding.btnSend.isEnabled = false
        binding.btnSend.text = "Sending..."

        FirebaseHelper.sendMessage(chatId, msg) { ok, err ->
            binding.btnSend.isEnabled = true
            binding.btnSend.text = "Send"

            if (ok) {
                binding.etMessage.setText("")
                binding.etMessage.error = null
            } else {
                Toast.makeText(this, "Failed to send message: ${err?.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onStart() {
        super.onStart()
        updateUserPresence(true)
        listenMessages()
    }

    override fun onStop() {
        super.onStop()
        updateUserPresence(false)
        cleanupListeners()
    }

    private fun updateUserPresence(online: Boolean) {
        val uid = currentUserId ?: return
        val updates = if (online) {
            mapOf("online" to true)
        } else {
            mapOf(
                "online" to false,
                "lastSeen" to System.currentTimeMillis()
            )
        }
        FirebaseHelper.getUserDoc(uid).update(updates)
    }

    private fun listenMessages() {
        messagesListener = FirebaseHelper.firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, "Error loading messages: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val items = snap?.documents?.mapNotNull { it.toObject(Message::class.java) } ?: emptyList()
                messageList.clear()
                messageList.addAll(items)
                adapter.notifyDataSetChanged()

                // Scroll to bottom
                if (adapter.itemCount > 0) {
                    binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
                }
            }
    }

    private fun cleanupListeners() {
        messagesListener?.remove()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            uploadImage(uri)
        }
    }

    private fun uploadImage(uri: android.net.Uri) {
        binding.btnImage.isEnabled = false
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

        FirebaseHelper.uploadImage(chatId, uri) { url, err ->
            binding.btnImage.isEnabled = true

            if (url != null) {
                sendImageMessage(url)
            } else {
                Toast.makeText(this, "Upload failed: ${err?.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendImageMessage(imageUrl: String) {
        val uid = currentUserId ?: return
        val senderName = currentUserName ?: "Anonymous"

        val msg = Message(
            senderId = uid,
            senderName = senderName,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis()
        )

        FirebaseHelper.sendMessage(chatId, msg) { ok, e ->
            if (!ok) {
                Toast.makeText(this, "Failed to send image: ${e?.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Add any custom back press logic here
    }
}