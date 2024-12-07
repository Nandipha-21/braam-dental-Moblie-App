package com.example.dentalapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class viewAppointments : AppCompatActivity() {

    private lateinit var msgTextView: TextView
    private lateinit var greetTextView: TextView
    private lateinit var signOutButton: Button
    private lateinit var appointmentsLayout: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_appointments)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize UI elements
        msgTextView = findViewById(R.id.msgTextView)
        greetTextView = findViewById(R.id.greetTextView)
        signOutButton = findViewById(R.id.signOutButton)
        appointmentsLayout = findViewById(R.id.appointmentsLayout)

        // Check user credentials and fetch appointments
        checkUserCredentials()
        fetchUpcomingAppointments()

        // Set sign-out button action
        signOutButton.setOnClickListener {
            signOut()
        }
    }

    private fun checkUserCredentials() {
        val user = auth.currentUser
        if (user == null) {
            // Redirect to login if not logged in
            startActivity(Intent(this, login::class.java))
            finish()
        } else {
            greetTextView.text = "Welcome ✨"
        }
    }

    private fun fetchUpcomingAppointments() {
        val userId = auth.currentUser?.uid
        val bookingsRef = database.child("bookings")
        val bookingQuery: Query = bookingsRef.orderByChild("userId").equalTo(userId) // Use Query here

        bookingQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                appointmentsLayout.removeAllViews() // Clear previous entries

                if (snapshot.exists()) {
                    for (childSnapshot in snapshot.children) {
                        val booking = childSnapshot.getValue(Appointment::class.java)
                        if (booking != null) {
                            // Create TextView for each appointment
                            val appointmentView = TextView(this@viewAppointments).apply {
                                text = "Date: ${booking.date}, Start: ${booking.startTime}, End: ${booking.endTime}, Type: ${booking.type}"
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                setPadding(8, 8, 8, 8)
                            }
                            appointmentsLayout.addView(appointmentView)
                        }
                    }
                } else {
                    val noAppointmentsView = TextView(this@viewAppointments).apply {
                        text = "No upcoming appointments"
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(8, 8, 8, 8)
                    }
                    appointmentsLayout.addView(noAppointmentsView)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@viewAppointments, "Error fetching appointments: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun signOut() {
        auth.signOut()
        startActivity(Intent(this, login::class.java))
        finish()
    }
}


