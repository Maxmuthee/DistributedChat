package com.example.vybzchat.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.vybzchat.databinding.ItemMessageBinding
import com.example.vybzchat.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val messageList: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Format timestamp to HH:mm
        val timeText = message.timestamp?.let {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(it))
        } ?: ""

        with(holder.binding) {
            // Reset visibility
            imageSent.visibility = View.GONE
            imageReceived.visibility = View.GONE
            textSent.visibility = View.GONE
            textReceived.visibility = View.GONE
            textCaptionSent.visibility = View.GONE
            textCaptionReceived.visibility = View.GONE

            // SET DARK COLORS FOR ALL TEXT - PERFECT FOR WHITE BACKGROUND
            textSent.setTextColor(Color.parseColor("#000000")) // Pure black
            textSenderNameSent.setTextColor(Color.parseColor("#333333")) // Dark gray
            tvTimeSent.setTextColor(Color.parseColor("#666666")) // Medium gray

            textReceived.setTextColor(Color.parseColor("#000000")) // Pure black
            textSenderNameReceived.setTextColor(Color.parseColor("#333333")) // Dark gray
            tvTimeReceived.setTextColor(Color.parseColor("#666666")) // Medium gray

            textCaptionSent.setTextColor(Color.parseColor("#000000")) // Pure black
            textCaptionReceived.setTextColor(Color.parseColor("#000000")) // Pure black

            if (message.senderId == currentUserId) {
                // Show message on the right (sent)
                layoutSent.visibility = View.VISIBLE
                layoutReceived.visibility = View.GONE
                textSenderNameSent.text = message.senderName ?: "You"
                tvTimeSent.text = timeText

                // Check if this is an image message
                if (!message.imageUrl.isNullOrEmpty()) {
                    // Show CHAT image
                    imageSent.visibility = View.VISIBLE
                    Glide.with(imageSent.context)
                        .load(message.imageUrl)
                        .override(600, 600)
                        .centerCrop()
                        .into(imageSent)

                    // Show caption if available
                    if (!message.text.isNullOrEmpty()) {
                        textCaptionSent.visibility = View.VISIBLE
                        textCaptionSent.text = message.text
                    }
                } else {
                    // Show text message
                    textSent.visibility = View.VISIBLE
                    textSent.text = message.text ?: ""
                }

            } else {
                // Show message on the left (received)
                layoutSent.visibility = View.GONE
                layoutReceived.visibility = View.VISIBLE
                textSenderNameReceived.text = message.senderName ?: "Unknown"
                tvTimeReceived.text = timeText

                // Load USER PROFILE PICTURE from Firestore (NOT from message.imageUrl)
                loadUserProfilePicture(message.senderId, imageProfile)

                // Check if this is an image message (CHAT image)
                if (!message.imageUrl.isNullOrEmpty()) {
                    // Show CHAT image
                    imageReceived.visibility = View.VISIBLE
                    Glide.with(imageReceived.context)
                        .load(message.imageUrl)
                        .override(600, 600)
                        .centerCrop()
                        .into(imageReceived)

                    // Show caption if available
                    if (!message.text.isNullOrEmpty()) {
                        textCaptionReceived.visibility = View.VISIBLE
                        textCaptionReceived.text = message.text
                    }
                } else {
                    // Show text message
                    textReceived.visibility = View.VISIBLE
                    textReceived.text = message.text ?: ""
                }
            }
        }
    }

    // NEW METHOD: Load user profile picture from Firestore
    private fun loadUserProfilePicture(userId: String, imageView: android.widget.ImageView) {
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val profilePictureUrl = document.getString("profilePicture")
                if (!profilePictureUrl.isNullOrEmpty()) {
                    // Load the actual profile picture
                    Glide.with(imageView.context)
                        .load(profilePictureUrl)
                        .circleCrop()
                        .override(100, 100) // Proper size for profile picture
                        .into(imageView)
                } else {
                    // Fallback to default image if no profile picture
                    imageView.setImageResource(android.R.drawable.ic_menu_myplaces)
                }
            }
            .addOnFailureListener {
                // Fallback to default image on error
                imageView.setImageResource(android.R.drawable.ic_menu_myplaces)
            }
    }

    override fun getItemCount(): Int = messageList.size
}