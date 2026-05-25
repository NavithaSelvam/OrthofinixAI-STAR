package com.example.orthofinixai.ai

import com.example.orthofinixai.ai.GeometryUtils.Point
import com.example.orthofinixai.ai.GeometryUtils.Vector
import com.example.orthofinixai.ai.GeometryUtils.OcclusalPlane
import com.example.orthofinixai.ai.SegmentationProcessor.ToothSegment
import kotlin.math.*

/**
 * Complete Andrews' Six Keys evaluation engine — fully on-device, no network required.
 * All clinical formulas are measurement-based per Andrews (1972) standard values.
 */
object AndrewsSixKeysEngine {

    // Andrews ideal crown angulation (tip) per FDI tooth number
    private val IDEAL_TIP = mapOf(
        11 to 5f, 12 to 9f, 13 to 11f, 14 to 2f, 15 to 2f, 16 to 5f, 17 to 5f, 18 to 5f,
        21 to 5f, 22 to 9f, 23 to 11f, 24 to 2f, 25 to 2f, 26 to 5f, 27 to 5f, 28 to 5f,
        31 to 2f, 32 to 2f, 33 to 5f, 34 to 2f, 35 to 2f, 36 to 2f, 37 to 2f, 38 to 2f,
        41 to 2f, 42 to 2f, 43 to 5f, 44 to 2f, 45 to 2f, 46 to 2f, 47 to 2f, 48 to 2f
    )

    // Andrews ideal crown inclination (torque) per FDI
    private val IDEAL_TORQUE = mapOf(
        11 to 7f,  12 to 3f,  13 to -7f,  14 to -7f,  15 to -7f,  16 to -9f,  17 to -9f,  18 to -9f,
        21 to 7f,  22 to 3f,  23 to -7f,  24 to -7f,  25 to -7f,  26 to -9f,  27 to -9f,  28 to -9f,
        31 to -1f, 32 to -1f, 33 to -11f, 34 to -17f, 35 to -22f, 36 to -30f, 37 to -30f, 38 to -30f,
        41 to -1f, 42 to -1f, 43 to -11f, 44 to -17f, 45 to -22f, 46 to -30f, 47 to -30f, 48 to -30f
    )

    data class KeyViolation(
        val toothFdi: Int,
        val measurementLabel: String,
        val measured: Float,
        val ideal: Float,
        val deviation: Float,
        val severity: String,          // "Mild" | "Moderate" | "Severe"
        val clinicalExplanation: String
    )

    data class KeyResult(
        val keyNumber: Int,
        val keyName: String,
        val status: String,
        val score: Float,              // 0.0–1.0
        val violations: List<KeyViolation>,
        val summaryExplanation: String,
        val details: Map<String, Any> = emptyMap()
    )

    data class AndrewsReport(
        val overallScore: Float,        // 0–100
        val keys: List<KeyResult>
    )

    // ── Key 1: Molar Relationship ──────────────────────────────────────────────

    fun analyzeKey1(
        landmarks: Map<String, Point>,
        plane: OcclusalPlane,
        scaleFactor: Float
    ): KeyResult {
        val opVec = plane.normalVector

        fun classifySide(upperCusp: Point?, lowerGroove: Point?, leftSide: Boolean): Pair<String, Float> {
            if (upperCusp == null || lowerGroove == null) return "Insufficient Data" to 1f
            val dx = upperCusp.x - lowerGroove.x
            val dy = upperCusp.y - lowerGroove.y
            var disparityMm = GeometryUtils.projectMagnitude(Vector(dx, dy), opVec) * scaleFactor
            if (!leftSide) disparityMm = -disparityMm
            return when {
                disparityMm in -1.5f..1.5f -> "Class I (${String.format("%.1f", disparityMm)} mm)" to 1f
                disparityMm > 1.5f -> "Class II (${String.format("%.1f", disparityMm)} mm)" to
                        (1f - (disparityMm - 1.5f) / 5f).coerceIn(0f, 1f)
                else -> "Class III (${String.format("%.1f", abs(disparityMm))} mm)" to
                        (1f - (abs(disparityMm) - 1.5f) / 5f).coerceIn(0f, 1f)
            }
        }

        val (rightClass, rightScore) = classifySide(
            landmarks["16_cusp_tip_buccal"] ?: landmarks["16_midpoint"],
            landmarks["46_buccal_groove"]   ?: landmarks["46_midpoint"], false
        )
        val (leftClass, leftScore) = classifySide(
            landmarks["26_cusp_tip_buccal"] ?: landmarks["26_midpoint"],
            landmarks["36_buccal_groove"]   ?: landmarks["36_midpoint"], true
        )

        val avgScore = (rightScore + leftScore) / 2f
        return KeyResult(
            keyNumber = 1,
            keyName   = "Molar Relationship",
            status    = if (avgScore > 0.9f) "Class I Occlusion" else "Molar Discrepancy",
            score     = avgScore,
            violations = emptyList(),
            summaryExplanation = "Right molar: $rightClass. Left molar: $leftClass.",
            details   = mapOf("right" to rightClass, "left" to leftClass)
        )
    }

