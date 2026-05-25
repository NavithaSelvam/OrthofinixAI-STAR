package com.example.orthofinixai.ai

import com.example.orthofinixai.ai.GeometryUtils.OcclusalPlane
import com.example.orthofinixai.ai.GeometryUtils.Point
import com.example.orthofinixai.ai.SegmentationProcessor.ToothSegment
import kotlin.math.abs

/**
 * Rule-based orthodontic measurements from landmarks and segments.
 * Produces clinically formatted findings with FDI numbers, mm, and degrees.
 */
object ClinicalRulesEngine {

    data class ClinicalFinding(
        val category: String,
        val toothFdi: Int? = null,
        val measurement: String,
        val value: String,
        val ideal: String,
        val severity: String,
        val explanation: String
    )

    data class SupplementalMetrics(
        val findings: List<ClinicalFinding>,
        val molarRightClass: String,
        val molarLeftClass: String,
        val midlineDiscrepancyMm: Float,
        val totalSpacingMm: Float,
        val curveOfSpeeMm: Float,
        val crownAngulationNotes: List<String>,
        val rootParallelismNotes: List<String>
    )

    fun analyze(
        landmarks: Map<String, Point>,
        segments: Map<Int, ToothSegment>,
        plane: OcclusalPlane,
        scaleFactor: Float,
        overjetMm: Float,
        overbitePercent: Float
    ): SupplementalMetrics {
        val findings = mutableListOf<ClinicalFinding>()

        findings.add(ClinicalFinding(
            category = "Overjet",
            measurement = "Overjet",
            value = String.format("%.1f mm", overjetMm),
            ideal = "2.0–4.0 mm",
            severity = when {
                overjetMm in 2f..4f -> "Normal"
                overjetMm > 6f -> "Severe"
                overjetMm > 4f -> "Moderate"
                overjetMm < 0f -> "Severe"
                else -> "Mild"
            },
            explanation = "Overjet: ${String.format("%.1f", overjetMm)} mm. " +
                if (overjetMm in 2f..4f) "Within normal range."
                else if (overjetMm > 4f) "Increased overjet — evaluate maxillary incisor proclination or mandibular retrognathia."
                else "Negative overjet (crossbite tendency) — assess anterior crossbite."
        ))

        findings.add(ClinicalFinding(
            category = "Overbite",
            measurement = "Overbite",
            value = String.format("%.0f%% (%.1f mm)", overbitePercent, overbitePercent * 0.25f),
            ideal = "20–40%",
            severity = when {
                overbitePercent in 20f..40f -> "Normal"
                overbitePercent > 50f -> "Severe"
                overbitePercent > 40f -> "Moderate"
                else -> "Mild"
            },
            explanation = "Overbite: ${String.format("%.0f", overbitePercent)}%. " +
                if (overbitePercent in 20f..40f) "Acceptable vertical overlap."
                else if (overbitePercent > 40f) "Deep bite — consider anterior intrusion or posterior extrusion mechanics."
                else "Reduced overbite — monitor open-bite tendency."
        ))

        val (rightClass, leftClass) = classifyMolars(landmarks, scaleFactor)
        findings.add(ClinicalFinding(
            category = "Molar Relationship",
            measurement = "Right Molar",
            value = rightClass,
            ideal = "Class I",
            severity = if (rightClass.startsWith("Class I")) "Normal" else "Moderate",
            explanation = "Right molar relationship classified as $rightClass."
        ))
        findings.add(ClinicalFinding(
            category = "Molar Relationship",
            measurement = "Left Molar",
            value = leftClass,
            ideal = "Class I",
            severity = if (leftClass.startsWith("Class I")) "Normal" else "Moderate",
            explanation = "Left molar relationship classified as $leftClass."
        ))

        val midlineMm = calculateMidlineDiscrepancy(landmarks, scaleFactor)
        findings.add(ClinicalFinding(
            category = "Midline",
            measurement = "Midline Discrepancy",
            value = String.format("%.1f mm", midlineMm),
            ideal = "< 1.0 mm",
            severity = when {
                midlineMm < 1f -> "Normal"
                midlineMm < 2f -> "Mild"
                midlineMm < 4f -> "Moderate"
                else -> "Severe"
            },
            explanation = "Dental midline discrepancy: ${String.format("%.1f", midlineMm)} mm " +
                if (midlineMm < 1f) "— within acceptable limits."
                else "— asymmetric elastic mechanics or unilateral arch expansion may be indicated."
        ))

        val spacingMm = calculateSpacing(segments, scaleFactor)
        if (spacingMm > 0.5f) {
            findings.add(ClinicalFinding(
                category = "Spacing",
                measurement = "Interdental Spacing",
                value = String.format("%.1f mm total", spacingMm),
                ideal = "0 mm (contact)",
                severity = if (spacingMm > 3f) "Moderate" else "Mild",
                explanation = "Total arch spacing ${String.format("%.1f", spacingMm)} mm detected — evaluate Bolton discrepancy and closure mechanics."
            ))
        }

        val curveMm = calculateCurveOfSpee(landmarks, scaleFactor)
        findings.add(ClinicalFinding(
            category = "Curve of Spee",
            measurement = "Curve of Spee Depth",
            value = String.format("%.1f mm", curveMm),
            ideal = "< 2.0 mm",
            severity = when {
                curveMm < 2f -> "Normal"
                curveMm < 4f -> "Mild"
                else -> "Moderate"
            },
            explanation = "Curve of Spee depth: ${String.format("%.1f", curveMm)} mm. " +
                if (curveMm < 2f) "Flat occlusal plane — favorable."
                else "Excessive curve — consider reverse curve of Spee in lower archwire."
        ))

        val crownNotes = analyzeCrownAngulation(segments, landmarks, plane)
        val torqueNotes = analyzeTorqueInclination(segments, landmarks, plane)
        (crownNotes + torqueNotes).forEach { note ->
            val fdi = note.substringAfter("Tooth ").substringBefore(" ").toIntOrNull()
            findings.add(ClinicalFinding(
                category = "Crown Angulation",
                toothFdi = fdi,
                measurement = "Crown Tip",
                value = note,
                ideal = "Andrews ideal",
                severity = "Moderate",
                explanation = note
            ))
        }

        val rootNotes = analyzeRootParallelism(landmarks, plane)
        rootNotes.forEach { note ->
            val fdi = note.substringAfter("Tooth ").substringBefore(" ").toIntOrNull()
            findings.add(ClinicalFinding(
                category = "Root Angulation",
                toothFdi = fdi,
                measurement = "Root Uprighting",
                value = note,
                ideal = "Parallel to occlusal plane",
                severity = "Moderate",
                explanation = note
            ))
        }

        return SupplementalMetrics(
            findings = findings,
            molarRightClass = rightClass,
            molarLeftClass = leftClass,
            midlineDiscrepancyMm = midlineMm,
            totalSpacingMm = spacingMm,
            curveOfSpeeMm = curveMm,
            crownAngulationNotes = crownNotes,
            rootParallelismNotes = rootNotes
        )
    }

