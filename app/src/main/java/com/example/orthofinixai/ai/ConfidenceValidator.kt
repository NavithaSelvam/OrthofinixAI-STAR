package com.example.orthofinixai.ai

import android.graphics.Bitmap
import com.example.orthofinixai.ai.SegmentationProcessor.ToothSegment

object ConfidenceValidator {

    data class ValidationResult(
        val score: Float,
        val warnings: List<String>,
        val lowConfidence: Boolean
    )

    fun validate(
        bitmap: Bitmap?,
        segments: Map<Int, ToothSegment>,
        modelUsed: Boolean,
        viewType: String
    ): ValidationResult {
        val warnings = mutableListOf<String>()
        var score = if (modelUsed) 0.88f else 0.74f

        val expectedMin = if (viewType == "lateral") 4 else 8
        if (segments.size < expectedMin) {
            score -= 0.12f
            warnings.add("Detected ${segments.size} teeth; expected ≥ $expectedMin for $viewType view.")
        }

        if (bitmap != null) {
            val sample = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
            var variance = 0f
            val mean = (0 until 48).flatMap { y -> (0 until 48).map { x ->
                val px = sample.getPixel(x, y)
                ((px shr 16 and 0xFF) + (px shr 8 and 0xFF) + (px and 0xFF)) / 3f / 255f
            }}.average().toFloat()
            for (y in 0 until 48) for (x in 0 until 48) {
                val px = sample.getPixel(x, y)
                val g = ((px shr 16 and 0xFF) + (px shr 8 and 0xFF) + (px and 0xFF)) / 3f / 255f
                variance += (g - mean) * (g - mean)
            }
            variance /= (48 * 48)
            if (variance < 0.004f) {
                score -= 0.1f
                warnings.add("Image appears blurred or low contrast.")
            }
        }

        val left = segments.filter { it.key / 10 in listOf(2, 3) }
        val right = segments.filter { it.key / 10 in listOf(1, 4) }
        if (left.isNotEmpty() && right.isNotEmpty()) {
            val asym = kotlin.math.abs(left.size - right.size)
            if (asym > 3) {
                score -= 0.08f
                warnings.add("Arch asymmetry in detection count — verify FDI numbering.")
            }
        }

        score = score.coerceIn(0.2f, 0.97f)
        val low = score < 0.65f
        if (low) {
            warnings.add(0, "Detection confidence low. Please verify landmarks manually.")
        }

        return ValidationResult(score, warnings.distinct(), low)
    }
}
