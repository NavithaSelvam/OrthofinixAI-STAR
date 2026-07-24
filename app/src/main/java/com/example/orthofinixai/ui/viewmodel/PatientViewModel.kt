package com.example.orthofinixai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orthofinixai.data.model.Patient
import com.example.orthofinixai.data.model.PatientCreate
import com.example.orthofinixai.data.model.SavedCase
import com.example.orthofinixai.data.repository.CaseRepository
import com.example.orthofinixai.data.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class PatientState {
    object Idle : PatientState()
    object Loading : PatientState()
    data class Success(val patients: List<Patient>, val savedCases: List<SavedCase> = emptyList()) : PatientState()
    data class Error(val message: String) : PatientState()
}

class PatientViewModel(application: Application) : AndroidViewModel(application) {

    private val patientRepository = PatientRepository(application.applicationContext)
    private val caseRepository = CaseRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<PatientState>(PatientState.Idle)
    val uiState: StateFlow<PatientState> = _uiState.asStateFlow()

    fun fetchPatients() {
        viewModelScope.launch {
            _uiState.value = PatientState.Loading
            caseRepository.observeCases()
                .catch { e -> _uiState.value = PatientState.Error(e.message ?: "Failed to load") }
                .collect { cases ->
                    val patients = cases.map { c ->
                        Patient(
                            id = c.id,
                            name = c.patientName,
                            age = 0,
                            dateOfBirth = "",
                            gender = "",
                            phone = "",
                            email = "",
                            doctorName = c.doctorName,
                            hospital = "",
                            diagnosis = "",
                            treatmentDate = "",
                            notes = "",
                            imageUrls = emptyList(),
                            doctorId = "",
                            createdAt = c.createdAt
                        )
                    }
                    _uiState.value = PatientState.Success(patients, cases)
                }
        }
    }

    fun addPatient(
        name: String, dob: String, gender: String, phone: String = "", 
        email: String = "", doctorName: String = "", hospital: String = "", 
        diagnosis: String = "", treatmentDate: String = "", notes: String = "", 
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            patientRepository.createPatient(
                PatientCreate(
                    name = name,
                    dateOfBirth = dob,
                    gender = gender,
                    phone = phone,
                    email = email,
                    doctorName = doctorName,
                    hospital = hospital,
                    diagnosis = diagnosis,
                    treatmentDate = treatmentDate,
                    notes = notes
                )
            ).collect { result ->
                result.onSuccess { patient ->
                    onSuccess(patient.id)
                    fetchPatients()
                }.onFailure { error ->
                    _uiState.value = PatientState.Error(error.message ?: "Failed to create patient")
                }
            }
        }
    }

    fun deleteCase(caseId: String) {
        viewModelScope.launch {
            caseRepository.deleteCase(caseId)
            fetchPatients()
        }
    }

    fun getSavedCaseForReport(report: com.example.orthofinixai.data.model.AIReport): com.example.orthofinixai.data.model.SavedCase? {
        val state = uiState.value
        if (state is PatientState.Success) {
            return state.savedCases.find { it.id == report.case_id }
        }
        return null
    }
}
