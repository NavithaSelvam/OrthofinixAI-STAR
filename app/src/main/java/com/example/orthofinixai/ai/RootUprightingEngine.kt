package com.example.orthofinixai.ai

import com.example.orthofinixai.ai.GeometryUtils.Point
import com.example.orthofinixai.ai.GeometryUtils.Vector
import com.example.orthofinixai.ai.GeometryUtils.OcclusalPlane
import kotlin.math.*

object RootUprightingEngine {

    data class ToothAngulation(
        val fdi: Int,
        val angleDeg: Float,
        val status: String,
        val severity: String,
        val recommendation: String
    )

    data class RootParallelismResult(
        val score: Float,
        val angulations: List<ToothAngulation>,
        val deviations: List<ToothAngulation>,
        val summary: String
    )

    fun analyze(landmarks: Map<String, Point>, plane: OcclusalPlane, scaleFactor: Float): RootParallelismResult {
        val opNorm = GeometryUtils.occlusalNormal(plane.normalVector)
        val angulations = mutableListOf<ToothAngulation>()

        val fdiList = listOf(11,12,13,14,15,16,21,22,23,24,25,26,
                             31,32,33,34,35,36,41,42,43,44,45,46)

        for (fdi in fdiList) {
            val apex = landmarks["${fdi}_apex"]     ?: continue
            val mid  = landmarks["${fdi}_midpoint"] ?: continue

            val vAxis = Vector(mid.x - apex.x, mid.y - apex.y)
            // θ = angle between long axis and occlusal plane normal
            val angleDeg = GeometryUtils.angleBetween(vAxis, opNorm) - 90f

            val (status, severity, rec) = when {
                abs(angleDeg) <= 2f  -> Triple("Upright", "Normal",
                    "Tooth $fdi root is well-uprighted (${String.format("%.1f",angleDeg)}°). No correction needed.")
                abs(angleDeg) <= 5f  -> Triple(if (angleDeg > 0) "Mesially Tipped" else "Distally Tipped", "Mild",
                    "Tooth $fdi: ${String.format("%.1f",abs(angleDeg))}° ${if(angleDeg>0) "mesial" else "distal"} tip. Place tip-back bend.")
                abs(angleDeg) <= 10f -> Triple(if (angleDeg > 0) "Mesially Tipped" else "Distally Tipped", "Moderate",
                    "Tooth $fdi: ${String.format("%.1f",abs(angleDeg))}° deviation — moderate uprighting required before debonding.")
                else -> Triple(if (angleDeg > 0) "Severely Mesially Tipped" else "Severely Distally Tipped", "Severe",
                    "Tooth $fdi: Severe ${String.format("%.1f",abs(angleDeg))}° root tipping. Uprighting auxiliary spring recommended.")
            }

            angulations.add(ToothAngulation(fdi, angleDeg, status, severity, rec))
        }

        val deviations = angulations.filter { it.severity != "Normal" }
        val score = if (angulations.isEmpty()) 1f else
            1f - (deviations.sumOf { when(it.severity) { "Severe" -> 2.0; "Moderate" -> 1.0; else -> 0.5 } }.toFloat() /
                    (angulations.size * 2f)).coerceIn(0f, 1f)

        return RootParallelismResult(
            score        = score * 100f,
            angulations  = angulations,
            deviations   = deviations,
            summary      = "${deviations.size} teeth with root angulation deviations. Score: ${String.format("%.1f", score * 100f)}%."
        )
    }
}
