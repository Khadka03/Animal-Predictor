package com.example.animalpredictor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            registerUser()
        }

        findViewById<TextView>(R.id.loginPromptTextView).setOnClickListener {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun registerUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        // Input validation with detailed error logging
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            Log.e("RegisterActivity", "Email field is empty")
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            Log.e("RegisterActivity", "Password field is empty")
            return
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.error = "Confirm Password is required"
            confirmPasswordEditText.requestFocus()
            Log.e("RegisterActivity", "Confirm Password field is empty")
            return
        }

        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            confirmPasswordEditText.requestFocus()
            Log.e("RegisterActivity", "Passwords do not match")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    Log.e("RegisterActivity", "Registration failed: $errorMessage")
                    Toast.makeText(this, "Registration failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("RegisterActivity", "Registration failed: ${exception.message}")
                Toast.makeText(this, "Registration failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}
