package com.example.orthofinixai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "patients", indices = [Index("userId")])
data class PatientEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val dateOfBirth: String = "",
    val age: Int,
    val gender: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
