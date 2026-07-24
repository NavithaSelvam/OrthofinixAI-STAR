package com.example.orthofinixai.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.orthofinixai.data.local.OrthofinixDatabase
import com.example.orthofinixai.data.model.AIReport
import com.example.orthofinixai.data.model.ClinicalReport
import com.example.orthofinixai.data.model.ClinicalReportMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

sealed class AnalysisProgress {
    data class Step(val progress: Float, val message: String) : AnalysisProgress()
    data class Complete(val report: AIReport) : AnalysisProgress()
    data class Failed(val error: String) : AnalysisProgress()
}

class AnalysisRepository(private val context: Context) {

    private val db = OrthofinixDatabase.getInstance(context)
    private val reportDao by lazy { db.reportDao() }
    private val caseRepository by lazy { CaseRepository(context) }
    private val authRepository by lazy { AuthRepository(context) }

    fun analyzeImageWithProgress(
        caseId: String,
        imageBytes: ByteArray,
        patientName: String,
        dob: String,
        gender: String,
        imageUri: Uri?,
        viewType: String
    ): Flow<AnalysisProgress> = flow {
        try {
            emit(AnalysisProgress.Step(0.05f, "Authenticating..."))

            val token = authRepository.getUserIdToken()

            Log.e(
                "AUTH_DEBUG",
                """
    UID=${com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid}
    EMAIL=${com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email}
    TOKEN_EXISTS=${token != null}
    TOKEN_LENGTH=${token?.length}
    """.trimIndent()
            )

            if (token.isNullOrEmpty()) {
                emit(
                    AnalysisProgress.Failed(
                        "Firebase token generation failed"
                    )
                )
                return@flow
            }
            
            val authHeader = "Bearer $token"
            Log.d("TOKEN_DEBUG", authHeader)
            Log.d(TAG, "Backend URL configuration: ${com.example.orthofinixai.data.api.ApiConfig.BASE_URL}")
            val api = com.example.orthofinixai.data.api.OrthofinixApi.create()

            emit(AnalysisProgress.Step(0.1f, "Uploading image to secure server..."))
            Log.d(TAG, "Uploading image started.")
            
            val uploadResponse = withContext(Dispatchers.IO) {
                val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = okhttp3.MultipartBody.Part.createFormData("file", "image.jpg", requestBody)
                api.uploadImage(authHeader, part)
            }
            Log.d(TAG, "Upload success. Upload ID: ${uploadResponse.upload_id}")
            
            emit(AnalysisProgress.Step(0.5f, "Running robust AI analysis pipeline..."))
            Log.d(TAG, "Analyze started.")
            
            val analysis = withContext(Dispatchers.IO) {
                api.analyzeImage(authHeader, uploadResponse.upload_id, patientName, viewType)
            }
            Log.d(TAG, "AI Finished. Analysis ID: ${analysis.id}")
            
            emit(AnalysisProgress.Step(0.8f, "Finalizing report..."))

            val reportId = analysis.id
            val metrics = analysis.metrics ?: emptyMap()
            val overjetOverbite = metrics["overjet_overbite"] as? Map<*, *>
            val rootParallelism = metrics["root_parallelism"] as? Map<*, *>
            
            val overjetMmVal = (overjetOverbite?.get("overjet_mm") as? Number)?.toFloat() ?: analysis.overjet_mm
            val overbitePercentVal = (overjetOverbite?.get("overbite_percent") as? Number)?.toFloat() ?: analysis.overbite_percent
            val overjetStatusVal = overjetOverbite?.get("overjet_status") as? String ?: "Normal"
            val overbiteStatusVal = overjetOverbite?.get("overbite_status") as? String ?: "Normal"
            
            // Extract Andrews Keys securely
            val rawAndrewsDetails = metrics["andrews_details"] as? List<*> ?: emptyList<Any>()
            val parsedAndrewsKeys = mutableListOf<ClinicalReport.KeySummary>()
            try {
                for (item in rawAndrewsDetails) {
                    if (item is Map<*, *>) {
                        val keyName = item["key"] as? String ?: ""
                        val score = (item["score"] as? Number)?.toFloat() ?: 1.0f
                        val explanation = item["explanation"] as? String ?: ""
                        // simplistic extraction
                        parsedAndrewsKeys.add(
                            ClinicalReport.KeySummary(
                                keyNumber = parsedAndrewsKeys.size + 1,
                                keyName = keyName,
                                status = if (score > 0.9f) "Pass" else "Fail",
                                score = score,
                                violations = emptyList(), // Can be parsed deeply if needed
                                explanation = explanation
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing andrews details", e)
            }

            // Extract Root Deviations securely
            val rawDeviations = rootParallelism?.get("deviations") as? List<*> ?: emptyList<Any>()
            val parsedDeviations = mutableListOf<ClinicalReport.RootDeviation>()
            try {
                for (item in rawDeviations) {
                    if (item is Map<*, *>) {
                        parsedDeviations.add(
                            ClinicalReport.RootDeviation(
                                fdi = (item["fdi"] as? Number)?.toInt() ?: 0,
                                angleDeg = (item["angle_deg"] as? Number)?.toFloat() ?: 0f,
                                status = item["status"] as? String ?: "Normal",
                                severity = item["severity"] as? String ?: "None",
                                recommendation = item["recommendation"] as? String ?: ""
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing root deviations", e)
            }

            val clinical = ClinicalReport(
                viewType = analysis.view_type,
                confidenceScore = analysis.confidence_score,
                aboScore = analysis.abo_score,
                archSymmetryScore = analysis.alignment_score,
                rootAngulationScore = analysis.root_angulation_score,
                andrewsScore = analysis.andrews_score,
                andrewsKeys = parsedAndrewsKeys, 
                overjetMm = overjetMmVal,
                overbitePercent = overbitePercentVal,
                overbiteAbsMm = 0f,
                overjetStatus = overjetStatusVal,
                overbiteStatus = overbiteStatusVal,
                rootDeviations = parsedDeviations,
                recommendations = analysis.recommendations,
                detectedTeethCount = (metrics["segmented_teeth"] as? Map<*, *>)?.size ?: 0,
                scaleFactor = (metrics["scale_factor"] as? Number)?.toFloat() ?: 1.0f,
                midlineDiscrepancyMm = analysis.midline_deviation_mm
            )
            
            val aiReport = ClinicalReportMapper.toAIReport(clinical, caseId, reportId)
            
            val patientObj = com.example.orthofinixai.data.model.Patient(
                id = caseId,
                name = patientName,
                dateOfBirth = dob,
                gender = gender,
                imageUrls = listOf(uploadResponse.image_url),
                createdAt = System.currentTimeMillis()
            )

            withContext(Dispatchers.IO) {
                caseRepository.saveFullCase(
                    patient = patientObj,
                    imageUri = imageUri,
                    imageBytes = imageBytes,
                    clinical = clinical,
                    aiReport = aiReport
                )
                Log.d(TAG, "Firestore Saved (or Case Repository Save Complete).")
                
                // Save to Room for offline cache synchronization
                val userId = AuthRepository.getCurrentUserId()
                val reportEntity = com.example.orthofinixai.data.local.entity.ReportEntity(
                    id = reportId,
                    userId = userId,
                    caseId = caseId,
                    patientId = patientObj.id,
                    viewType = clinical.viewType,
                    reportJson = clinical.toJson(),
                    aboScore = clinical.aboScore,
                    andrewsScore = clinical.andrewsScore,
                    archSymmetryScore = clinical.archSymmetryScore,
                    rootAngulationScore = clinical.rootAngulationScore,
                    confidenceScore = clinical.confidenceScore,
                    imagePath = "" // Would be filled by CaseRepository usually if needed
                )
                reportDao.insertReport(reportEntity)
                Log.d(TAG, "SQLite Saved.")
                
                val savedCase = caseRepository.getSavedCaseSync(caseId)
                if (savedCase != null) {
                    com.example.orthofinixai.util.PdfGenerator.generatePdf(context, savedCase)
                    Log.d(TAG, "PDF Generated.")
                }
            }
            
            emit(AnalysisProgress.Complete(aiReport))
        } catch (e: HttpException) {
            Log.e(TAG, "Analysis API failed: ${e.code()}", e)
            val errorMessage = when (e.code()) {
                401 -> "Session expired or unauthorized. Please log out and sign in again."
                413 -> "Image file too large."
                else -> "Server error (${e.code()}): ${e.message()}"
            }
            emit(AnalysisProgress.Failed(errorMessage))
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "Connection failed to backend. Check URL.", e)
            emit(AnalysisProgress.Failed("Failed to connect to backend: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Analysis progress failed", e)
            emit(AnalysisProgress.Failed(e.message ?: "An unexpected error occurred: ${e.javaClass.simpleName}"))
        }
    }

    fun getReport(caseId: String): Flow<Result<AIReport>> = flow {
        try {
            val userId = AuthRepository.getCurrentUserId()
            val cached = reportDao.getLatestByCase(userId, caseId)
            if (cached != null) {
                val clinical = ClinicalReport.fromJson(cached.reportJson)
                emit(Result.success(ClinicalReportMapper.toAIReport(clinical, cached.caseId, cached.id)))
            } else {
                val savedCase = caseRepository.getSavedCaseSync(caseId)
                if (savedCase != null) {
                    val clinical = ClinicalReport.fromJson(savedCase.clinicalDataJson)
                    emit(Result.success(ClinicalReportMapper.toAIReport(clinical, savedCase.id, savedCase.id)))
                } else {
                    emit(Result.failure(Exception("Report not found in local cache or Firestore")))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    companion object {
        private const val TAG = "AnalysisRepository"
    }
}
