package com.example.glumedic

data class OcrResponse(
    val text: String? = null,
    val error: String? = null,
    val success: Boolean = false,
    val api: String? = null,
    val timestamp: String? = null
)