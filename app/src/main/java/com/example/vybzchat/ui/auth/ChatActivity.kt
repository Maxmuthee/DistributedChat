package com.example.vybzchat.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vybzchat.adapter.MessageAdapter
import com.example.vybzchat.databinding.ActivityChatBinding
import com.example.vybzchat.model.Message
import com.example.vybzchat.utils.FirebaseHelper
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var chatId: String
    private var messagesListener: ListenerRegistration? = null
    private var typingListener: ListenerRegistration? = null
    private val PICK_IMAGE = 1234

    private val messageList = mutableListOf<Message>()
    private var currentUserName: String? = null
    private val usersMap = mutableMapOf<String, String>() // Cache for usernames

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra("chatId") ?: "global_chat"
        adapter = MessageAdapter(messageList)

        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter

        // Load current user's username and cache other users
        val uid = FirebaseHelper.currentUid()
        if (uid != null) {
            FirebaseHelper.firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    currentUserName = doc.getString("username") ?: "Anonymous"
                    usersMap[uid] = currentUserName!!
                }
        }

        // Button send message
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val userId = FirebaseHelper.currentUid() ?: return@setOnClickListener
            val senderName = currentUserName ?: "Anonymous"

            val msg = Message(
                senderId = userId,
                senderName = senderName,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            FirebaseHelper.sendMessage(chatId, msg) { ok, err ->
                if (ok) binding.etMessage.setText("")
                else Toast.makeText(this, "Send failed: ${err?.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        // Button send image
        binding.btnImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_IMAGE)
        }

        // Typing indicator setup
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            var timer = Timer()
            override fun afterTextChanged(s: Editable?) {
                val uid = FirebaseHelper.currentUid() ?: return
                val typingRef = FirebaseHelper.firestore.collection("chats").document(chatId)
                    .collection("typing").document(uid)

                if (s.isNullOrEmpty()) {
                    typingRef.delete()
                } else {
                    typingRef.set(mapOf("typing" to true, "updated" to System.currentTimeMillis()))
                    timer.cancel()
                    timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            typingRef.delete()
                        }
                    }, 2500)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onStart() {
        super.onStart()
        val uid = FirebaseHelper.currentUid() ?: return
        FirebaseHelper.getUserDoc(uid).update(mapOf("online" to true))
        listenMessages()
        listenTyping()
    }

    override fun onStop() {
        super.onStop()
        val uid = FirebaseHelper.currentUid() ?: return
        FirebaseHelper.getUserDoc(uid).update(
            mapOf(
                "online" to false,
                "lastSeen" to System.currentTimeMillis()
            )
        )
        messagesListener?.remove()
        typingListener?.remove()
    }

    private fun listenMessages() {
        messagesListener = FirebaseHelper.firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val items = snap?.documents?.mapNotNull { it.toObject(Message::class.java) } ?: emptyList()
                messageList.clear()
                messageList.addAll(items)
                adapter.notifyDataSetChanged()
                binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
            }
    }

    private fun listenTyping() {
        typingListener = FirebaseHelper.firestore.collection("chats").document(chatId)
            .collection("typing")
            .addSnapshotListener { snap, _ ->
                val typingDocs = snap?.documents?.filter { it.id != FirebaseHelper.currentUid() } ?: emptyList()

                if (typingDocs.isNotEmpty()) {
                    val typingNames = mutableListOf<String>()
                    var processedCount = 0

                    typingDocs.forEach { doc ->
                        val uid = doc.id
                        if (usersMap.containsKey(uid)) {
                            typingNames.add(usersMap[uid]!!)
                            processedCount++
                            if (processedCount == typingDocs.size) {
                                binding.tvTyping.text = typingNames.joinToString(", ") + " is typing..."
                                binding.tvTyping.visibility = View.VISIBLE
                            }
                        } else {
                            FirebaseHelper.firestore.collection("users").document(uid).get()
                                .addOnSuccessListener { userDoc ->
                                    val username = userDoc.getString("username") ?: "Someone"
                                    usersMap[uid] = username
                                    typingNames.add(username)
                                    processedCount++
                                    if (processedCount == typingDocs.size) {
                                        binding.tvTyping.text = typingNames.joinToString(", ") + " is typing..."
                                        binding.tvTyping.visibility = View.VISIBLE
                                    }
                                }
                        }
                    }
                } else {
                    binding.tvTyping.visibility = View.GONE
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            FirebaseHelper.uploadImage(chatId, uri) { url, err ->
                if (url != null) {
                    val uid = FirebaseHelper.currentUid() ?: return@uploadImage
                    val senderName = currentUserName ?: "Anonymous"
                    val msg = Message(
                        senderId = uid,
                        senderName = senderName,
                        imageUrl = url,
                        timestamp = System.currentTimeMillis()
                    )
                    FirebaseHelper.sendMessage(chatId, msg) { ok, e ->
                        if (!ok)
                            Toast.makeText(this, "Image send failed: ${e?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Upload failed: ${err?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
