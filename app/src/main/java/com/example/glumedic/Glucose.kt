package com.example.glumedic

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType.*
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.glumedic.ui.ChartView
import com.example.glumedic.ui.DataPoint
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class Glucose : AppCompatActivity() {

    private lateinit var etGlucose: TextInputEditText
    private lateinit var etMeal: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnDateTime: MaterialCardView
    private lateinit var tvDateTime: TextView
    private lateinit var btnSave: com.google.android.material.button.MaterialButton
    private lateinit var btnShowHistory: com.google.android.material.button.MaterialButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvMeasurementsCount: TextView
    private lateinit var chartView: ChartView
    private lateinit var tvAvg: TextView
    private lateinit var tvMin: TextView
    private lateinit var tvMax: TextView

    private var selectedTimeMillis: Long = System.currentTimeMillis()
    private val measurements = mutableListOf<VitalMeasurement>()
    private var autoRefreshJob: Job? = null
    private var isLoading = false

    private val token: String?
        get() = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("access_token", null)

    private val displayFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val serverFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glucose)

        initViews()
        setupToolbar()
        setupClickListeners()
        updateDateTimeButton()

        loadData()
    }

    override fun onResume() {
        super.onResume()
        startAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val currentFocus = currentFocus
            if (currentFocus is EditText) {
                val rect = Rect()
                currentFocus.getGlobalVisibleRect(rect)
                if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    hideKeyboard(currentFocus)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    private fun initViews() {
        etGlucose = findViewById(R.id.etGlucose)
        etGlucose.inputType = TYPE_NUMBER_FLAG_DECIMAL or TYPE_CLASS_NUMBER
        etGlucose.filters = arrayOf(DecimalDigitsInputFilter(2, 1))
        etMeal = findViewById(R.id.etMeal)
        etNotes = findViewById(R.id.etNotes)
        btnDateTime = findViewById(R.id.btnDateTime)
        tvDateTime = findViewById(R.id.tvDateTime)
        btnSave = findViewById(R.id.btnSave)
        btnShowHistory = findViewById(R.id.btnShowHistory)
        toolbar = findViewById(R.id.toolbar)
        tvMeasurementsCount = findViewById(R.id.tvMeasurementsCount)
        chartView = findViewById(R.id.weeklyChart)
        chartView.setValueScale(10, "ммоль/л")
        tvAvg = findViewById(R.id.tvAvg)
        tvMin = findViewById(R.id.tvMin)
        tvMax = findViewById(R.id.tvMax)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openGalleryActivity() {
        val intent = Intent(this, GalleryActivity::class.java)
        startActivity(intent)
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("О приложении")
            .setMessage("VitaHub\nВерсия 1.0\n\nПриложение для отслеживания жизненных показателей.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupClickListeners() {
        btnDateTime.setOnClickListener { showDateTimePicker() }
        btnSave.setOnClickListener { saveMeasurement() }
        btnShowHistory.setOnClickListener { showHistory() }
    }

    // ---------- Данные (кэш + сервер) ----------

    private fun loadData() {
        if (isLoading) return
        isLoading = true
        refreshFromCache()
        if (!VitalStore.isOnline()) {
            isLoading = false
            showToast(if (measurements.isEmpty())
                "Оффлайн: данных нет"
            else
                "Оффлайн: показаны данные из кэша (${measurements.size})")
            return
        }
        val currentToken = token
        if (currentToken.isNullOrEmpty()) {
            isLoading = false
            showToast("Требуется авторизация")
            return
        }
        lifecycleScope.launch {
            try {
                val synced = withContext(Dispatchers.IO) { VitalStore.syncPending() }
                val metricsResp = ApiClient.apiService.getMetrics()
                val listResp = ApiClient.apiService.getMeasurements("glucose", 200)
                val metric = metricsResp.body()?.firstOrNull { it.key == "glucose" }
                withContext(Dispatchers.Main) {
                    metric?.let { m ->
                        chartView.setMetricLimits(m.min, m.max)
                        chartView.setDecimals(m.decimals)
                    }
                    applyServerData(listResp.body(), synced)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Ошибка загрузки: ${e.message}")
                }
            } finally {
                isLoading = false
            }
        }
    }

    private fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(20_000)
                loadData(silent = true)
            }
        }
    }

    private fun loadData(silent: Boolean) {
        if (isLoading) return
        isLoading = true
        refreshFromCache()
        if (!VitalStore.isOnline() || token.isNullOrEmpty()) {
            isLoading = false
            return
        }
        lifecycleScope.launch {
            try {
                val synced = withContext(Dispatchers.IO) { VitalStore.syncPending() }
                val metricsResp = ApiClient.apiService.getMetrics()
                val listResp = ApiClient.apiService.getMeasurements("glucose", 200)
                val metric = metricsResp.body()?.firstOrNull { it.key == "glucose" }
                withContext(Dispatchers.Main) {
                    metric?.let { m ->
                        chartView.setMetricLimits(m.min, m.max)
                        chartView.setDecimals(m.decimals)
                    }
                    applyServerData(listResp.body(), synced, silent)
                }
            } catch (e: Exception) {
                if (!silent) {
                    withContext(Dispatchers.Main) {
                        showToast("Ошибка загрузки: ${e.message}")
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    private fun refreshFromCache() {
        measurements.clear()
        measurements.addAll(VitalStore.cacheFor("glucose"))
        applyCacheToUi()
    }

    private fun applyCacheToUi() {
        tvMeasurementsCount.text = measurements.size.toString()
        tvMeasurementsCount.visibility =
            if (measurements.isNotEmpty()) TextView.VISIBLE else TextView.GONE
        updateStats()
        updateChartFromMeasurements()
    }

    private fun updateStats() {
        if (measurements.isEmpty()) {
            tvAvg.text = "—"
            tvMin.text = "—"
            tvMax.text = "—"
            return
        }
        val values = measurements.map { it.value }
        tvAvg.text = String.format(Locale.getDefault(), "%.1f", values.average())
        tvMin.text = String.format(Locale.getDefault(), "%.1f", values.min())
        tvMax.text = String.format(Locale.getDefault(), "%.1f", values.max())
    }

    private fun updateChartFromMeasurements() {
        val chartPoints = measurements
            .map { DataPoint(parseServerTime(it.measured_at), (it.value * 10).toInt()) }
            .sortedBy { it.timestamp }
        chartView.setPoints(chartPoints)
    }

    private fun applyServerData(
        list: List<VitalMeasurement>?,
        synced: Int,
        silent: Boolean = false
    ) {
        list?.let {
            VitalStore.setCache("glucose", it)
            refreshFromCache()
        }
        if (silent) return
        val msg = if (synced > 0)
            "✓ Синхронизировано измерений: $synced"
        else
            "Загружено ${measurements.size} измерений"
        showToast(msg)
    }

    private fun saveMeasurement() {
        val glucoseStr = etGlucose.text.toString().trim()

        if (glucoseStr.isEmpty()) {
            showToast("Введите уровень глюкозы")
            return
        }

        val value = glucoseStr.toDoubleOrNull()
        if (value == null) {
            showToast("Некорректное значение глюкозы")
            return
        }

        val meal = etMeal.text.toString().trim()
        val notes = etNotes.text.toString().trim()
        val fullNotes = listOfNotNull(meal.ifBlank { null }, notes.ifBlank { null })
            .joinToString(" | ")

        // 1. Всегда сохраняем локально (кэш)
        VitalStore.addLocal(
            "glucose",
            value,
            null,
            serverFormat.format(Date(selectedTimeMillis)),
            fullNotes.ifBlank { null }
        )
        clearForm()
        refreshFromCache()

        // 2. На сервер — только если есть сеть
        if (VitalStore.isOnline()) {
            lifecycleScope.launch {
                val synced = withContext(Dispatchers.IO) { VitalStore.syncPending() }
                withContext(Dispatchers.Main) {
                    if (synced > 0) {
                        showToast("✓ Измерение сохранено и синхронизировано")
                        refreshFromCache()
                    } else {
                        showToast("Сохранено в кэш")
                    }
                }
            }
        } else {
            showToast("💾 Измерение сохранено в кэш (оффлайн)")
        }
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
                        selectedTimeMillis = selectedDate.timeInMillis
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

    private fun showHistory() {
        if (measurements.isEmpty()) {
            showToast("История измерений пуста")
            return
        }

        val history = measurements.sortedByDescending { it.measured_at }
            .joinToString("\n\n") { m ->
                val date = formatServerTime(m.measured_at)
                val note = m.notes?.takeIf { it.isNotBlank() }?.let { "\n📝 $it" } ?: ""
                "🕒 $date\n🩸 ${formatValue(m.value)} ммоль/л$note"
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
        selectedTimeMillis = System.currentTimeMillis()
        updateDateTimeButton()
    }

    private fun updateDateTimeButton() {
        tvDateTime.text = displayFormat.format(Date(selectedTimeMillis))
        tvDateTime.setTextColor(resources.getColor(R.color.text_primary))
    }

    private fun formatValue(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", value)
        }
    }

    private fun parseServerTime(raw: String): Long {
        return try {
            serverFormat.parse(raw.replace("Z", ""))?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun formatServerTime(raw: String): String {
        return try {
            val parsed = serverFormat.parse(raw.replace("Z", "")) ?: return raw
            displayFormat.format(parsed)
        } catch (e: Exception) {
            raw
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
