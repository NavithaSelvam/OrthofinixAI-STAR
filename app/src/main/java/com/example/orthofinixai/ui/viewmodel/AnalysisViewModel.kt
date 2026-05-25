package com.example.orthofinixai.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orthofinixai.data.AnalysisSession
import com.example.orthofinixai.data.DemoCaseProvider
import com.example.orthofinixai.data.model.AIReport
import com.example.orthofinixai.data.repository.AnalysisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Processing : AnalysisState()
    data class Success(val report: AIReport) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnalysisRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val uiState: StateFlow<AnalysisState> = _uiState.asStateFlow()

    fun uploadAndAnalyze(
        caseId: String,
        imageBytes: ByteArray,
        patientName: String,
        dob: String,
        gender: String,
        imageUri: Uri?,
        viewType: String = "frontal"
    ) {
        viewModelScope.launch {
            _uiState.value = AnalysisState.Processing
            repository.analyzeImage(caseId, imageBytes, patientName, dob, gender, imageUri, viewType).collect { result ->
                result.onSuccess { _uiState.value = AnalysisState.Success(it) }
                    .onFailure { _uiState.value = AnalysisState.Error(it.message ?: "Analysis failed") }
            }
        }
    }

    fun loadReport(caseId: String) {
        viewModelScope.launch {
            _uiState.value = AnalysisState.Processing
            repository.getReport(caseId).collect { result ->
                result.onSuccess { _uiState.value = AnalysisState.Success(it) }
                    .onFailure { _uiState.value = AnalysisState.Error(it.message ?: "Failed to load report") }
            }
        }
    }

    fun loadDemoReport() {
        val report = DemoCaseProvider.buildDemoReport()
        AnalysisSession.lastReport = report
        _uiState.value = AnalysisState.Success(report)
    }

    fun reset() { _uiState.value = AnalysisState.Idle }
}
