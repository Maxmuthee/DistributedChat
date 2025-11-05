package com.example.vybzchat.utils

import android.net.Uri
import com.example.vybzchat.model.Message
import com.example.vybzchat.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseHelper {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val storage: FirebaseStorage = FirebaseStorage.getInstance()

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

    fun uploadImage(chatId: String, uri: Uri, onComplete: (String?, Exception?) -> Unit) {
        val fileName = System.currentTimeMillis().toString()
        val ref = storage.reference.child("chat_images/$chatId/$fileName")
        ref.putFile(uri).continueWithTask { it.result?.storage?.downloadUrl }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onComplete(task.result.toString(), null)
                else onComplete(null, task.exception)
            }
    }
}
