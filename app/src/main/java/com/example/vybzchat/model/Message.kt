package com.example.vybzchat.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val timestamp: Long = 0L
)
