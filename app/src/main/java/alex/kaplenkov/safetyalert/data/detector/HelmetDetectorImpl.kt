package alex.kaplenkov.safetyalert.data.detector

import alex.kaplenkov.safetyalert.data.detector.base.BaseTensorFlowDetector
import alex.kaplenkov.safetyalert.data.model.HelmetDetection
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import kotlin.math.max
import kotlin.math.min

class HelmetDetectorImpl(
    context: Context,
    modelPath: String
) : BaseTensorFlowDetector<List<HelmetDetection>>(context, modelPath, INPUT_SIZE) {

    companion object {
        private const val TAG = "HelmetDetector"
        private const val INPUT_SIZE = 416
        private const val CONF_THRESHOLD = 0.25f
        private const val NMS_THRESHOLD = 0.45f
    }

    override fun detect(bitmap: Bitmap): List<HelmetDetection> {
        val (scaledBitmap, ratio, offsets) = preprocessImage(bitmap)
        val (xOffset, yOffset) = offsets

        fillInputBuffer(scaledBitmap)

        val outputShape = intArrayOf(1, 5, 3549)
        val outputBuffer = Array(outputShape[0]) {
            Array(outputShape[1]) {
                FloatArray(outputShape[2])
            }
        }

        interpreter.run(inputBuffer, outputBuffer)

        val transposed = transposeOutput(outputBuffer, outputShape)
        val detections = parseDetections(transposed, outputShape, bitmap, ratio, xOffset, yOffset)

        Log.d(TAG, "Found ${detections.size} helmets before NMS")

        val nmsResult = applyNMS(detections)
        Log.d(TAG, "Found ${nmsResult.size} helmets after NMS")

        return nmsResult
    }

    private fun transposeOutput(
        outputBuffer: Array<Array<FloatArray>>,
        outputShape: IntArray
    ): Array<Array<FloatArray>> {
        val transposed = Array(1) {
            Array(outputShape[2]) {
                FloatArray(outputShape[1])
            }
        }

        for (i in 0 until outputShape[2]) {
            for (j in 0 until outputShape[1]) {
                transposed[0][i][j] = outputBuffer[0][j][i]
            }
        }

        return transposed
    }

    private fun parseDetections(
        transposed: Array<Array<FloatArray>>,
        outputShape: IntArray,
        originalBitmap: Bitmap,
        ratio: Float,
        xOffset: Float,
        yOffset: Float
    ): List<HelmetDetection> {
        val detections = mutableListOf<HelmetDetection>()

        for (i in 0 until outputShape[2]) {
            val confidence = transposed[0][i][4]

            if (confidence > CONF_THRESHOLD) {
                val centerX = transposed[0][i][0]
                val centerY = transposed[0][i][1]
                val width = transposed[0][i][2]
                val height = transposed[0][i][3]

                var x1 = centerX - width / 2
                var y1 = centerY - height / 2
                var x2 = centerX + width / 2
                var y2 = centerY + height / 2

                if (x1 > 1 || y1 > 1 || x2 > 1 || y2 > 1) {
                    x1 /= INPUT_SIZE
                    y1 /= INPUT_SIZE
                    x2 /= INPUT_SIZE
                    y2 /= INPUT_SIZE
                }

                val originalX1 = (x1 * INPUT_SIZE - xOffset) / ratio
                val originalY1 = (y1 * INPUT_SIZE - yOffset) / ratio
                val originalX2 = (x2 * INPUT_SIZE - xOffset) / ratio
                val originalY2 = (y2 * INPUT_SIZE - yOffset) / ratio

                val boundingBox = RectF(
                    max(0f, originalX1),
                    max(0f, originalY1),
                    min(originalBitmap.width.toFloat(), originalX2),
                    min(originalBitmap.height.toFloat(), originalY2)
                )

                if (boundingBox.width() > 5 && boundingBox.height() > 5) {
                    detections.add(
                        HelmetDetection(
                            boundingBox = boundingBox,
                            confidence = confidence
                        )
                    )
                }
            }
        }

        return detections
    }

    private fun applyNMS(detections: List<HelmetDetection>): List<HelmetDetection> {
        if (detections.isEmpty()) return emptyList()

        val sortedDetections = detections.sortedByDescending { it.confidence }
        val keep = BooleanArray(sortedDetections.size) { true }

        for (i in sortedDetections.indices) {
            if (!keep[i]) continue

            val boxA = sortedDetections[i].boundingBox

            for (j in i + 1 until sortedDetections.size) {
                if (!keep[j]) continue

                val boxB = sortedDetections[j].boundingBox
                if (calculateIoU(boxA, boxB) > NMS_THRESHOLD) {
                    keep[j] = false
                }
            }
        }

        return sortedDetections.filterIndexed { index, _ -> keep[index] }
    }
}