package com.example.orthofinixai.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.orthofinixai.data.SessionManager
import com.example.orthofinixai.data.model.AIReport
import com.example.orthofinixai.data.model.ClinicalReport
import com.example.orthofinixai.data.model.SavedCase
import com.example.orthofinixai.data.model.Patient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CaseRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private fun userId(): String =
        SessionManager.currentUserId ?: AuthRepository.getCurrentUserId()

    fun observeCases(): Flow<List<SavedCase>> = kotlinx.coroutines.flow.callbackFlow {
        val uid = userId()
        val listenerRegistration = firestore.collection("users")
            .document(uid)
            .collection("cases")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error fetching cases", e)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val cases = snapshot.toObjects(SavedCase::class.java)
                    trySend(cases)
                } else {
                    trySend(emptyList())
                }
            }
            
        awaitClose {
            listenerRegistration.remove()
        }
    }

    suspend fun saveFullCase(
        patient: Patient,
        imageUri: Uri?,
        imageBytes: ByteArray?,
        clinical: ClinicalReport,
        aiReport: AIReport
    ) {
        val uid = userId()
        val caseId = aiReport.case_id
        
        var downloadUrl = ""
        
        // Upload to Firebase Storage
        try {
            val storageRef = storage.reference.child("users/$uid/cases/$caseId/image.jpg")
            if (imageUri != null) {
                storageRef.putFile(imageUri).await()
                downloadUrl = storageRef.downloadUrl.await().toString()
            } else if (imageBytes != null && imageBytes.isNotEmpty()) {
                storageRef.putBytes(imageBytes).await()
                downloadUrl = storageRef.downloadUrl.await().toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload image", e)
        }

        // Save to Firestore
        val savedCase = SavedCase(
            id = caseId,
            patientId = patient.id,
            patientName = patient.name,
            doctorName = patient.doctorName,
            imagePath = downloadUrl, // Store Firebase Storage URL
            viewType = clinical.viewType,
            confidenceScore = clinical.confidenceScore,
            aboScore = clinical.aboScore,
            andrewsScore = clinical.andrewsScore,
            createdAt = System.currentTimeMillis(),
            hasReport = true,
            clinicalDataJson = clinical.toJson(),
            patientProfile = patient
        )
        
        try {
            firestore.collection("users")
                .document(uid)
                .collection("cases")
                .document(caseId)
                .set(savedCase)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save case to Firestore", e)
        }
    }
    
    suspend fun getClinicalReport(caseId: String): ClinicalReport? {
        val uid = userId()
        try {
            val doc = firestore.collection("users").document(uid).collection("cases").document(caseId).get().await()
            val savedCase = doc.toObject(SavedCase::class.java)
            return savedCase?.clinicalDataJson?.let { ClinicalReport.fromJson(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get report", e)
            return null
        }
    }

    suspend fun getSavedCaseSync(caseId: String): SavedCase? {
        val uid = userId()
        try {
            val doc = firestore.collection("users").document(uid).collection("cases").document(caseId).get().await()
            return doc.toObject(SavedCase::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get saved case", e)
            return null
        }
    }

    suspend fun deleteCase(caseId: String) {
        val uid = userId()
        try {
            firestore.collection("users").document(uid).collection("cases").document(caseId).delete().await()
            // Try to delete image
            storage.reference.child("users/$uid/cases/$caseId/image.jpg").delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete case", e)
        }
    }

    companion object {
        private const val TAG = "CaseRepository"
    }
}
