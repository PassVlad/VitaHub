package com.example.glumedic

import java.io.Serializable

data class GlucoseMeasurement(
    val id: String = System.currentTimeMillis().toString(),
    val glucoseLevel: Double,
    val dateTime: String,
    val mealTime: String,
    val notes: String = ""
) : Serializable