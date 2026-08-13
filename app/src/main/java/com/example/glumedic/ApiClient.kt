package com.example.glumedic

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val DJANGO_BASE_URL = "http://10.128.79.95:8000/"

    private var tokenProvider: (() -> String?)? = null

    fun init(tokenProvider: () -> String?) {
        this.tokenProvider = tokenProvider
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val token = tokenProvider?.invoke()
        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)  // Для AI ответов
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DJANGO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // API для чата с AI (история)
    val chatAiApi: ChatAiApi by lazy {
        Retrofit.Builder()
            .baseUrl(DJANGO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatAiApi::class.java)
    }

    // API для OCR
    val ocrApi: OcrApi by lazy {
        Retrofit.Builder()
            .baseUrl(DJANGO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OcrApi::class.java)
    }

    fun getBaseUrl(): String = DJANGO_BASE_URL
}
