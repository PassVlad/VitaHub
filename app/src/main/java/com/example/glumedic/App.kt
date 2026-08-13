package com.example.glumedic

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        VitalStore.init(this)
        ApiClient.init {
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("access_token", null)
        }
    }
}
