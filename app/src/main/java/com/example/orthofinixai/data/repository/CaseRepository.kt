package com.example.orthofinixai.data.repository

import android.content.Context
import android.net.Uri
import com.example.orthofinixai.data.SessionManager
import com.example.orthofinixai.data.local.OrthofinixDatabase
import com.example.orthofinixai.data.local.entity.CaseEntity
import com.example.orthofinixai.data.local.entity.PatientEntity
import com.example.orthofinixai.data.local.entity.ReportEntity
import com.example.orthofinixai.data.model.AIReport
import com.example.orthofinixai.data.model.ClinicalReport
import com.example.orthofinixai.data.model.SavedCase
import com.example.orthofinixai.util.ImageStorageUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CaseRepository(private val context: Context) {

    private val db = OrthofinixDatabase.getInstance(context)
    private val caseDao = db.caseDao()
    private val patientDao = db.patientDao()
    private val reportDao = db.reportDao()

    private fun userId(): String = SessionManager.requireUserId()

    fun observeCases(): Flow<List<SavedCase>> =
        caseDao.getCasesForUser(userId()).map { list ->
            list.map { it.toSavedCase() }
        }

    fun searchCases(query: String): Flow<List<SavedCase>> =
        caseDao.searchCases(userId(), query).map { it.map { e -> e.toSavedCase() } }

    suspend fun saveFullCase(
        caseId: String,
        patientName: String,
        dob: String,
        gender: String,
        imageUri: Uri?,
        imageBytes: ByteArray?,
        clinical: ClinicalReport,
        aiReport: AIReport
    ) {
        val uid = userId()
        val imagePath = when {
            imageUri != null -> ImageStorageUtil.saveImage(context, uid, caseId, imageUri)
            imageBytes != null && imageBytes.isNotEmpty() -> ImageStorageUtil.saveImageBytes(context, uid, caseId, imageBytes)
            else -> null
        } ?: ""

        patientDao.insertPatient(
            PatientEntity(
                id = caseId,
                userId = uid,
                name = patientName,
                dateOfBirth = dob,
                age = estimateAge(dob),
                gender = gender,
                phone = "",
                notes = "Orthodontic analysis"
            )
        )

        val reportId = aiReport.id
        caseDao.insertCase(
            CaseEntity(
                id = caseId,
                userId = uid,
                patientId = caseId,
                patientName = patientName,
                title = "Analysis — $patientName",
                viewType = clinical.viewType,
                imagePath = imagePath,
                reportJson = clinical.toJson(),
                reportId = reportId,
                confidenceScore = clinical.confidenceScore,
                aboScore = clinical.aboScore,
                andrewsScore = clinical.andrewsScore,
                status = "Analyzed"
            )
        )

        reportDao.insertReport(
            ReportEntity(
                id = reportId,
                userId = uid,
                caseId = caseId,
                patientId = caseId,
                viewType = clinical.viewType,
                reportJson = clinical.toJson(),
                aboScore = clinical.aboScore,
                andrewsScore = clinical.andrewsScore,
                archSymmetryScore = clinical.archSymmetryScore,
                rootAngulationScore = clinical.rootAngulationScore,
                confidenceScore = clinical.confidenceScore,
                imagePath = imagePath
            )
        )
    }

    suspend fun getCaseReport(caseId: String): AIReport? {
        val uid = userId()
        val case = caseDao.getCase(uid, caseId) ?: caseDao.getCaseById(caseId)?.takeIf { it.userId == uid }
        if (case != null && case.reportJson.isNotBlank()) {
            return ClinicalReport.fromJson(case.reportJson).toAiReport(caseId, case.reportId)
        }
        val report = reportDao.getLatestByCase(uid, caseId) ?: reportDao.getLatestByCaseId(caseId)?.takeIf { it.userId == uid }
        return report?.let { ClinicalReport.fromJson(it.reportJson).toAiReport(caseId, it.id) }
    }

    suspend fun getCaseImagePath(caseId: String): String? {
        val uid = userId()
        return caseDao.getCase(uid, caseId)?.imagePath?.takeIf { it.isNotBlank() }
    }

    suspend fun deleteCase(caseId: String) {
        val uid = userId()
        caseDao.deleteCase(uid, caseId)
        reportDao.deleteByCase(uid, caseId)
        ImageStorageUtil.deleteCaseImages(context, uid, caseId)
        patientDao.getPatient(uid, caseId)?.let { patientDao.deletePatient(it) }
    }

    private fun CaseEntity.toSavedCase() = SavedCase(
        id = id,
        patientId = patientId,
        patientName = patientName,
        imagePath = imagePath,
        viewType = viewType,
        confidenceScore = confidenceScore,
        andrewsScore = andrewsScore,
        createdAt = createdAt,
        hasReport = reportJson.isNotBlank()
    )

    private fun ClinicalReport.toAiReport(caseId: String, reportId: String): AIReport {
        val recs = recommendations.toMutableList()
        return AIReport(
            id = reportId,
            case_id = caseId,
            abo_score = aboScore,
            arch_symmetry_score = archSymmetryScore,
            root_angulation_score = rootAngulationScore,
            andrews_score = andrewsScore,
            recommendations = recs,
            created_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            confidence_score = confidenceScore,
            overjet_mm = overjetMm,
            overbite_percent = overbitePercent,
            overjet_status = overjetStatus,
            overbite_status = overbiteStatus,
            detected_teeth_count = detectedTeethCount,
            view_type = viewType,
            molar_right_class = molarRightClass,
            molar_left_class = molarLeftClass,
            midline_discrepancy_mm = midlineDiscrepancyMm,
            curve_of_spee_mm = curveOfSpeeMm,
            clinical_findings = supplementalFindings.map {
                com.example.orthofinixai.data.model.ClinicalFindingDto(
                    it.category, it.toothFdi, it.measurement, it.value, it.ideal, it.severity, it.explanation
                )
            },
            low_confidence_warning = if (confidenceScore < 0.65f)
                "Detection confidence low. Please verify landmarks manually." else null
        )
    }

    private fun estimateAge(dob: String): Int {
        val year = dob.split("/").lastOrNull()?.toIntOrNull() ?: 2010
        return (2026 - year).coerceIn(5, 80)
    }
}