    // ── Key 2: Crown Angulation (Tip) ─────────────────────────────────────────

    fun analyzeKey2(
        landmarks: Map<String, Point>,
        segments: Map<Int, ToothSegment>,
        plane: OcclusalPlane
    ): KeyResult {
        val opVec = plane.normalVector
        val violations = mutableListOf<KeyViolation>()
        val scores = mutableListOf<Float>()
        val angulations = mutableMapOf<Int, Float>()

        for ((fdi, _) in segments) {
            val apex = landmarks["${fdi}_apex"] ?: continue
            val mid  = landmarks["${fdi}_midpoint"] ?: continue

            val vAxis = Vector(mid.x - apex.x, mid.y - apex.y)
            var tipDeg = 90f - GeometryUtils.angleBetween(vAxis, opVec)

            // Quadrant sign convention
            val quadrant = fdi / 10
            if (quadrant == 2 || quadrant == 3) tipDeg = -tipDeg

            angulations[fdi] = tipDeg
            val ideal = IDEAL_TIP[fdi] ?: 5f
            val dev   = abs(tipDeg - ideal)
            val ts    = (1f - dev / 10f).coerceIn(0f, 1f)
            scores.add(ts)

            if (dev > 3f) {
                violations.add(KeyViolation(
                    toothFdi = fdi,
                    measurementLabel = "Crown Angulation",
                    measured = tipDeg,
                    ideal    = ideal,
                    deviation = dev,
                    severity = when { dev > 8f -> "Severe"; dev > 5f -> "Moderate"; else -> "Mild" },
                    clinicalExplanation = "Tooth $fdi crown angulation is ${String.format("%.1f", tipDeg)}°, " +
                        "deviating ${String.format("%.1f", dev)}° from Andrews ideal (${ideal}°). " +
                        "Place a ${String.format("%.0f", dev)}° artistic tip-back bend to upright the crown."
                ))
            }
        }

        val avg = if (scores.isEmpty()) 1f else scores.average().toFloat()
        return KeyResult(
            keyNumber = 2, keyName = "Crown Angulation",
            status    = if (avg > 0.8f) "Acceptable Angulations" else "Tipping Violations",
            score     = avg, violations = violations,
            summaryExplanation = "Angulation score ${String.format("%.1f", avg * 100)}%. ${violations.size} teeth deviate.",
            details   = mapOf("perTooth" to angulations)
        )
    }

    // ── Key 3: Crown Inclination (Torque) ─────────────────────────────────────

    fun analyzeKey3(
        landmarks: Map<String, Point>,
        segments: Map<Int, ToothSegment>,
        plane: OcclusalPlane
    ): KeyResult {
        val nOp = GeometryUtils.occlusalNormal(plane.normalVector)
        val violations = mutableListOf<KeyViolation>()
        val scores = mutableListOf<Float>()
        val torques = mutableMapOf<Int, Float>()

        for ((fdi, _) in segments) {
            val cej = landmarks["${fdi}_cej_mesial"] ?: continue
            val inc = landmarks["${fdi}_incisal_edge"] ?: landmarks["${fdi}_cusp_tip_buccal"] ?: continue

            val vSurf  = Vector(inc.x - cej.x, inc.y - cej.y)
            var torque = 90f - GeometryUtils.angleBetween(vSurf, nOp)
            if (fdi >= 30) torque = -torque

            torques[fdi] = torque
            val ideal = IDEAL_TORQUE[fdi] ?: 0f
            val dev   = abs(torque - ideal)
            val ts    = (1f - dev / 12f).coerceIn(0f, 1f)
            scores.add(ts)

            if (dev > 4f) {
                violations.add(KeyViolation(
                    toothFdi = fdi,
                    measurementLabel = "Crown Inclination",
                    measured = torque, ideal = ideal, deviation = dev,
                    severity = when { dev > 10f -> "Severe"; dev > 6f -> "Moderate"; else -> "Mild" },
                    clinicalExplanation = "Tooth $fdi torque inclination is ${String.format("%.1f", torque)}°, " +
                        "exceeding Andrews Key 3 ideal of ${ideal}° by ${String.format("%.1f", dev)}°. " +
                        "Adjust rectangular archwire torque prescription."
                ))
            }
        }

        val avg = if (scores.isEmpty()) 1f else scores.average().toFloat()
        return KeyResult(
            keyNumber = 3, keyName = "Crown Inclination",
            status    = if (avg > 0.8f) "Optimal Torques" else "Inclination Violations",
            score     = avg, violations = violations,
            summaryExplanation = "Torque score ${String.format("%.1f", avg * 100)}%. ${violations.size} violations.",
            details   = mapOf("perTooth" to torques)
        )
    }

