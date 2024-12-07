package com.example.dentalapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    // Variables for Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var welcomeMessage: TextView
    lateinit var btnBook: Button
    lateinit var btnViewAppointment: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize welcomeMessage TextView
        welcomeMessage = findViewById(R.id.greetTextView)

        btnBook=findViewById(R.id.btnBookAppointment)
        btnBook.setOnClickListener {
            val intent = Intent(this, bookAppointment::class.java)
            startActivity(intent)
        }
        btnViewAppointment=findViewById(R.id.btnViewAppointment)
        btnViewAppointment.setOnClickListener {
            val intent = Intent(this, viewAppointments::class.java)
            startActivity(intent)
        }

        checkCred()
    }


    fun checkCred() {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // If user is not logged in, redirect them to the login activity
            val intent = Intent(this, login::class.java)
            startActivity(intent)
            finish()
        } else {
            // Fetch user data from Firebase Realtime Database
            val userId = currentUser.uid
            database = FirebaseDatabase.getInstance().getReference("users/$userId")

            database.get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    // Fetch data with correct field names
                    val firstName = dataSnapshot.child("firstName").value?.toString() ?: "User"
                    val lastName = dataSnapshot.child("lastName").value?.toString() ?: ""

                    Log.d("MainActivity", "First Name: $firstName, Last Name: $lastName")

                    val greetingMessage = if (lastName.isNotEmpty()) {
                        " Hey, $firstName $lastName!✨"
                    } else {
                        " Hello, $firstName! ✨"
                    }
                    welcomeMessage.text = greetingMessage
                } else {
                    Log.e("MainActivity", "User data does not exist in the database.")
                    Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Log.e("MainActivity", "Error retrieving user data", exception)
                Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}