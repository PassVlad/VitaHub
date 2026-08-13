package com.example.glumedic

data class ProfileResponse(
    val id: Int,
    val username: String,
    val email: String,
    val full_name: String? = null,
    val created_at: String? = null
)
