package com.example.orthofinixai.data.model

data class User(
    val uid: String,
    val email: String,
    val display_name: String? = null,
    val role: String = "doctor"
)
