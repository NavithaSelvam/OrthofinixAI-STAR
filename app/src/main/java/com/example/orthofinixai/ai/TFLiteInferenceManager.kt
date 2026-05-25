package com.example.orthofinixai.ai

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Manages loading and running TFLite models from assets/models/.
 * Falls back to rule-based synthetic analysis when models are not bundled.
 */
class TFLiteInferenceManager(private val context: Context) {

    private var segInterpreter: Interpreter? = null
    private var landmarkInterpreter: Interpreter? = null

    companion object {
        const val SEG_MODEL = "models/seg_model.tflite"
        const val LANDMARK_MODEL = "models/landmarks_model.tflite"
        const val SEG_INPUT_SIZE = 640
        const val LM_INPUT_SIZE = 512
        const val LM_HEATMAP_SIZE = 128
        const val NUM_LANDMARKS = 19
    }

    fun initialize() {
        val options = Interpreter.Options().apply { setNumThreads(4) }
        try {
            segInterpreter = Interpreter(loadModel(SEG_MODEL), options)
            landmarkInterpreter = Interpreter(loadModel(LANDMARK_MODEL), options)
        } catch (e: Exception) {
            android.util.Log.w("TFLite", "Model load failed (rule-based fallback): ${e.message}")
        }
    }

    private fun loadModel(assetPath: String): MappedByteBuffer {
        val afd = context.assets.openFd(assetPath)
        return FileInputStream(afd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
    }

    fun runSegmentation(bitmap: Bitmap): Array<FloatArray>? {
        val interpreter = segInterpreter ?: return null
        val scaled = Bitmap.createScaledBitmap(bitmap, SEG_INPUT_SIZE, SEG_INPUT_SIZE, true)
        val input = bitmapToInputTensor(scaled, SEG_INPUT_SIZE)
        val output = Array(1) { Array(80) { FloatArray(6400) } }
        interpreter.run(input, output)
        return output[0]
    }

    fun runLandmarks(bitmap: Bitmap): Array<Array<FloatArray>>? {
        val interpreter = landmarkInterpreter ?: return null
        val scaled = Bitmap.createScaledBitmap(bitmap, LM_INPUT_SIZE, LM_INPUT_SIZE, true)
        val input = bitmapToInputTensor(scaled, LM_INPUT_SIZE)
        val numChannels = NUM_LANDMARKS * 7
        val output = Array(1) { Array(numChannels) { Array(LM_HEATMAP_SIZE) { FloatArray(LM_HEATMAP_SIZE) } } }
        interpreter.run(input, output)
        return output[0]
    }

    private fun bitmapToInputTensor(bmp: Bitmap, size: Int): Array<Array<Array<FloatArray>>> {
        val tensor = Array(1) { Array(size) { Array(size) { FloatArray(3) } } }
        for (y in 0 until size) for (x in 0 until size) {
            val px = bmp.getPixel(x, y)
            tensor[0][y][x][0] = ((px shr 16 and 0xFF) / 255f)
            tensor[0][y][x][1] = ((px shr 8 and 0xFF) / 255f)
            tensor[0][y][x][2] = ((px and 0xFF) / 255f)
        }
        return tensor
    }

    fun close() {
        segInterpreter?.close()
        landmarkInterpreter?.close()
    }
}
