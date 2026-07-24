package com.example.orthofinixai.data.model

data class SavedCase(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorName: String = "",
    val imagePath: String = "",
    val viewType: String = "",
    val confidenceScore: Float = 0f,
    val aboScore: Float = 0f,
    val andrewsScore: Float = 0f,
    val createdAt: Long = 0L,
    val hasReport: Boolean = false,
    val clinicalDataJson: String = "",
    val patientProfile: Patient? = null
)
