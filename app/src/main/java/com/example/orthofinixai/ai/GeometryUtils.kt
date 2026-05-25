package com.example.orthofinixai.ai

import kotlin.math.*

/**
 * Core geometric mathematics for orthodontic calculations.
 * Mirrors the Python geometry.py module — runs entirely on-device.
 */
object GeometryUtils {

    data class Point(val x: Float, val y: Float)
    data class Vector(val x: Float, val y: Float)
    data class OcclusalPlane(val slope: Float, val intercept: Float, val normalVector: Vector)

    /** Euclidean distance between two points (normalized coordinates). */
    fun distance(p1: Point, p2: Point): Float =
        sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))

    /** Distance converted to millimeters using calibration scale factor. */
    fun distanceMm(p1: Point, p2: Point, scaleFactor: Float): Float =
        distance(p1, p2) * scaleFactor

    /** Angle in degrees between two 2D vectors. */
    fun angleBetween(v1: Vector, v2: Vector): Float {
        val dot = v1.x * v2.x + v1.y * v2.y
        val len1 = sqrt(v1.x.pow(2) + v1.y.pow(2))
        val len2 = sqrt(v2.x.pow(2) + v2.y.pow(2))
        val denom = len1 * len2
        if (denom == 0f) return 0f
        val cosVal = (dot / denom).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosVal.toDouble())).toFloat()
    }

    /** Signed magnitude of vector v projected onto direction u. */
    fun projectMagnitude(v: Vector, u: Vector): Float {
        val uLen = sqrt(u.x.pow(2) + u.y.pow(2))
        if (uLen == 0f) return 0f
        return (v.x * u.x + v.y * u.y) / uLen
    }

    /**
     * Fits an occlusal plane line through a set of landmark points using RANSAC.
     * Returns slope, intercept, and normalized direction vector.
     */
    fun fitOcclusalPlane(points: List<Point>): OcclusalPlane {
        if (points.size < 2) return OcclusalPlane(0f, 0.5f, Vector(1f, 0f))

        if (points.size <= 3) {
            return linearRegression(points)
        }

        var bestSlope = 0f
        var bestIntercept = 0.5f
        var maxInliers = 0
        val threshold = 0.03f

        repeat(20) {
            val i1 = (points.indices).random()
            var i2 = (points.indices).random()
            while (i2 == i1) i2 = (points.indices).random()

            val p1 = points[i1]; val p2 = points[i2]
            if (abs(p2.x - p1.x) < 1e-6f) return@repeat

            val m = (p2.y - p1.y) / (p2.x - p1.x)
            val c = p1.y - m * p1.x
            val denom = sqrt(m.pow(2) + 1f)

            val inliers = points.count { p ->
                abs(m * p.x - p.y + c) / denom < threshold
            }

            if (inliers > maxInliers) {
                maxInliers = inliers; bestSlope = m; bestIntercept = c
            }
        }

        // Re-fit on inliers
        val denom = sqrt(bestSlope.pow(2) + 1f)
        val inlierPts = points.filter { p ->
            abs(bestSlope * p.x - p.y + bestIntercept) / denom < threshold
        }
        val refined = if (inlierPts.size >= 2) linearRegression(inlierPts) else
            OcclusalPlane(bestSlope, bestIntercept, normalizeVector(Vector(1f, bestSlope)))

        return refined
    }

    private fun linearRegression(pts: List<Point>): OcclusalPlane {
        val n = pts.size.toFloat()
        val sumX = pts.sumOf { it.x.toDouble() }.toFloat()
        val sumY = pts.sumOf { it.y.toDouble() }.toFloat()
        val sumXY = pts.sumOf { (it.x * it.y).toDouble() }.toFloat()
        val sumX2 = pts.sumOf { (it.x * it.x).toDouble() }.toFloat()
        val denom = n * sumX2 - sumX * sumX
        val m = if (abs(denom) < 1e-6f) 0f else (n * sumXY - sumX * sumY) / denom
        val c = (sumY - m * sumX) / n
        return OcclusalPlane(m, c, normalizeVector(Vector(1f, m)))
    }

    private fun normalizeVector(v: Vector): Vector {
        val len = sqrt(v.x.pow(2) + v.y.pow(2))
        return if (len == 0f) Vector(1f, 0f) else Vector(v.x / len, v.y / len)
    }

    /**
     * Computes the calibration scale factor.
     * Standard bracket width = 3.2 mm.
     * scaleFactor (mm/normalized unit) = 3.2 / normalizedBracketWidth
     */
    fun calibrationScaleFactor(bracketPixelWidth: Float, imageWidthPx: Float): Float {
        val normWidth = bracketPixelWidth / imageWidthPx.coerceAtLeast(1f)
        return if (normWidth > 0f) 3.2f / normWidth else 85f
    }

    /** Perpendicular (normal) vector to the occlusal plane direction. */
    fun occlusalNormal(planeVector: Vector): Vector =
        normalizeVector(Vector(-planeVector.y, planeVector.x))
}
