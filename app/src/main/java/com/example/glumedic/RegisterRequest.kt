package com.example.glumedic

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val full_name: String? = null
)
