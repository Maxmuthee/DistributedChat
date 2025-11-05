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

        // Check if user is already logged in
        if (FirebaseHelper.auth.currentUser != null) {
            navigateToChatList()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Add click listener for forgot password if you added that TextView
        // binding.tvForgotPassword.setOnClickListener {
        //     showForgotPasswordDialog()
        // }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (!validateInputs(email, password)) {
            return
        }

        loginUser(email, password)
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            binding.etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            binding.etPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun loginUser(email: String, password: String) {
        // Show loading state
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Signing in..."

        FirebaseHelper.auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                updateUserPresence(email)
                navigateToChatList()
            }
            .addOnFailureListener { e ->
                handleLoginFailure(e)
            }
    }

    private fun updateUserPresence(email: String) {
        val uid = FirebaseHelper.currentUid() ?: return
        FirebaseHelper.getUserDoc(uid).set(
            mapOf(
                "uid" to uid,
                "email" to email,
                "online" to true,
                "lastSeen" to System.currentTimeMillis()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )
    }

    private fun navigateToChatList() {
        startActivity(Intent(this, ChatListActivity::class.java))
        finish()
    }

    private fun handleLoginFailure(exception: Exception) {
        // Reset button state
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = "Login"

        val errorMessage = when {
            exception.localizedMessage?.contains("invalid credential") == true ->
                "Invalid email or password"
            exception.localizedMessage?.contains("network error") == true ->
                "Network error. Please check your connection"
            exception.localizedMessage?.contains("user not found") == true ->
                "No account found with this email"
            else -> "Login failed: ${exception.localizedMessage}"
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    // Optional: Handle back press to exit app
    private var backPressedTime = 0L
    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            finishAffinity()
        } else {
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
        backPressedTime = System.currentTimeMillis()
    }
}