    private fun classifyMolars(landmarks: Map<String, Point>, scale: Float): Pair<String, String> {
        fun side(upper: Point?, lower: Point?, left: Boolean): String {
            if (upper == null || lower == null) return "Class I (insufficient data)"
            val dx = (upper.x - lower.x) * scale * 100f
            return when {
                abs(dx) <= 1.5f -> "Class I (${String.format("%.1f", abs(dx))} mm)"
                dx > 1.5f -> if (left) "Class II (${String.format("%.1f", dx)} mm)" else "Class II (${String.format("%.1f", abs(dx))} mm)"
                else -> if (left) "Class III (${String.format("%.1f", abs(dx))} mm)" else "Class III (${String.format("%.1f", abs(dx))} mm)"
            }
        }
        val right = side(
            landmarks["16_cusp_tip_buccal"] ?: landmarks["16_midpoint"],
            landmarks["46_buccal_groove"] ?: landmarks["46_midpoint"], false
        )
        val left = side(
            landmarks["26_cusp_tip_buccal"] ?: landmarks["26_midpoint"],
            landmarks["36_buccal_groove"] ?: landmarks["36_midpoint"], true
        )
        return right to left
    }

    private fun calculateMidlineDiscrepancy(landmarks: Map<String, Point>, scale: Float): Float {
        val u11 = landmarks["11_midpoint"] ?: landmarks["11_incisal_edge"]
        val u21 = landmarks["21_midpoint"] ?: landmarks["21_incisal_edge"]
        if (u11 == null || u21 == null) return 0.8f
        val archCenter = 0.5f
        val midline = (u11.x + u21.x) / 2f
        return abs(midline - archCenter) * scale * 100f
    }