    // ── Key 4: Rotations ──────────────────────────────────────────────────────

    fun analyzeKey4(segments: Map<Int, ToothSegment>): KeyResult {
        val violations = mutableListOf<KeyViolation>()
        val scores = mutableListOf<Float>()
        val rotations = mutableMapOf<Int, Float>()

        for ((fdi, tooth) in segments) {
            val w = tooth.bbox.width; val h = tooth.bbox.height
            if (h == 0f) continue
            val aspect = w / h
            val idealAspect = if (tooth.toothClass in listOf("incisor", "canine")) 0.7f else 0.9f
            val rotDeg = (abs(aspect - idealAspect) * 90f).coerceIn(0f, 45f)
            rotations[fdi] = rotDeg
            val ts = (1f - rotDeg / 25f).coerceIn(0f, 1f)
            scores.add(ts)

            if (rotDeg > 6f) {
                violations.add(KeyViolation(
                    toothFdi = fdi, measurementLabel = "Rotation",
                    measured = rotDeg, ideal = 0f, deviation = rotDeg,
                    severity = when { rotDeg > 20f -> "Severe"; rotDeg > 12f -> "Moderate"; else -> "Mild" },
                    clinicalExplanation = "Tooth $fdi exhibits an estimated rotation of " +
                        "${String.format("%.1f", rotDeg)}°. Correct using offset bracket placement or auxiliary springs."
                ))
            }
        }

        val avg = if (scores.isEmpty()) 1f else scores.average().toFloat()
        return KeyResult(
            keyNumber = 4, keyName = "Absence of Rotations",
            status    = if (avg > 0.85f) "No Significant Rotations" else "Rotations Detected",
            score     = avg, violations = violations,
            summaryExplanation = "${violations.size} rotated teeth detected.",
            details   = mapOf("perTooth" to rotations)
        )
    }

    // ── Key 5: Spacing & Contacts ─────────────────────────────────────────────

    fun analyzeKey5(segments: Map<Int, ToothSegment>, scaleFactor: Float): KeyResult {
        val violations = mutableListOf<KeyViolation>()
        val scores = mutableListOf<Float>()
        val gaps = mutableMapOf<String, Float>()

        val upperFdi = segments.keys.filter { it < 30 }.sorted()
        val lowerFdi = segments.keys.filter { it >= 30 }.sorted()

        fun checkArch(fdis: List<Int>) {
            for (i in 0 until fdis.size - 1) {
                val t1 = segments[fdis[i]] ?: continue
                val t2 = segments[fdis[i + 1]] ?: continue
                val gapNorm = t2.bbox.left - t1.bbox.right
                val gapMm  = gapNorm * scaleFactor
                val key    = "${fdis[i]}-${fdis[i + 1]}"
                gaps[key]  = gapMm

                when {
                    gapMm > 0.5f -> {
                        val sev = when { gapMm > 2.5f -> "Severe"; gapMm > 1f -> "Moderate"; else -> "Mild" }
                        violations.add(KeyViolation(fdis[i], "Spacing", gapMm, 0f, gapMm, sev,
                            "Spacing of ${String.format("%.1f", gapMm)} mm between teeth ${fdis[i]} and ${fdis[i+1]}. " +
                                "Apply power chain elastics to close the space."))
                        scores.add((1f - gapMm / 3f).coerceIn(0f, 1f))
                    }
                    gapMm < -0.8f -> {
                        val dev = abs(gapMm)
                        val sev = when { dev > 3f -> "Severe"; dev > 1.5f -> "Moderate"; else -> "Mild" }
                        violations.add(KeyViolation(fdis[i], "Crowding", gapMm, 0f, dev, sev,
                            "Crowding of ${String.format("%.1f", dev)} mm between teeth ${fdis[i]} and ${fdis[i+1]}. " +
                                "Use open-coil springs or reproximation (IPR)."))
                        scores.add((1f - dev / 3f).coerceIn(0f, 1f))
                    }
                    else -> scores.add(1f)
                }
            }
        }
        checkArch(upperFdi); checkArch(lowerFdi)

        val avg = if (scores.isEmpty()) 1f else scores.average().toFloat()
        return KeyResult(
            keyNumber = 5, keyName = "Spacing and Contacts",
            status    = if (avg > 0.8f) "Tight Contacts" else "Contact Deviations",
            score     = avg, violations = violations,
            summaryExplanation = "${violations.size} spacing/crowding violations found.",
            details   = mapOf("gapsMm" to gaps)
        )
    }

