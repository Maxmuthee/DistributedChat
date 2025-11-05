package com.example.vybzchat.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vybzchat.databinding.ActivityLoginBinding
import com.example.vybzchat.ui.chat.ChatListActivity
import com.example.vybzchat.utils.FirebaseHelper

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (FirebaseHelper.auth.currentUser != null) {
            startActivity(Intent(this, ChatListActivity::class.java))
            finish()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            FirebaseHelper.auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    // update presence
                    val uid = FirebaseHelper.currentUid() ?: return@addOnSuccessListener
                    FirebaseHelper.getUserDoc(uid).set(mapOf(
                        "uid" to uid,
                        "email" to email,
                        "online" to true,
                        "lastSeen" to System.currentTimeMillis()
                    ), com.google.firebase.firestore.SetOptions.merge())
                    startActivity(Intent(this, ChatListActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Login failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
