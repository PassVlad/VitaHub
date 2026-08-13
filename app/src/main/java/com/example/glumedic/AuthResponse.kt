package com.example.glumedic

data class AuthResponse(
    val access_token: String,
    val token_type: String = "bearer"
)
