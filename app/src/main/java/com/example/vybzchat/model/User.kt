package com.example.vybzchat.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0L
)
