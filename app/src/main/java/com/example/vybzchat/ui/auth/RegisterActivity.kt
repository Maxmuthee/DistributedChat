package com.example.vybzchat.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vybzchat.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    // References to Realtime Database and Firestore
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            attemptRegistration()
        }

        // Add click listener for login link - MAKE SURE THIS IS UNCOMMENTED
        binding.tvLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun attemptRegistration() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (!validateInputs(username, email, password)) {
            return
        }

        registerUser(username, email, password)
    }

    private fun validateInputs(username: String, email: String, password: String): Boolean {
        if (username.isEmpty()) {
            binding.etUsername.error = "Username is required"
            binding.etUsername.requestFocus()
            return false
        }

        if (username.length < 3) {
            binding.etUsername.error = "Username must be at least 3 characters"
            binding.etUsername.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            binding.etEmail.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email address"
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

    private fun registerUser(username: String, email: String, password: String) {
        // Show loading state
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Creating Account..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: run {
                        handleRegistrationFailure(Exception("User ID is null"))
                        return@addOnCompleteListener
                    }
                    saveUserToDatabases(uid, username, email)
                } else {
                    handleRegistrationFailure(task.exception ?: Exception("Unknown error"))
                }
            }
    }

    private fun saveUserToDatabases(uid: String, username: String, email: String) {
        val userData = mapOf(
            "uid" to uid,
            "username" to username,
            "email" to email,
            "online" to true,
            "lastSeen" to System.currentTimeMillis()
        )

        // Save to Realtime Database
        dbRef.child(uid).setValue(userData)
            .addOnSuccessListener {
                // Save to Firestore
                firestore.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        handleRegistrationSuccess()
                    }
                    .addOnFailureListener { firestoreError ->
                        handleDatabaseError("Firestore", firestoreError)
                    }
            }
            .addOnFailureListener { realtimeError ->
                handleDatabaseError("Realtime Database", realtimeError)
            }
    }

    private fun handleRegistrationSuccess() {
        // Reset button state
        binding.btnRegister.isEnabled = true
        binding.btnRegister.text = "Create Account"

        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    private fun handleRegistrationFailure(exception: Exception) {
        // Reset button state
        binding.btnRegister.isEnabled = true
        binding.btnRegister.text = "Create Account"

        val errorMessage = when {
            exception.localizedMessage?.contains("email already in use", ignoreCase = true) == true ->
                "Email is already registered"
            exception.localizedMessage?.contains("invalid email", ignoreCase = true) == true ->
                "Invalid email format"
            exception.localizedMessage?.contains("network error", ignoreCase = true) == true ->
                "Network error. Please check your connection"
            exception.localizedMessage?.contains("weak password", ignoreCase = true) == true ->
                "Password is too weak"
            else -> "Registration failed: ${exception.localizedMessage}"
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun handleDatabaseError(databaseType: String, error: Exception) {
        // Even if one database fails, we still consider registration successful
        // but notify the user about the partial failure
        Toast.makeText(
            this,
            "Registration complete but $databaseType save failed: ${error.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()

        // Still navigate to login since auth was successful
        navigateToLogin()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}