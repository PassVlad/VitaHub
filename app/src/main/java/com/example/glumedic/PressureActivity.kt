package com.example.glumedic

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType.*
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

class PressureActivity : AppCompatActivity() {

    private lateinit var etSystolic: TextInputEditText
    private lateinit var etDiastolic: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnDateTime: MaterialCardView
    private lateinit var tvDateTime: TextView
    private lateinit var btnSave: com.google.android.material.button.MaterialButton
    private lateinit var btnShowHistory: com.google.android.material.button.MaterialButton
    private lateinit var toolbar: MaterialToolbar
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
        setContentView(R.layout.activity_pressure)

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
        etSystolic = findViewById(R.id.etSystolic)
        etSystolic.inputType = TYPE_CLASS_NUMBER
        etDiastolic = findViewById(R.id.etDiastolic)
        etDiastolic.inputType = TYPE_CLASS_NUMBER
        etNotes = findViewById(R.id.etNotes)
        btnDateTime = findViewById(R.id.btnDateTime)
        tvDateTime = findViewById(R.id.tvDateTime)
        btnSave = findViewById(R.id.btnSave)
        btnShowHistory = findViewById(R.id.btnShowHistory)
        toolbar = findViewById(R.id.toolbar)
        chartView = findViewById(R.id.weeklyChart)
        chartView.setValueScale(1, "мм рт. ст.")
        tvAvg = findViewById(R.id.tvAvg)
        tvMin = findViewById(R.id.tvMin)
        tvMax = findViewById(R.id.tvMax)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
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
                val listResp = ApiClient.apiService.getMeasurements("pressure", 200)
                val metric = metricsResp.body()?.firstOrNull { it.key == "pressure" }
                withContext(Dispatchers.Main) {
                    metric?.let { m ->
                        chartView.setMetricLimits(m.min, m.max)
                        chartView.setMetricLimits2(m.min2, m.max2)
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
                withContext(Dispatchers.IO) { VitalStore.syncPending() }
                val metricsResp = ApiClient.apiService.getMetrics()
                val listResp = ApiClient.apiService.getMeasurements("pressure", 200)
                val metric = metricsResp.body()?.firstOrNull { it.key == "pressure" }
                withContext(Dispatchers.Main) {
                    metric?.let { m ->
                        chartView.setMetricLimits(m.min, m.max)
                        chartView.setMetricLimits2(m.min2, m.max2)
                        chartView.setDecimals(m.decimals)
                    }
                    applyServerData(listResp.body(), 0, silent)
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
        measurements.addAll(VitalStore.cacheFor("pressure"))
        applyCacheToUi()
    }

    private fun applyCacheToUi() {
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
        tvAvg.text = values.average().toInt().toString()
        tvMin.text = values.min().toInt().toString()
        tvMax.text = values.max().toInt().toString()
    }

    private fun updateChartFromMeasurements() {
        val chartPoints = measurements
            .map {
                DataPoint(
                    timestamp = parseServerTime(it.measured_at),
                    value = it.value.toInt(),
                    value2 = it.value2?.toInt()
                )
            }
            .sortedBy { it.timestamp }
        chartView.setPoints(chartPoints)
    }

    private fun applyServerData(
        list: List<VitalMeasurement>?,
        synced: Int,
        silent: Boolean = false
    ) {
        list?.let {
            VitalStore.setCache("pressure", it)
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
        val systolicStr = etSystolic.text.toString().trim()
        val diastolicStr = etDiastolic.text.toString().trim()

        if (systolicStr.isEmpty() || diastolicStr.isEmpty()) {
            showToast("Введите оба значения давления")
            return
        }

        val systolic = systolicStr.toIntOrNull()
        val diastolic = diastolicStr.toIntOrNull()
        if (systolic == null || diastolic == null) {
            showToast("Некорректные значения давления")
            return
        }

        val notes = etNotes.text.toString().trim()

        // 1. Всегда сохраняем локально (кэш)
        VitalStore.addLocal(
            "pressure",
            systolic.toDouble(),
            diastolic.toDouble(),
            serverFormat.format(Date(selectedTimeMillis)),
            notes.ifBlank { null }
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
                val value2 = m.value2?.let { it.toInt().toString() } ?: "-"
                val note = m.notes?.takeIf { it.isNotBlank() }?.let { "\n📝 $it" } ?: ""
                "🕒 $date\n🩸 ${m.value.toInt()}/$value2 мм рт. ст.$note"
            }

        AlertDialog.Builder(this)
            .setTitle("📋 История измерений (${measurements.size})")
            .setMessage(history)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun clearForm() {
        etSystolic.text?.clear()
        etDiastolic.text?.clear()
        etNotes.text?.clear()
        selectedTimeMillis = System.currentTimeMillis()
        updateDateTimeButton()
    }

    private fun updateDateTimeButton() {
        tvDateTime.text = displayFormat.format(Date(selectedTimeMillis))
        tvDateTime.setTextColor(resources.getColor(R.color.text_primary))
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
