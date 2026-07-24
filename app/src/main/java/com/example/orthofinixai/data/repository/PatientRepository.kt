package com.example.orthofinixai.data.repository

import android.content.Context
import android.util.Log
import com.example.orthofinixai.data.local.OrthofinixDatabase
import com.example.orthofinixai.data.local.entity.PatientEntity
import com.example.orthofinixai.data.model.Patient
import com.example.orthofinixai.data.model.PatientCreate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PatientRepository(private val context: Context) {

    private val patientDao by lazy { OrthofinixDatabase.getInstance(context).patientDao() }

    fun getPatients(): Flow<Result<List<Patient>>> {
        val userId = AuthRepository.getCurrentUserId()
        return patientDao.getPatientsForUser(userId).map { entities ->
            Result.success(entities.map { it.toPatient() })
        }
    }

    fun createPatient(patient: PatientCreate): Flow<Result<Patient>> = flow {
        try {
            val userId = AuthRepository.getCurrentUserId()
            val id = "PT-${System.currentTimeMillis()}"
            val entity = PatientEntity(
                id = id,
                userId = userId,
                name = patient.name,
                age = estimateAge(patient.dateOfBirth),
                gender = patient.gender,
                phone = patient.phone,
                notes = patient.notes,
                createdAt = System.currentTimeMillis()
            )
            patientDao.insertPatient(entity)
            
            val createdPatient = Patient(
                id = id,
                name = patient.name,
                dateOfBirth = patient.dateOfBirth,
                gender = patient.gender,
                phone = patient.phone,
                email = patient.email,
                doctorName = patient.doctorName,
                hospital = patient.hospital,
                diagnosis = patient.diagnosis,
                treatmentDate = patient.treatmentDate,
                notes = patient.notes,
                doctorId = userId,
                createdAt = entity.createdAt
            )
            emit(Result.success(createdPatient))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create patient", e)
            emit(Result.failure(e))
        }
    }

    private fun PatientEntity.toPatient() = Patient(
        id = id,
        name = name,
        age = age,
        gender = gender,
        phone = phone,
        notes = notes,
        createdAt = createdAt
    )

    private fun estimateAge(dob: String): Int {
        return try {
            val year = dob.split("/").lastOrNull()?.toIntOrNull() ?: 2010
            (2026 - year).coerceIn(5, 80)
        } catch (e: Exception) { 25 }
    }

    companion object {
        private const val TAG = "PatientRepository"
    }
}
