package com.example.orthofinixai.data.model

import com.google.gson.Gson
import java.util.Date

/** Complete clinical analysis report — serializable to JSON and Room. */
data class ClinicalReport(
    val viewType: String,
    val confidenceScore: Float,
    val aboScore: Float,
    val archSymmetryScore: Float,
    val rootAngulationScore: Float,
    val andrewsScore: Float,
    val andrewsKeys: List<KeySummary>,
    val overjetMm: Float,
    val overbitePercent: Float,
    val overbiteAbsMm: Float,
    val overjetStatus: String,
    val overbiteStatus: String,
    val rootDeviations: List<RootDeviation>,
    val recommendations: List<String>,
    val detectedTeethCount: Int,
    val scaleFactor: Float,
    val molarRightClass: String = "Class I",
    val molarLeftClass: String = "Class I",
    val midlineDiscrepancyMm: Float = 0f,
    val curveOfSpeeMm: Float = 0f,
    val supplementalFindings: List<SupplementalFinding> = emptyList(),
    val landmarkOverlay: Map<String, LandmarkPoint> = emptyMap(),
    val detectedTeethFdi: List<Int> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
) {
    data class LandmarkPoint(val x: Float, val y: Float)
    data class SupplementalFinding(
        val category: String,
        val toothFdi: Int? = null,
        val measurement: String,
        val value: String,
        val ideal: String,
        val severity: String,
        val explanation: String
    )
    data class KeySummary(
        val keyNumber: Int,
        val keyName: String,
        val status: String,
        val score: Float,
        val violations: List<Violation>,
        val explanation: String
    )

    data class Violation(
        val toothFdi: Int,
        val measurementLabel: String,
        val measured: Float,
        val ideal: Float,
        val deviation: Float,
        val severity: String,
        val clinicalExplanation: String
    )

    data class RootDeviation(
        val fdi: Int,
        val angleDeg: Float,
        val status: String,
        val severity: String,
        val recommendation: String
    )

    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): ClinicalReport = Gson().fromJson(json, ClinicalReport::class.java)
    }
}
