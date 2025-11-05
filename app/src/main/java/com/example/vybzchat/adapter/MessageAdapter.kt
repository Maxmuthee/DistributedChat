package com.example.vybzchat.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.vybzchat.databinding.ItemMessageBinding
import com.example.vybzchat.model.Message
import com.google.firebase.auth.FirebaseAuth
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
        val sdf = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault())

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Format timestamp to HH:mm
        val timeText = message.timestamp?.let {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(it))
        } ?: ""

        with(holder.binding) {
            // SET BLACK TEXT COLORS FOR ALL TEXT VIEWS
            textSent.setTextColor(Color.BLACK)
            textSenderNameSent.setTextColor(Color.BLACK)
            tvTimeSent.setTextColor(Color.parseColor("#666666"))

            textReceived.setTextColor(Color.BLACK)
            textSenderNameReceived.setTextColor(Color.BLACK)
            tvTimeReceived.setTextColor(Color.parseColor("#666666"))

            if (message.senderId == currentUserId) {
                // Show message on the right (sent)
                layoutSent.visibility = android.view.View.VISIBLE
                layoutReceived.visibility = android.view.View.GONE
                textSent.text = message.text
                textSenderNameSent.text = message.senderName ?: "You"
                tvTimeSent.text = timeText

            } else {
                // Show message on the left (received)
                layoutSent.visibility = android.view.View.GONE
                layoutReceived.visibility = android.view.View.VISIBLE
                textReceived.text = message.text
                textSenderNameReceived.text = message.senderName ?: "Unknown"
                tvTimeReceived.text = timeText

                // Load user profile image if available
                if (!message.imageUrl.isNullOrEmpty()) {
                    Glide.with(imageProfile.context)
                        .load(message.imageUrl)
                        .into(imageProfile)
                } else {
                    imageProfile.setImageResource(android.R.drawable.ic_menu_myplaces)
                }
            }
        }
    }

    override fun getItemCount(): Int = messageList.size
}