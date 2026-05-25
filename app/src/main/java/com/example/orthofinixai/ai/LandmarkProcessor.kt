package com.example.orthofinixai.ai

import com.example.orthofinixai.ai.GeometryUtils.Point
import kotlin.math.*

/**
 * Converts TFLite heatmap regression output into sub-pixel landmark coordinates.
 * Mirrors the Python landmarks.py HRNet peak extraction with gravity-center refinement.
 */
object LandmarkProcessor {

    // All landmark key names produced for each FDI tooth
    private val LANDMARK_TYPES = listOf(
        "apex", "midpoint", "incisal_edge", "cusp_tip_buccal",
        "cej_mesial", "cej_distal", "buccal_groove"
    )

    /**
     * Extracts sub-pixel landmarks from a heatmap tensor.
     * @param heatmaps Float array shaped [numChannels][height][width]
     * @param fdiList  Ordered list of FDI numbers corresponding to each heatmap channel group
     */
    fun extractLandmarks(
        heatmaps: Array<Array<FloatArray>>,
        fdiList: List<Int>,
        imageWidth: Int,
        imageHeight: Int
    ): Map<String, Point> {
        val landmarks = mutableMapOf<String, Point>()
        var channelIdx = 0

        for (fdi in fdiList) {
            for (lmType in LANDMARK_TYPES) {
                if (channelIdx >= heatmaps.size) break
                val hm = heatmaps[channelIdx++]
                val peak = gravityCenter(hm)
                if (peak != null) {
                    landmarks["${fdi}_${lmType}"] = Point(
                        peak.first / hm[0].size.toFloat(),
                        peak.second / hm.size.toFloat()
                    )
                }
            }
        }
        return landmarks
    }

    /** 3×3 gravity-center sub-pixel refinement around the heatmap maximum. */
    private fun gravityCenter(hm: Array<FloatArray>): Pair<Float, Float>? {
        val H = hm.size; val W = if (H > 0) hm[0].size else 0
        if (H == 0 || W == 0) return null

        var maxVal = Float.NEGATIVE_INFINITY
        var maxR = 0; var maxC = 0
        for (r in 0 until H) for (c in 0 until W) {
            if (hm[r][c] > maxVal) { maxVal = hm[r][c]; maxR = r; maxC = c }
        }
        if (maxVal <= 0f) return null

        // 3×3 neighbourhood
        var sumW = 0f; var sumX = 0f; var sumY = 0f
        for (dr in -1..1) for (dc in -1..1) {
            val r = (maxR + dr).coerceIn(0, H - 1)
            val c = (maxC + dc).coerceIn(0, W - 1)
            val w = hm[r][c].coerceAtLeast(0f)
            sumW += w; sumX += w * c; sumY += w * r
        }
        return if (sumW > 0f) Pair(sumX / sumW, sumY / sumW) else Pair(maxC.toFloat(), maxR.toFloat())
    }

    /**
     * Generates anatomically realistic synthetic landmarks from tooth segments.
     * Used when model output is insufficient (stub model / low confidence).
     */
    fun generateSyntheticLandmarks(
        segments: Map<Int, SegmentationProcessor.ToothSegment>,
        viewType: String
    ): Map<String, Point> {
        val lm = mutableMapOf<String, Point>()

        for ((fdi, tooth) in segments) {
            val box = tooth.bbox
            val cx = tooth.centroid.x
            val cy = tooth.centroid.y
            val isUpper = fdi < 30
            val h = box.height
            val w = box.width

            // Apex (root tip) — above for upper, below for lower
            lm["${fdi}_apex"] = if (isUpper)
                Point(cx, box.top - h * 0.3f)
            else
                Point(cx, box.bottom + h * 0.3f)

            // Midpoint (crown centre / FA point)
            lm["${fdi}_midpoint"] = Point(cx, cy)

            // Incisal edge / cusp tip (bottom of upper crown, top of lower crown)
            val cuspalEdge = if (isUpper) box.bottom else box.top
            lm["${fdi}_incisal_edge"] = Point(cx, cuspalEdge)
            lm["${fdi}_cusp_tip_buccal"] = Point(cx + w * 0.1f, cuspalEdge)

            // CEJ (cemento-enamel junction) mesial and distal
            val cejY = if (isUpper) box.top + h * 0.35f else box.bottom - h * 0.35f
            lm["${fdi}_cej_mesial"] = Point(box.left + w * 0.15f, cejY)
            lm["${fdi}_cej_distal"] = Point(box.right - w * 0.15f, cejY)

            // Buccal groove (molars / premolars only)
            if (tooth.toothClass in listOf("molar", "premolar")) {
                lm["${fdi}_buccal_groove"] = Point(cx - w * 0.1f, cy + (if (isUpper) h * 0.1f else -h * 0.1f))
            }
        }
        return lm
    }
}
