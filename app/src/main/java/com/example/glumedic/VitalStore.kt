package com.example.glumedic

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.Gson
import java.util.UUID

data class LocalMeasurement(
    val local_id: String,
    val metric: String,
    val value: Double,
    val value2: Double? = null,
    val measured_at: String,
    val notes: String? = null,
    val added_at: Long = 0L
)

fun LocalMeasurement.toVital(): VitalMeasurement = VitalMeasurement(
    id = -1,
    metric = metric,
    value = value,
    value2 = value2,
    unit = "",
    measured_at = measured_at,
    notes = notes
)

/**
 * Локальный кэш измерений (оффлайн-first).
 * Все измерения всегда сохраняются на устройстве;
 * на сервер отправляются только при наличии сети (см. [syncPending]).
 */
object VitalStore {
    private const val PREFS = "vital_store"
    private val gson = Gson()
    private lateinit var ctx: Context

    fun init(context: Context) {
        ctx = context.applicationContext
    }

    private fun prefs() = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isOnline(): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return try {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            cm.activeNetworkInfo?.isConnectedOrConnecting ?: false
        }
    }

    private fun pendingKey(metric: String) = "pending_$metric"
    private fun cacheKey(metric: String) = "cache_$metric"

    @Synchronized
    fun pendingFor(metric: String): MutableList<LocalMeasurement> {
        val json = prefs().getString(pendingKey(metric), null) ?: return mutableListOf()
        return try {
            gson.fromJson(json, Array<LocalMeasurement>::class.java).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    @Synchronized
    fun addLocal(
        metric: String,
        value: Double,
        value2: Double?,
        measuredAt: String,
        notes: String?
    ): LocalMeasurement {
        val item = LocalMeasurement(
            local_id = UUID.randomUUID().toString(),
            metric = metric,
            value = value,
            value2 = value2,
            measured_at = measuredAt,
            notes = notes,
            added_at = System.currentTimeMillis()
        )
        val list = pendingFor(metric)
        list.add(item)
        prefs().edit().putString(pendingKey(metric), gson.toJson(list)).apply()
        return item
    }

    @Synchronized
    fun removePending(metric: String, localId: String) {
        val list = pendingFor(metric)
        list.removeAll { it.local_id == localId }
        prefs().edit().putString(pendingKey(metric), gson.toJson(list)).apply()
    }

    @Synchronized
    fun setCache(metric: String, list: List<VitalMeasurement>) {
        prefs().edit().putString(cacheKey(metric), gson.toJson(list)).apply()
    }

    /** Полный список измерений: серверный кэш + несинхронизированные локальные. */
    @Synchronized
    fun cacheFor(metric: String): List<VitalMeasurement> {
        val cached: List<VitalMeasurement> = prefs().getString(cacheKey(metric), null)?.let {
            try {
                gson.fromJson(it, Array<VitalMeasurement>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
        val all = cached + pendingFor(metric).map { it.toVital() }
        val seen = HashSet<String>()
        val dedup = mutableListOf<VitalMeasurement>()
        for (m in all.sortedBy { it.measured_at }) {
            val key = "${m.id}:${m.measured_at}"
            if (seen.add(key)) dedup.add(m)
        }
        return dedup
    }

    @Synchronized
    fun clearAll() {
        prefs().edit().clear().apply()
    }

    /** Отправка накопленных локальных измерений на сервер. */
    suspend fun syncPending(): Int {
        var synced = 0
        for (metric in listOf("glucose", "pressure")) {
            for (item in pendingFor(metric).toList()) {
                try {
                    val resp = ApiClient.apiService.addMeasurement(
                        AddMeasurementRequest(item.metric, item.value, item.value2, item.measured_at, item.notes)
                    )
                    if (resp.isSuccessful) {
                        removePending(metric, item.local_id)
                        synced++
                    }
                } catch (e: Exception) {
                    // сеть недоступна — оставляем в очереди до следующей попытки
                }
            }
        }
        refreshCacheFromServer()
        return synced
    }

    private suspend fun refreshCacheFromServer() {
        for (metric in listOf("glucose", "pressure")) {
            try {
                val resp = ApiClient.apiService.getMeasurements(metric, 200)
                if (resp.isSuccessful) resp.body()?.let { setCache(metric, it) }
            } catch (e: Exception) {
            }
        }
    }
}
