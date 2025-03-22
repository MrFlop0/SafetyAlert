package alex.kaplenkov.safetyalert.data.detector

import alex.kaplenkov.safetyalert.data.model.HelmetDetection
import alex.kaplenkov.safetyalert.utils.ImageUtils
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

class HelmetDetector(context: Context, modelPath: String) {
    companion object {
        private const val TAG = "HelmetDetector"
        private const val IMG_SIZE = 416
        private const val CONF_THRESHOLD = 0.25f
    }

    private val interpreter: Interpreter
    private val inputBuffer: ByteBuffer

    init {
        val options = Interpreter.Options()
        interpreter = Interpreter(FileUtil.loadMappedFile(context, modelPath), options)
        inputBuffer = ByteBuffer.allocateDirect(1 * IMG_SIZE * IMG_SIZE * 3 * 4) // Float32 = 4 bytes
        inputBuffer.order(ByteOrder.nativeOrder())
    }

    fun detect(bitmap: Bitmap): List<HelmetDetection> {
        // Preprocess image
        val preprocessedImage = ImageUtils.preprocessImage(bitmap, IMG_SIZE, IMG_SIZE)
        val (scaledBitmap, ratio, xOffset, yOffset) = preprocessedImage

        // Fill input buffer
        inputBuffer.rewind()
        val pixels = IntArray(scaledBitmap.width * scaledBitmap.height)
        scaledBitmap.getPixels(pixels, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)

        for (pixel in pixels) {
            // Extract RGB values and normalize to [0, 1]
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)          // B
        }

        // Prepare output buffer
        // For helmet model with shape [1, 5, 3549]
        val outputShape = intArrayOf(1, 5, 3549)
        val outputBuffer = Array(outputShape[0]) {
            Array(outputShape[1]) {
                FloatArray(outputShape[2])
            }
        }

        // Run inference
        interpreter.run(inputBuffer, outputBuffer)

        // Process outputs (format: [1, 5, 3549])
        // Transpose to [1, 3549, 5] for easier processing
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

        // Extract detections
        val detections = mutableListOf<HelmetDetection>()

        for (i in 0 until outputShape[2]) {
            val confidence = transposed[0][i][4]

            if (confidence > CONF_THRESHOLD) {
                // Check if the format is x1,y1,x2,y2 or xywh
                var x1: Float
                var y1: Float
                var x2: Float
                var y2: Float

                // Try to interpret as center_x, center_y, width, height
                val centerX = transposed[0][i][0]
                val centerY = transposed[0][i][1]
                val width = transposed[0][i][2]
                val height = transposed[0][i][3]

                // Convert from center format to corner format
                x1 = centerX - width/2
                y1 = centerY - height/2
                x2 = centerX + width/2
                y2 = centerY + height/2

                // Normalize coordinates to [0,1] if they aren't already
                if (x1 > 1 || y1 > 1 || x2 > 1 || y2 > 1) {
                    x1 /= IMG_SIZE
                    y1 /= IMG_SIZE
                    x2 /= IMG_SIZE
                    y2 /= IMG_SIZE
                }

                // Denormalize to original image coordinates
                val originalX1 = (x1 * IMG_SIZE - xOffset) / ratio
                val originalY1 = (y1 * IMG_SIZE - yOffset) / ratio
                val originalX2 = (x2 * IMG_SIZE - xOffset) / ratio
                val originalY2 = (y2 * IMG_SIZE - yOffset) / ratio

                // Create bounding box
                val boundingBox = RectF(
                    max(0f, originalX1),
                    max(0f, originalY1),
                    min(bitmap.width.toFloat(), originalX2),
                    min(bitmap.height.toFloat(), originalY2)
                )

                // Add to detections if width and height are positive and reasonable
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

        Log.d(TAG, "Found ${detections.size} helmets before NMS")

        // Apply non-maximum suppression
        val nmsResult = applyNMS(detections)
        Log.d(TAG, "Found ${nmsResult.size} helmets after NMS")
        return nmsResult
    }

    private fun applyNMS(detections: List<HelmetDetection>): List<HelmetDetection> {
        if (detections.isEmpty()) return emptyList()

        // Sort by confidence (descending)
        val sortedDetections = detections.sortedByDescending { it.confidence }
        val keep = BooleanArray(sortedDetections.size) { true }

        // NMS algorithm
        for (i in sortedDetections.indices) {
            if (!keep[i]) continue

            val boxA = sortedDetections[i].boundingBox

            for (j in i + 1 until sortedDetections.size) {
                if (!keep[j]) continue

                val boxB = sortedDetections[j].boundingBox
                if (calculateIoU(boxA, boxB) > 0.45f) {
                    keep[j] = false
                }
            }
        }

        return sortedDetections.filterIndexed { index, _ -> keep[index] }
    }

    private fun calculateIoU(boxA: RectF, boxB: RectF): Float {
        val xOverlap = max(0f, min(boxA.right, boxB.right) - max(boxA.left, boxB.left))
        val yOverlap = max(0f, min(boxA.bottom, boxB.bottom) - max(boxA.top, boxB.top))
        val intersection = xOverlap * yOverlap

        val areaA = (boxA.right - boxA.left) * (boxA.bottom - boxA.top)
        val areaB = (boxB.right - boxB.left) * (boxB.bottom - boxB.top)
        val union = areaA + areaB - intersection

        return if (union <= 0) 0f else intersection / union
    }

    fun close() {
        interpreter.close()
    }
}