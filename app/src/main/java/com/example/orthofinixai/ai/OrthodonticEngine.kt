package com.example.orthofinixai.ai

import android.content.Context
import android.graphics.Bitmap
import com.example.orthofinixai.ai.GeometryUtils.Point
import com.example.orthofinixai.data.model.ClinicalReport

/**
 * On-device orthodontic AI pipeline — 100% offline, no FastAPI required.
 */
class OrthodonticEngine(context: Context) {

    private val tflite = TFLiteInferenceManager(context)

    init { tflite.initialize() }

    suspend fun analyze(
        bitmap: Bitmap,
        viewType: String = "frontal",
        bracketPixelWidth: Float = ImagePreprocessor.estimateBracketPixelWidth(bitmap)
    ): ClinicalReport {
        val processed = ImagePreprocessor.preprocess(bitmap)
        val modelUsed = tflite.runSegmentation(processed) != null

        val rawMasks = tflite.runSegmentation(processed)
        val segments = if (rawMasks != null) {
            SegmentationProcessor.processModelOutput(rawMasks, processed.width, processed.height, viewType)
        } else {
            SegmentationProcessor.generateSyntheticSegments(viewType)
        }

        val rawHeatmaps = tflite.runLandmarks(processed)
        val landmarks: Map<String, Point> = if (rawHeatmaps != null) {
            LandmarkProcessor.extractLandmarks(rawHeatmaps, segments.keys.sorted(), processed.width, processed.height)
        } else {
            LandmarkProcessor.generateSyntheticLandmarks(segments, viewType)
        }

        val scaleFactor = GeometryUtils.calibrationScaleFactor(
            bracketPixelWidth.coerceAtLeast(8f),
            processed.width.toFloat()
        )

        val opPoints = buildList {
            for (key in listOf("16_midpoint", "26_midpoint", "36_midpoint", "46_midpoint", "11_incisal_edge", "21_incisal_edge")) {
                landmarks[key]?.let { add(it) }
            }
            if (size < 2) {
                add(Point(0.2f, 0.5f))
                add(Point(0.8f, 0.5f))
            }
        }
        val plane = GeometryUtils.fitOcclusalPlane(opPoints)

        val andrews = AndrewsSixKeysEngine.runFullAnalysis(landmarks, segments, plane, scaleFactor)
        val ojob = OverjetOverbiteEngine.analyze(landmarks, plane, scaleFactor)
        val roots = RootUprightingEngine.analyze(landmarks, plane, scaleFactor)

        val validation = ConfidenceValidator.validate(processed, segments, modelUsed, viewType)
        val confidence = validation.score

        val aboPenalty = calculateAboPenalty(andrews, ojob, roots)
        val archSymmetry = calculateArchSymmetry(segments)
        val supplemental = ClinicalRulesEngine.analyze(landmarks, segments, plane, scaleFactor, ojob.overjetMm, ojob.overbitePercent)

        val recs = buildRecommendations(andrews, ojob, roots, viewType).toMutableList()
        supplemental.findings.forEach { recs.add(it.explanation) }
        validation.warnings.forEach { if (!recs.contains(it)) recs.add(0, it) }

        val overlay = landmarks.mapValues { ClinicalReport.LandmarkPoint(it.value.x, it.value.y) }

        return ClinicalReport(
            viewType = viewType,
            confidenceScore = confidence,
            aboScore = aboPenalty,
            archSymmetryScore = archSymmetry,
            rootAngulationScore = roots.score,
            andrewsScore = andrews.overallScore,
            andrewsKeys = andrews.keys.map { key ->
                ClinicalReport.KeySummary(
                    key.keyNumber, key.keyName, key.status, key.score,
                    key.violations.map {
                        ClinicalReport.Violation(
                            it.toothFdi, it.measurementLabel, it.measured, it.ideal,
                            it.deviation, it.severity, it.clinicalExplanation
                        )
                    },
                    key.summaryExplanation
                )
            },
            overjetMm = ojob.overjetMm,
            overbitePercent = ojob.overbitePercent,
            overbiteAbsMm = ojob.overbiteAbsMm,
            overjetStatus = ojob.overjetStatus,
            overbiteStatus = ojob.overbiteStatus,
            rootDeviations = roots.deviations.map {
                ClinicalReport.RootDeviation(it.fdi, it.angleDeg, it.status, it.severity, it.recommendation)
            },
            recommendations = recs.distinct(),
            detectedTeethCount = segments.size,
            scaleFactor = scaleFactor,
            molarRightClass = supplemental.molarRightClass,
            molarLeftClass = supplemental.molarLeftClass,
            midlineDiscrepancyMm = supplemental.midlineDiscrepancyMm,
            curveOfSpeeMm = supplemental.curveOfSpeeMm,
            supplementalFindings = supplemental.findings.map {
                ClinicalReport.SupplementalFinding(
                    it.category, it.toothFdi, it.measurement, it.value, it.ideal, it.severity, it.explanation
                )
            },
            landmarkOverlay = overlay,
            detectedTeethFdi = segments.keys.sorted()
        )
    }

    private fun calculateAboPenalty(
        andrews: AndrewsSixKeysEngine.AndrewsReport,
        ojob: OverjetOverbiteEngine.OJOBResult,
        roots: RootUprightingEngine.RootParallelismResult
    ): Float {
        var penalty = 0f
        andrews.keys.forEach { key ->
            penalty += key.violations.size * when (key.keyNumber) {
                1 -> 2f; 2, 3 -> 1.5f; 4, 5 -> 1f; else -> 0.5f
            }
        }
        if (ojob.overjetMm > 4f) penalty += (ojob.overjetMm - 4f) * 2f
        if (ojob.overjetMm < 0f) penalty += 4f
        if (ojob.overbitePercent > 40f) penalty += ((ojob.overbitePercent - 40f) / 10f) * 1.5f
        roots.deviations.forEach { penalty += when (it.severity) { "Severe" -> 2f; "Moderate" -> 1f; else -> 0.5f } }
        return penalty
    }

    private fun calculateArchSymmetry(segments: Map<Int, SegmentationProcessor.ToothSegment>): Float {
        val left = segments.filter { it.key / 10 in listOf(2, 3) }.values.map { it.centroid.x }
        val right = segments.filter { it.key / 10 in listOf(1, 4) }.values.map { it.centroid.x }
        if (left.isEmpty() || right.isEmpty()) return 90f
        val diff = kotlin.math.abs(left.average() - (1.0 - right.average())).toFloat()
        return (100f - diff * 200f).coerceIn(50f, 99f)
    }

    private fun buildRecommendations(
        andrews: AndrewsSixKeysEngine.AndrewsReport,
        ojob: OverjetOverbiteEngine.OJOBResult,
        roots: RootUprightingEngine.RootParallelismResult,
        view: String
    ): List<String> {
        val recs = mutableListOf<String>()
        recs.add("Overjet: ${String.format("%.1f", ojob.overjetMm)} mm.")
        recs.add("Overbite: ${String.format("%.0f", ojob.overbitePercent)}%.")
        andrews.keys.forEach { key -> key.violations.forEach { recs.add(it.clinicalExplanation) } }
        recs.addAll(OverjetOverbiteEngine.recommendations(ojob))
        if (view == "opg") {
            roots.deviations.filter { it.severity != "Normal" }.forEach { recs.add(it.recommendation) }
        }
        if (recs.size <= 2) recs.add("Occlusion meets Andrews Six Keys criteria. Acceptable for debonding.")
        return recs
    }

    fun close() = tflite.close()
}
