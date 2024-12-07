package com.example.dentalapp

data class Appointment(
    val userId: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val type: String = ""
)
