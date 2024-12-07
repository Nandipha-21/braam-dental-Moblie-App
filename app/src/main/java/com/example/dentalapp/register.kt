package com.example.dentalapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class register : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnSignUp: Button
    private lateinit var tvLoginRedirect: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Bind UI elements
        etFirstName = findViewById(R.id.etfirstName)
        etLastName = findViewById(R.id.etlastName)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.etLoginPassword)
        etPhoneNumber = findViewById(R.id.et_phoneNumber)
        btnSignUp = findViewById(R.id.btn_Signup_loginPage)
        tvLoginRedirect = findViewById(R.id.tvLoginRedirect)

        // Set click listener for the Sign Up button
        btnSignUp.setOnClickListener {
            registerUser()
        }

        // Redirect to login activity if user clicks the login text
        tvLoginRedirect.setOnClickListener {
            val intent = Intent(this, login::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // User registered successfully
                    val userId = auth.currentUser?.uid
                    val userData = mapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to email,
                        "phone" to phoneNumber
                    )

                    // Save user data to Firebase Realtime Database
                    userId?.let {
                        database.child("users").child(it).setValue(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "User created successfully!", Toast.LENGTH_SHORT).show()
                                // Optionally, redirect to the main activity
                                val intent = Intent(this, login::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // Handle sign-in failure
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}