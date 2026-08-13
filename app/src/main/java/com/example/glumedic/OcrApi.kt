package com.example.glumedic

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface OcrApi {
    @Multipart
    @POST("api/ocr-fast/")
    fun recognizeImage(@Part image: MultipartBody.Part): Call<OcrResponse>
}