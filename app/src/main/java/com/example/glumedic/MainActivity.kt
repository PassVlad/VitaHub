package com.example.glumedic

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var etGlucose: TextInputEditText
    private lateinit var etMeal: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnDateTime: Button
    private lateinit var btnSave: Button
    private lateinit var btnShowHistory: Button
    private lateinit var tvStats: TextView

    private var selectedDateTime: String = ""
    private val measurements = mutableListOf<GlucoseMeasurement>()
    private lateinit var prefsHelper: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsHelper = SharedPreferencesHelper(this)

        initViews()
        setupClickListeners()
        selectedDateTime = getCurrentDateTime()
        updateDateTimeButton()

        // Загружаем сохраненные данные при запуске
        loadSavedMeasurements()
    }

    override fun onPause() {
        super.onPause()
        // Сохраняем данные когда приложение уходит в фон
        saveMeasurementsToPrefs()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Сохраняем данные при закрытии приложения
        saveMeasurementsToPrefs()
    }

    private fun initViews() {
        etGlucose = findViewById(R.id.etGlucose)
        etMeal = findViewById(R.id.etMeal)
        etNotes = findViewById(R.id.etNotes)
        btnDateTime = findViewById(R.id.btnDateTime)
        btnSave = findViewById(R.id.btnSave)
        btnShowHistory = findViewById(R.id.btnShowHistory)
        tvStats = findViewById(R.id.tvStats)
    }

    private fun setupClickListeners() {
        btnDateTime.setOnClickListener { showDateTimePicker() }
        btnSave.setOnClickListener { saveMeasurement() }
        btnShowHistory.setOnClickListener { showHistory() }
    }

    private fun loadSavedMeasurements() {
        val savedMeasurements = prefsHelper.getMeasurements()
        measurements.clear()
        measurements.addAll(savedMeasurements)
        updateStatistics()

        if (measurements.isNotEmpty()) {
            showToast("Загружено ${measurements.size} сохраненных измерений")
        }
    }

    private fun saveMeasurementsToPrefs() {
        prefsHelper.saveMeasurements(measurements)
    }

    private fun showDateTimePicker() {
        val currentDate = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }

                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        selectedDate.apply {
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                        }
                        selectedDateTime = formatDateTime(selectedDate)
                        updateDateTimeButton()
                    },
                    currentDate.get(Calendar.HOUR_OF_DAY),
                    currentDate.get(Calendar.MINUTE),
                    true
                ).show()
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveMeasurement() {
        val glucoseStr = etGlucose.text.toString().trim()

        if (glucoseStr.isEmpty()) {
            showToast("Введите уровень глюкозы")
            return
        }

        try {
            val glucoseLevel = glucoseStr.toDouble()
            val mealTime = etMeal.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            val measurement = GlucoseMeasurement(
                glucoseLevel = glucoseLevel,
                dateTime = selectedDateTime,
                mealTime = mealTime,
                notes = notes
            )

            measurements.add(measurement)
            saveMeasurementsToPrefs() // Сохраняем сразу после добавления
            updateStatistics()
            clearForm()
            showToast("Измерение сохранено")

        } catch (e: NumberFormatException) {
            showToast("Некорректное значение глюкозы")
        }
    }

    private fun updateStatistics() {
        if (measurements.isEmpty()) {
            tvStats.text = "Данных пока нет"
            return
        }

        val glucoseLevels = measurements.map { it.glucoseLevel }
        val average = glucoseLevels.average()
        val min = glucoseLevels.minOrNull() ?: 0.0
        val max = glucoseLevels.maxOrNull() ?: 0.0

        val stats = """
            Всего измерений: ${measurements.size}
            Средний уровень: ${"%.1f".format(average)}
            Мин: ${"%.1f".format(min)}
            Макс: ${"%.1f".format(max)}
        """.trimIndent()

        tvStats.text = stats
    }

    private fun showHistory() {
        if (measurements.isEmpty()) {
            showToast("История измерений пуста")
            return
        }

        val history = measurements.sortedByDescending { it.dateTime }
            .joinToString("\n\n") { measurement ->
                "${measurement.dateTime}: ${measurement.glucoseLevel} ммоль/л\n" +
                        "Прием пищи: ${measurement.mealTime}\n" +
                        if (measurement.notes.isNotEmpty()) "Заметки: ${measurement.notes}" else ""
            }

        AlertDialog.Builder(this)
            .setTitle("История измерений (${measurements.size})")
            .setMessage(history)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun clearForm() {
        etGlucose.text?.clear()
        etMeal.text?.clear()
        etNotes.text?.clear()
        selectedDateTime = getCurrentDateTime()
        updateDateTimeButton()
    }

    private fun getCurrentDateTime(): String {
        return formatDateTime(Calendar.getInstance())
    }

    private fun formatDateTime(calendar: Calendar): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun updateDateTimeButton() {
        btnDateTime.text = selectedDateTime
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}