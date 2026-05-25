package com.example.orthofinixai.ai

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

object ImagePreprocessor {

    /** Standard bracket width calibration (mm). */
    const val BRACKET_WIDTH_MM = 3.2f

    fun preprocess(bitmap: Bitmap, targetSize: Int = 640): Bitmap {
        val scaled = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        return enhanceContrast(scaled)
    }

    private fun enhanceContrast(bmp: Bitmap): Bitmap {
        val out = bmp.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(out.width * out.height)
        out.getPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        for (i in pixels.indices) {
            val p = pixels[i]
            var r = (p shr 16 and 0xFF)
            var g = (p shr 8 and 0xFF)
            var b = (p and 0xFF)
            r = min(255, max(0, ((r - 128) * 1.15f + 128).toInt()))
            g = min(255, max(0, ((g - 128) * 1.15f + 128).toInt()))
            b = min(255, max(0, ((b - 128) * 1.15f + 128).toInt()))
            pixels[i] = Color.rgb(r, g, b)
        }
        out.setPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        return out
    }

    fun estimateBracketPixelWidth(bitmap: Bitmap): Float {
        val w = bitmap.width
        return w * 0.047f
    }

    fun scaleFactorMmPerPixel(bracketPixelWidth: Float): Float =
        if (bracketPixelWidth > 0f) BRACKET_WIDTH_MM / bracketPixelWidth else 0.1f
}
