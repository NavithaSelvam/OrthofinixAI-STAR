package com.example.orthofinixai.data.repository

import android.content.Context
import com.example.orthofinixai.data.SessionManager
import com.example.orthofinixai.data.local.OrthofinixDatabase
import com.example.orthofinixai.data.local.entity.PatientEntity
import com.example.orthofinixai.data.model.Patient
import com.example.orthofinixai.data.model.PatientCreate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class PatientRepository(private val context: Context) {

    private val patientDao = OrthofinixDatabase.getInstance(context).patientDao()
    private fun userId() = SessionManager.requireUserId()

    fun getPatients(): Flow<List<Patient>> =
        patientDao.getPatientsForUser(userId()).map { list -> list.map { it.toPatient() } }

    fun createPatient(patient: PatientCreate): Flow<Patient> = flow {
        val created = createPatientInternal(patient)
        emit(created)
    }

    private suspend fun createPatientInternal(patient: PatientCreate): Patient {
        val id = "OF-${System.currentTimeMillis()}"
        val entity = PatientEntity(
            id = id,
            userId = userId(),
            name = patient.name,
            dateOfBirth = patient.date_of_birth,
            age = estimateAge(patient.date_of_birth),
            gender = patient.gender,
            phone = patient.contact_info.orEmpty()
        )
        patientDao.insertPatient(entity)
        return entity.toPatient()
    }

    private fun PatientEntity.toPatient() = Patient(
        id = id,
        name = name,
        date_of_birth = dateOfBirth.ifBlank { "${age}/01/2010" },
        gender = gender,
        contact_info = phone,
        created_at = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(createdAt))
    )

    private fun estimateAge(dob: String): Int {
        val year = dob.split("/").lastOrNull()?.toIntOrNull() ?: 2010
        return (2026 - year).coerceIn(5, 80)
    }
}
