package com.example.glumedic

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var etGlucose: TextInputEditText
    private lateinit var etMeal: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnDateTime: MaterialCardView
    private lateinit var tvDateTime: TextView
    private lateinit var btnSave: com.google.android.material.button.MaterialButton
    private lateinit var btnShowHistory: com.google.android.material.button.MaterialButton
    private lateinit var tvStats: TextView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvMeasurementsCount: TextView
//    private lateinit var fragBtnTest: com.google.android.material.button.MaterialButton

    private var selectedDateTime: String = ""
    private val measurements = mutableListOf<GlucoseMeasurement>()
    private lateinit var prefsHelper: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsHelper = SharedPreferencesHelper(this)

        initViews()
        setupToolbar()
        setupClickListeners()
        selectedDateTime = getCurrentDateTime()
        updateDateTimeButton()

        loadSavedMeasurements()
    }

    private fun initViews() {
        etGlucose = findViewById(R.id.etGlucose)
        etMeal = findViewById(R.id.etMeal)
        etNotes = findViewById(R.id.etNotes)
        btnDateTime = findViewById(R.id.btnDateTime)
        tvDateTime = findViewById(R.id.tvDateTime)
        btnSave = findViewById(R.id.btnSave)
        btnShowHistory = findViewById(R.id.btnShowHistory)
        tvStats = findViewById(R.id.tvStats)
        toolbar = findViewById(R.id.toolbar)
        tvMeasurementsCount = findViewById(R.id.tvMeasurementsCount)
//        fragBtnTest=findViewById(R.id.btnFrag);
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        // Обработчик клика по иконке навигации
        toolbar.setNavigationOnClickListener {
            showToast("Глюкоза Трекер")
        }
    }

    // Создание меню в Toolbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // Обработка кликов по пунктам меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_camera -> {
                openCameraActivity()
                true
            }
            R.id.action_history -> {
                showHistory()
                true
            }
            R.id.action_documents -> {
                openGalleryActivity()
                true
            }
            R.id.action_settings -> {
                showToast("Настройки скоро будут доступны")
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            R.id.action_digitize -> {
                val intent = Intent(this, DocumentDigitizerActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openGalleryActivity() {
        val intent = Intent(this, GalleryActivity::class.java)
        startActivity(intent)
    }

    // Добавьте метод для открытия активности камеры
    private fun openCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("О приложении")
            .setMessage("Глюкоза Трекер\nВерсия 1.0\n\nПриложение для отслеживания уровня глюкозы в крови.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateMeasurementsCounter() {
        if (measurements.isNotEmpty()) {
            tvMeasurementsCount.text = measurements.size.toString()
            tvMeasurementsCount.visibility = TextView.VISIBLE
        } else {
            tvMeasurementsCount.visibility = TextView.GONE
        }
    }

    private fun fragTest(){
        val intent = Intent(this, MainAct::class.java)
        startActivity(intent)
    }

    private fun setupClickListeners() {
        btnDateTime.setOnClickListener { showDateTimePicker() }
        btnSave.setOnClickListener { saveMeasurement() }
        btnShowHistory.setOnClickListener { showHistory() }
//        fragBtnTest.setOnClickListener { fragTest() }
    }

    private fun loadSavedMeasurements() {
        val savedMeasurements = prefsHelper.getMeasurements()
        measurements.clear()
        measurements.addAll(savedMeasurements)
        updateStatistics()
        updateMeasurementsCounter() // Обновляем счетчик

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
            saveMeasurementsToPrefs()
            updateStatistics()
            updateMeasurementsCounter() // Обновляем счетчик
            clearForm()
            showToast("✓ Измерение сохранено")

        } catch (e: NumberFormatException) {
            showToast("Некорректное значение глюкозы")
        }
    }

    private fun updateStatistics() {
        if (measurements.isEmpty()) {
            tvStats.text = "Данных пока нет\nДобавьте первое измерение"
            return
        }

        val glucoseLevels = measurements.map { it.glucoseLevel }
        val average = glucoseLevels.average()
        val min = glucoseLevels.minOrNull() ?: 0.0
        val max = glucoseLevels.maxOrNull() ?: 0.0

        val stats = """
            📊 Всего измерений: ${measurements.size}
            📈 Средний уровень: ${"%.1f".format(average)} ммоль/л
            📉 Минимальный: ${"%.1f".format(min)} ммоль/л
            📈 Максимальный: ${"%.1f".format(max)} ммоль/л
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
                "🕒 ${measurement.dateTime}\n" +
                        "🩸 ${measurement.glucoseLevel} ммоль/л\n" +
                        "🍽 Прием пищи: ${measurement.mealTime}\n" +
                        if (measurement.notes.isNotEmpty()) "📝 Заметки: ${measurement.notes}" else ""
            }

        AlertDialog.Builder(this)
            .setTitle("📋 История измерений (${measurements.size})")
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
        tvDateTime.text = selectedDateTime
        tvDateTime.setTextColor(resources.getColor(R.color.text_primary))
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        saveMeasurementsToPrefs()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveMeasurementsToPrefs()
    }
}