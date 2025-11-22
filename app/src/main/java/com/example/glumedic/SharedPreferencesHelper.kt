package com.example.glumedic

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharedPreferencesHelper(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("glucose_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveMeasurements(measurements: List<GlucoseMeasurement>) {
        val json = gson.toJson(measurements)
        sharedPreferences.edit().putString("measurements", json).apply()
    }

    fun getMeasurements(): List<GlucoseMeasurement> {
        val json = sharedPreferences.getString("measurements", null)
        return if (json != null) {
            val type = object : TypeToken<List<GlucoseMeasurement>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }
}