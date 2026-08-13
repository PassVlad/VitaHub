package com.example.glumedic

data class OcrResponse(
    val success: Boolean = false,
    val filename: String? = null,
    val text: String? = null,
    val error: String? = null
)