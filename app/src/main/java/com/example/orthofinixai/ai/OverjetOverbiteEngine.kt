package com.example.orthofinixai.ai

import com.example.orthofinixai.ai.GeometryUtils.Point
import com.example.orthofinixai.ai.GeometryUtils.Vector
import com.example.orthofinixai.ai.GeometryUtils.OcclusalPlane
import kotlin.math.*

object OverjetOverbiteEngine {

    data class OJOBResult(
        val overjetMm: Float,
        val overbitePercent: Float,
        val overbiteAbsMm: Float,
        val overjetStatus: String,
        val overbiteStatus: String,
        val clinicalSummary: String
    )

    fun analyze(landmarks: Map<String, Point>, plane: OcclusalPlane, scaleFactor: Float): OJOBResult {
        val opVec  = plane.normalVector
        val opNorm = GeometryUtils.occlusalNormal(opVec)

        val upperIncisal = landmarks["11_incisal_edge"] ?: landmarks["21_incisal_edge"]
        val lowerIncisal = landmarks["31_incisal_edge"] ?: landmarks["41_incisal_edge"]
        val lowerApex    = landmarks["31_apex"]         ?: landmarks["41_apex"]

        val overjetMm = if (upperIncisal != null && lowerIncisal != null) {
            val v = Vector(upperIncisal.x - lowerIncisal.x, upperIncisal.y - lowerIncisal.y)
            GeometryUtils.projectMagnitude(v, opVec) * scaleFactor
        } else 2.5f

        val overbiteAbsMm = if (upperIncisal != null && lowerIncisal != null) {
            val v = Vector(upperIncisal.x - lowerIncisal.x, upperIncisal.y - lowerIncisal.y)
            abs(GeometryUtils.projectMagnitude(v, opNorm)) * scaleFactor
        } else 2.0f

        val crownH = if (lowerApex != null && lowerIncisal != null)
            GeometryUtils.distance(lowerApex, lowerIncisal) * scaleFactor else 9.5f

        val overbitePercent = (overbiteAbsMm / crownH.coerceAtLeast(1f)) * 100f

        val ojStatus = when {
            overjetMm < 0f        -> "Anterior Crossbite"
            overjetMm <= 4f       -> "Normal Overjet"
            overjetMm <= 6f       -> "Mild Excess Overjet"
            else                  -> "Excessive Overjet"
        }
        val obStatus = when {
            overbitePercent < 0f  -> "Anterior Open Bite"
            overbitePercent <= 10f -> "Reduced Overbite"
            overbitePercent <= 40f -> "Normal Overbite"
            overbitePercent <= 60f -> "Deep Bite"
            else                  -> "Severe Deep Bite"
        }

        return OJOBResult(
            overjetMm, overbitePercent, overbiteAbsMm, ojStatus, obStatus,
            "Overjet: ${String.format("%.1f",overjetMm)} mm ($ojStatus). " +
            "Overbite: ${String.format("%.1f",overbiteAbsMm)} mm / ${String.format("%.0f",overbitePercent)}% ($obStatus)."
        )
    }

    fun recommendations(r: OJOBResult): List<String> = buildList {
        when (r.overjetStatus) {
            "Excessive Overjet"   -> add("Overjet ${String.format("%.1f",r.overjetMm)} mm: Apply Class II elastics and correct upper incisor torque.")
            "Anterior Crossbite"  -> add("Anterior crossbite (${String.format("%.1f",r.overjetMm)} mm): Evaluate skeletal Class III; use cross-elastics and anterior bite opening.")
            "Mild Excess Overjet" -> add("Mild overjet ${String.format("%.1f",r.overjetMm)} mm: Verify upper incisor torque and arch coordination.")
        }
        when (r.overbiteStatus) {
            "Deep Bite", "Severe Deep Bite" -> add("Deep bite ${String.format("%.0f",r.overbitePercent)}%: Intrude lower incisors using utility arch or anterior bite turbos.")
            "Anterior Open Bite" -> add("Open bite detected: Eliminate oral habits; apply vertical elastics or TADs if skeletal origin.")
            "Reduced Overbite"   -> add("Reduced overbite: Monitor vertical anchorage during finishing.")
        }
    }
}
