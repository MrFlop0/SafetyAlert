package alex.kaplenkov.safetyalert.data.detector

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StairSafetyDetector(context: Context, modelPath: String) {
    companion object {
        private const val TAG = "StairSafetyDetector"
        private const val SEQUENCE_LENGTH = 6
        private const val HEATMAP_HEIGHT = 64
        private const val HEATMAP_WIDTH = 48
        private const val NUM_KEYPOINTS = 17
        private const val SAFETY_THRESHOLD = 0.8f
    }

    private val interpreter: Interpreter
    private var poseHeatmapSequence = mutableListOf<FloatArray>()

    init {
        val options = Interpreter.Options()

        try {
            val delegate = FlexDelegate()
            options.addDelegate(delegate)
            Log.d(TAG, "Flex delegate added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Could not create Flex delegate", e)
        }

        interpreter = Interpreter(FileUtil.loadMappedFile(context, modelPath), options)
        interpreter.allocateTensors()

        Log.d(TAG, "Stair safety model loaded")
    }

    fun addPoseHeatmap(heatmap: FloatArray) {
        if (heatmap.size != HEATMAP_HEIGHT * HEATMAP_WIDTH * NUM_KEYPOINTS) {
            Log.w(TAG, "Heatmap size mismatch! Received: ${heatmap.size}, Expected: ${HEATMAP_HEIGHT * HEATMAP_WIDTH * NUM_KEYPOINTS}")
        }

        poseHeatmapSequence.add(heatmap)
        Log.d(TAG, "Added heatmap to sequence. Current size: ${poseHeatmapSequence.size}")

        if (poseHeatmapSequence.size > SEQUENCE_LENGTH) {
            poseHeatmapSequence.removeAt(0)
            Log.d(TAG, "Removed oldest frame from sequence. New size: ${poseHeatmapSequence.size}")
        }
    }

    fun hasSufficientData(): Boolean {
        return poseHeatmapSequence.size == SEQUENCE_LENGTH
    }

    private fun prepareStairSafetyInput(): ByteBuffer {
        val flatSize = HEATMAP_HEIGHT * HEATMAP_WIDTH * NUM_KEYPOINTS
        val buffer = ByteBuffer.allocateDirect(1 * SEQUENCE_LENGTH * flatSize * 4).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        Log.d(TAG, "Preparing input buffer for ${poseHeatmapSequence.size} frames")

        for (heatmap in poseHeatmapSequence) {
            for (value in heatmap) {
                buffer.putFloat(value)
            }
        }

        buffer.rewind()
        return buffer
    }

    fun detect(): Pair<Float, Float> {
        if (!hasSufficientData()) {
            return Pair(0.0f, 0.0f)
        }

        try {
            val inputBuffer = prepareStairSafetyInput()
            val outputBuffer = Array(1) { FloatArray(2) }

            interpreter.run(inputBuffer, outputBuffer)

            val safeScore = outputBuffer[0][0]
            val unsafeScore = outputBuffer[0][1]

            Log.d(TAG, "Stair safety prediction: safe=$safeScore, unsafe=$unsafeScore")

            return Pair(safeScore, unsafeScore)
        } catch (e: Exception) {
            Log.e(TAG, "Error running model inference", e)
            e.printStackTrace()
            return Pair(0.0f, 0.0f)
        }
    }

    fun isHandrailSafe(safeUnsafeScores: Pair<Float, Float>): HandrailStatus {
        val (safeScore, unsafeScore) = safeUnsafeScores
        Log.d(TAG, "Checking safety with scores - Safe: $safeScore, Unsafe: $unsafeScore, Threshold: $SAFETY_THRESHOLD")

        return when {
            safeScore > SAFETY_THRESHOLD -> HandrailStatus.SAFE
            unsafeScore > SAFETY_THRESHOLD -> HandrailStatus.UNSAFE
            safeScore > unsafeScore + 0.2f -> HandrailStatus.SAFE
            else -> HandrailStatus.UNSAFE
        }
    }

    fun clear() {
        poseHeatmapSequence.clear()
    }

    fun close() {
        interpreter.close()
    }

    enum class HandrailStatus {
        SAFE,
        UNSAFE,
        UNKNOWN
    }
}