    private fun calculateSpacing(segments: Map<Int, ToothSegment>, scale: Float): Float {
        val sorted = segments.values.sortedBy { it.centroid.x }
        if (sorted.size < 2) return 0f
        var total = 0f
        for (i in 0 until sorted.size - 1) {
            val gap = (sorted[i + 1].centroid.x - sorted[i].centroid.x) * scale * 100f
            if (gap > 2.5f) total += gap - 2.0f
        }
        return total.coerceAtLeast(0f)
    }

    private fun calculateCurveOfSpee(landmarks: Map<String, Point>, scale: Float): Float {
        val cusps = listOf("16_midpoint", "14_midpoint", "11_incisal_edge", "21_incisal_edge", "24_midpoint", "26_midpoint")
            .mapNotNull { landmarks[it] }
        if (cusps.size < 3) return 1.5f
        val avgY = cusps.map { it.y }.average().toFloat()
        val maxDev = cusps.maxOf { abs(it.y - avgY) }
        return maxDev * scale * 100f
    }

    private fun analyzeCrownAngulation(
        segments: Map<Int, ToothSegment>,
        landmarks: Map<String, Point>,
        plane: OcclusalPlane
    ): List<String> {
        val notes = mutableListOf<String>()
        val opVec = plane.normalVector
        for ((fdi, _) in segments) {
            val apex = landmarks["${fdi}_apex"] ?: continue
            val mid = landmarks["${fdi}_midpoint"] ?: continue
            val vAxis = GeometryUtils.Vector(mid.x - apex.x, mid.y - apex.y)
            var tipDeg = 90f - GeometryUtils.angleBetween(vAxis, opVec)
            val ideal = AndrewsSixKeysEngine.idealTipFor(fdi) ?: continue
            val dev = tipDeg - ideal
            if (abs(dev) > 3f) {
                val sign = if (dev > 0) "+" else ""
                notes.add("Tooth $fdi crown inclination deviates by $sign${String.format("%.0f", dev)}° (ideal ${String.format("%.0f", ideal)}°).")
            }
        }
        return notes.take(6)
    }

    private fun analyzeTorqueInclination(
        segments: Map<Int, ToothSegment>,
        landmarks: Map<String, Point>,
        plane: OcclusalPlane
    ): List<String> {
        val notes = mutableListOf<String>()
        val opVec = plane.normalVector
        for ((fdi, _) in segments) {
            val ideal = AndrewsSixKeysEngine.idealTorqueFor(fdi) ?: continue
            val mid = landmarks["${fdi}_midpoint"] ?: continue
            val edge = landmarks["${fdi}_incisal_edge"] ?: landmarks["${fdi}_cusp_tip_buccal"] ?: continue
            val labial = GeometryUtils.Vector(edge.x - mid.x, edge.y - mid.y)
            val measured = GeometryUtils.angleBetween(labial, opVec) - 90f
            val dev = measured - ideal
            if (abs(dev) > 3f) {
                val sign = if (dev > 0) "+" else ""
                notes.add("Tooth $fdi torque inclination deviates by $sign${String.format("%.0f", dev)}°.")
            }
        }
        return notes.take(6)
    }

    private fun analyzeRootParallelism(landmarks: Map<String, Point>, plane: OcclusalPlane): List<String> {
        val notes = mutableListOf<String>()
        val opVec = plane.normalVector
        listOf(13, 23, 33, 43, 14, 24).forEach { fdi ->
            val apex = landmarks["${fdi}_apex"] ?: return@forEach
            val mid = landmarks["${fdi}_midpoint"] ?: return@forEach
            val vAxis = GeometryUtils.Vector(apex.x - mid.x, apex.y - mid.y)
            val angle = GeometryUtils.angleBetween(vAxis, opVec) - 90f
            if (abs(angle) > 8f) {
                notes.add("Root uprighting required for tooth $fdi (${String.format("%.0f", abs(angle))}° deviation).")
            }
        }
        return notes.take(4)
    }
}
