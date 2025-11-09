package com.example.vybzchat.utils

import com.example.vybzchat.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    // REMOVED: val storage: FirebaseStorage = FirebaseStorage.getInstance()

    fun currentUid(): String? = auth.currentUser?.uid

    fun getUserDoc(uid: String) = firestore.collection("users").document(uid)

    fun sendMessage(chatId: String, msg: Message, onComplete: (Boolean, Exception?) -> Unit) {
        val docRef = firestore.collection("chats").document(chatId)
            .collection("messages").document()
        val withId = msg.copy(id = docRef.id)
        docRef.set(withId).addOnCompleteListener { t ->
            onComplete(t.isSuccessful, t.exception)
        }
    }

    // REMOVED: uploadImage function since we're using Cloudinary directly now
    // The image upload is handled by ImageUploader.kt with Cloudinary
}