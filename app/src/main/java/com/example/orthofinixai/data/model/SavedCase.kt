package com.example.orthofinixai.data.model

data class SavedCase(
    val id: String,
    val patientId: String,
    val patientName: String,
    val imagePath: String,
    val viewType: String,
    val confidenceScore: Float,
    val andrewsScore: Float,
    val createdAt: Long,
    val hasReport: Boolean
)