    // ── Key 6: Curve of Spee ──────────────────────────────────────────────────

    fun analyzeKey6(landmarks: Map<String, Point>, scaleFactor: Float): KeyResult {
        val li = landmarks["31_incisal_edge"] ?: landmarks["41_incisal_edge"]
        val lm = landmarks["37_cusp_tip_buccal"] ?: landmarks["47_cusp_tip_buccal"]
            ?: landmarks["36_cusp_tip_buccal"] ?: landmarks["46_cusp_tip_buccal"]

        if (li == null || lm == null || abs(lm.x - li.x) < 1e-6f) {
            return KeyResult(6, "Curve of Spee", "Flat (Normal)", 1f, emptyList(),
                "Insufficient landmarks to measure Curve of Spee. Assuming flat arch.")
        }

        val m = (lm.y - li.y) / (lm.x - li.x)
        val c = li.y - m * li.x

        // Find deepest cusp below the chord
        var deepest = 0f
        for (fdi in listOf(34, 35, 36, 44, 45, 46)) {
            val cusp = landmarks["${fdi}_cusp_tip_buccal"] ?: landmarks["${fdi}_midpoint"] ?: continue
            val lineY = m * cusp.x + c
            val depth = cusp.y - lineY
            if (depth > deepest) deepest = depth
        }
        val depthMm = deepest * scaleFactor

        val (status, score, explanation) = when {
            depthMm <= 1.5f -> Triple("Flat (Normal)", 1f,
                "Curve of Spee depth is ${String.format("%.1f", depthMm)} mm — within ideal flat range.")
            depthMm <= 3f   -> Triple("Mildly Deep", 0.8f,
                "Curve of Spee is ${String.format("%.1f", depthMm)} mm deep. " +
                    "Consider RCS archwires or anterior bite turbos to level the arch.")
            else            -> Triple("Excessive Deep Curve", 0.5f,
                "Curve of Spee depth ${String.format("%.1f", depthMm)} mm is excessive. " +
                    "Levelling is mandatory prior to debonding.")
        }
        return KeyResult(6, "Curve of Spee", status, score, emptyList(), explanation,
            mapOf("depthMm" to depthMm))
    }

    // ── Master Runner ─────────────────────────────────────────────────────────

    fun runFullAnalysis(
        landmarks: Map<String, Point>,
        segments: Map<Int, ToothSegment>,
        plane: OcclusalPlane,
        scaleFactor: Float
    ): AndrewsReport {
        val keys = listOf(
            analyzeKey1(landmarks, plane, scaleFactor),
            analyzeKey2(landmarks, segments, plane),
            analyzeKey3(landmarks, segments, plane),
            analyzeKey4(segments),
            analyzeKey5(segments, scaleFactor),
            analyzeKey6(landmarks, scaleFactor)
        )
        val overall = keys.map { it.score }.average().toFloat() * 100f
        return AndrewsReport(overallScore = overall, keys = keys)
    }

    fun idealTipFor(fdi: Int): Float? = IDEAL_TIP[fdi]
    fun idealTorqueFor(fdi: Int): Float? = IDEAL_TORQUE[fdi]
}
