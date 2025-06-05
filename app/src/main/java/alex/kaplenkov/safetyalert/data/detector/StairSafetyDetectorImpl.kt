package alex.kaplenkov.safetyalert.data.detector

import alex.kaplenkov.safetyalert.data.detector.base.BaseTensorFlowDetector
import alex.kaplenkov.safetyalert.domain.detector.SequentialDetector
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StairSafetyDetectorImpl(
    context: Context,
    modelPath: String
) : BaseTensorFlowDetector<Pair<Float, Float>>(context, modelPath, 0),
    SequentialDetector<Pair<Float, Float>> {

    companion object {
        private const val TAG = "StairSafetyDetector"
        private const val SEQUENCE_LENGTH = 6
        private const val HEATMAP_HEIGHT = 64
        private const val HEATMAP_WIDTH = 48
        private const val NUM_KEYPOINTS = 17
        private const val SAFETY_THRESHOLD = 0.8f
    }

    private var poseHeatmapSequence = mutableListOf<FloatArray>()

    override fun createInterpreterOptions(): Interpreter.Options {
        val options = Interpreter.Options()
        try {
            val delegate = FlexDelegate()
            options.addDelegate(delegate)
            Log.d(TAG, "Flex delegate added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Could not create Flex delegate", e)
        }
        return options
    }

    override fun createInputBuffer(): ByteBuffer {
        return ByteBuffer.allocateDirect(0)
    }

    override fun detect(bitmap: Bitmap): Pair<Float, Float> {
        return if (hasSufficientData()) {
            runInference()
        } else {
            Pair(0.0f, 0.0f)
        }
    }

    override fun addFrame(bitmap: Bitmap) {
        // Этот метод будет вызываться извне с heatmap данными
        // Для совместимости с интерфейсом
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

    override fun hasSufficientData(): Boolean {
        return poseHeatmapSequence.size == SEQUENCE_LENGTH
    }

    private fun runInference(): Pair<Float, Float> {
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
            return Pair(0.0f, 0.0f)
        }
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

    override fun clear() {
        poseHeatmapSequence.clear()
    }

    enum class HandrailStatus {
        SAFE,
        UNSAFE,
        UNKNOWN
    }
}