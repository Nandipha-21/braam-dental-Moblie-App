package com.example.dentalapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.database.MatrixCursor
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.SimpleCursorAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class bookAppointment : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var tvDatePicker: TextView
    private lateinit var tvTimePicker: TextView
    private lateinit var spinnerAppointmentType: Spinner
    private lateinit var btnBook: Button
    private lateinit var tvBookingStatus: TextView
    private lateinit var back: ImageView // Back button


    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedAppointmentType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_appointment)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        tvDatePicker = findViewById(R.id.tvDatePicker)
        tvTimePicker = findViewById(R.id.tvTimePicker)
        spinnerAppointmentType = findViewById(R.id.spinnerAppointmentType)
        btnBook = findViewById(R.id.btnBook)
        tvBookingStatus = findViewById(R.id.tvBookingStatus)
        back = findViewById(R.id.back)

        val btnDatePicker: Button = findViewById(R.id.btnDatePicker)
        val btnTimePicker: Button = findViewById(R.id.btnTimePicker)

        btnDatePicker.setOnClickListener { showDatePickerDialog() }
        btnTimePicker.setOnClickListener { showTimePickerDialog() }
        setupAppointmentTypeSpinner()
        btnBook.setOnClickListener { bookAppointment() }

        back.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close the current activity
        }

    }

    private fun setupAppointmentTypeSpinner() {
        data class AppointmentType(val displayText: String, val value: String)

        val appointmentTypes = listOf(
            AppointmentType("Teeth Cleaning (30 min)", "Cleaning"),
            AppointmentType("Filling (45 min)", "Filling"),
            AppointmentType("Check-up (20 min)", "Checkup")
        )

        val cursor = MatrixCursor(arrayOf("_id", "displayText", "value"))
        appointmentTypes.forEachIndexed { index, appointmentType ->
            cursor.addRow(arrayOf(index, appointmentType.displayText, appointmentType.value))
        }

        val adapter = SimpleCursorAdapter(
            this,
            android.R.layout.simple_spinner_item,
            cursor,
            arrayOf("displayText"),
            intArrayOf(android.R.id.text1),
            0
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAppointmentType.adapter = adapter

        spinnerAppointmentType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View, position: Int, id: Long) {
                cursor.moveToPosition(position)
                selectedAppointmentType = cursor.getString(cursor.getColumnIndex("value"))
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedAppointmentType = ""
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            tvDatePicker.text = "Selected Date: $selectedDate"
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            tvTimePicker.text = "Selected Time: $selectedTime"
        }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun bookAppointment() {
        if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty() && selectedAppointmentType.isNotEmpty()) {
            val userId = auth.currentUser?.uid

            if (userId != null) {
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(selectedDate)
                val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                val dayOfWeek = Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_WEEK)

                if (isValidBookingTime(dayOfWeek, selectedTime)) {
                    val duration = getAppointmentDuration(selectedAppointmentType)
                    val endTime = calculateEndTime(selectedTime, duration)

                    val bookingDetails = Appointment(
                        userId = userId,
                        date = formattedDate,
                        startTime = selectedTime,
                        endTime = endTime,
                        type = selectedAppointmentType
                    )

                    checkForConflictsAndBook(bookingDetails)
                } else {
                    Toast.makeText(this, "Invalid booking time. Please select a time between 8 AM to 12 PM or 1 PM to 5 PM.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "User not authenticated.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Please select a date, time, and appointment type.", Toast.LENGTH_LONG).show()
        }
    }

    private fun isValidBookingTime(day: Int, time: String): Boolean {
        val (hour, _) = time.split(":").map { it.toInt() }
        return when (day) {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY -> {
                (hour in 8..11) || (hour in 13..16)
            }
            else -> false
        }
    }

    private fun getAppointmentDuration(appointmentType: String): Int {
        return when (appointmentType) {
            "Cleaning" -> 30
            "Filling" -> 45
            "Checkup" -> 20
            else -> 0
        }
    }

    private fun calculateEndTime(startTime: String, duration: Int): String {
        val (hour, minute) = startTime.split(":").map { it.toInt() }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            add(Calendar.MINUTE, duration)
        }
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
    }

    private fun checkForConflictsAndBook(bookingDetails: Appointment) {
        val bookingsRef = database.child("bookings")
        val dateRef = bookingsRef.orderByChild("date").equalTo(bookingDetails.date)

        dateRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var slotTaken = false

                for (childSnapshot in snapshot.children) {
                    val booking = childSnapshot.getValue(Appointment::class.java)

                    if (booking != null &&
                        (bookingDetails.startTime >= booking.startTime && bookingDetails.startTime < booking.endTime ||
                                bookingDetails.endTime > booking.startTime && bookingDetails.endTime <= booking.endTime)
                    ) {
                        slotTaken = true
                        break
                    }
                }

                if (slotTaken) {
                    Toast.makeText(this@bookAppointment, "This time slot is already booked. Please choose another time.", Toast.LENGTH_LONG).show()
                } else {
                    val bookingId = "${bookingDetails.date}_${bookingDetails.startTime}"
                    bookingsRef.child(bookingId).setValue(bookingDetails)
                        .addOnSuccessListener {
                            Toast.makeText(this@bookAppointment, "Appointment booked successfully!", Toast.LENGTH_LONG).show()
                            clearFields() // Clear fields after booking
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@bookAppointment, "Failed to book appointment. Try again.", Toast.LENGTH_LONG).show()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DatabaseError", "Error checking for booking conflicts", error.toException())
            }
        })
    }

    private fun clearFields() {
        tvDatePicker.text = "Selected Date:"
        tvTimePicker.text = "Selected Time:"
        selectedDate = ""
        selectedTime = ""
        selectedAppointmentType = ""
        spinnerAppointmentType.setSelection(0) // Reset spinner to the first item
    }
}
