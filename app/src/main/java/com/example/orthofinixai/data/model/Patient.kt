package com.example.orthofinixai.data.model

data class Patient(
    val id: String? = null,
    val name: String,
    val date_of_birth: String,
    val gender: String,
    val contact_info: String? = null,
    val doctor_id: String? = null,
    val created_at: String? = null
)

data class PatientCreate(
    val name: String,
    val date_of_birth: String,
    val gender: String,
    val contact_info: String? = null
)
