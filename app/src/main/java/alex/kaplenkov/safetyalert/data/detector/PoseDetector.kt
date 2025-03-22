package alex.kaplenkov.safetyalert.data.detector

import alex.kaplenkov.safetyalert.data.model.Keypoint
import alex.kaplenkov.safetyalert.data.model.KeypointType
import alex.kaplenkov.safetyalert.data.model.PersonDetection
import alex.kaplenkov.safetyalert.data.model.Point
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

class PoseDetector(context: Context, modelPath: String) {
    companion object {
        private const val TAG = "PoseDetector"
        private const val IMG_SIZE = 640
        private const val CONF_THRESHOLD = 0.25f
        private const val KEYPOINT_THRESHOLD = 0.2f
        private const val MIN_KEYPOINTS = 3
        private const val NUM_KEYPOINTS = 17
    }

    private val interpreter: Interpreter
    private val inputBuffer: ByteBuffer

    init {
        val options = Interpreter.Options()
        interpreter = Interpreter(FileUtil.loadMappedFile(context, modelPath), options)
        inputBuffer = ByteBuffer.allocateDirect(1 * IMG_SIZE * IMG_SIZE * 3 * 4) // Float32 = 4 bytes
        inputBuffer.order(ByteOrder.nativeOrder())
    }

    fun detect(bitmap: Bitmap): List<PersonDetection> {
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
        // Format: [1, 56, 8400] where 56 = 4 (bbox) + 1 (confidence) + 51 (keypoints: 17 * 3)
        val outputShape = intArrayOf(1, 56, 8400)
        val outputBuffer = Array(outputShape[0]) {
            Array(outputShape[1]) {
                FloatArray(outputShape[2])
            }
        }

        // Run inference
        interpreter.run(inputBuffer, outputBuffer)

        // Process results
        val detections = mutableListOf<PersonDetection>()
        val confidences = outputBuffer[0][4] // Confidence scores

        // Find detections above threshold
        for (i in confidences.indices) {
            if (confidences[i] > CONF_THRESHOLD) {
                // Extract bounding box - these are normalized coordinates [0-1]
                val x1 = outputBuffer[0][0][i]
                val y1 = outputBuffer[0][1][i]
                val x2 = outputBuffer[0][2][i]
                val y2 = outputBuffer[0][3][i]

                // Convert to original image coordinates
                val originalX1 = (x1 * IMG_SIZE - xOffset) / ratio
                val originalY1 = (y1 * IMG_SIZE - yOffset) / ratio
                val originalX2 = (x2 * IMG_SIZE - xOffset) / ratio
                val originalY2 = (y2 * IMG_SIZE - yOffset) / ratio

                // Extract keypoints
                val keypoints = mutableListOf<Keypoint>()
                var validKeypointsCount = 0

                for (k in 0 until NUM_KEYPOINTS) {
                    val baseIdx = 5 + k * 3
                    val kpX = outputBuffer[0][baseIdx][i]
                    val kpY = outputBuffer[0][baseIdx + 1][i]
                    val kpConf = outputBuffer[0][baseIdx + 2][i]

                    // Convert to original image coordinates
                    val originalKpX = (kpX * IMG_SIZE - xOffset) / ratio
                    val originalKpY = (kpY * IMG_SIZE - yOffset) / ratio

                    keypoints.add(
                        Keypoint(
                            position = Point(originalKpX, originalKpY),
                            confidence = kpConf,
                            type = KeypointType.fromId(k)
                        )
                    )

                    if (kpConf > KEYPOINT_THRESHOLD) {
                        validKeypointsCount++
                    }
                }

                // Only add detection if it has enough valid keypoints
                if (validKeypointsCount >= MIN_KEYPOINTS) {
                    val bbox = RectF(originalX1, originalY1, originalX2, originalY2)
                    val improvedBbox = calculateImprovedBoundingBox(keypoints, bitmap.width, bitmap.height)
                    val headBbox = calculateHeadBoundingBox(keypoints, bitmap.width, bitmap.height)

                    detections.add(
                        PersonDetection(
                            boundingBox = improvedBbox ?: bbox,
                            confidence = confidences[i],
                            keypoints = keypoints,
                            headBoundingBox = headBbox
                        )
                    )
                }
            }
        }

        Log.d(TAG, "Found ${detections.size} people before NMS")

        // Apply non-maximum suppression
        val result = applyNMS(detections)
        Log.d(TAG, "Found ${result.size} people after NMS")
        return result
    }

    private fun calculateImprovedBoundingBox(keypoints: List<Keypoint>, imageWidth: Int, imageHeight: Int): RectF? {
        // Get all visible keypoints (above threshold)
        val visibleKeypoints = keypoints.filter { it.confidence > KEYPOINT_THRESHOLD }

        if (visibleKeypoints.isEmpty()) return null

        // Find min/max coordinates
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = 0f
        var maxY = 0f

        visibleKeypoints.forEach { keypoint ->
            minX = min(minX, keypoint.position.x)
            minY = min(minY, keypoint.position.y)
            maxX = max(maxX, keypoint.position.x)
            maxY = max(maxY, keypoint.position.y)
        }

        // Calculate dimensions
        val width = maxX - minX
        val height = maxY - minY

        // Center of the bounding box
        val centerX = (minX + maxX) / 2
        val centerY = (minY + maxY) / 2

        // Check aspect ratio and adjust if needed
        val aspectRatio = height / width

        // For a person, aspect ratio is typically ~2:1 (height:width)
        val targetAspectRatio = 2.0f

        if (aspectRatio < targetAspectRatio) {
            // Too wide, increase height
            val newHeight = width * targetAspectRatio
            minY = centerY - newHeight / 2
            maxY = centerY + newHeight / 2
        } else if (aspectRatio > 3.0f) {
            // Too tall, increase width
            val newWidth = height / targetAspectRatio
            minX = centerX - newWidth / 2
            maxX = centerX + newWidth / 2
        }

        // Add padding for better detection
        val paddingX = width * 0.2f
        val paddingY = height * 0.2f

        return RectF(
            max(0f, minX - paddingX),
            max(0f, minY - paddingY),
            min(imageWidth.toFloat(), maxX + paddingX),
            min(imageHeight.toFloat(), maxY + paddingY)
        )
    }

    private fun calculateHeadBoundingBox(keypoints: List<Keypoint>, imageWidth: Int, imageHeight: Int): RectF? {
        // Get head keypoints (nose, eyes, ears)
        val headKeypoints = keypoints.filter {
            KeypointType.HEAD_KEYPOINTS.contains(it.type) && it.confidence > KEYPOINT_THRESHOLD
        }

        if (headKeypoints.isEmpty()) return null

        // Find min/max coordinates
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = 0f
        var maxY = 0f

        headKeypoints.forEach { keypoint ->
            minX = min(minX, keypoint.position.x)
            minY = min(minY, keypoint.position.y)
            maxX = max(maxX, keypoint.position.x)
            maxY = max(maxY, keypoint.position.y)
        }

        // Calculate dimensions
        val width = maxX - minX
        val height = maxY - minY

        // Apply larger padding for head to catch helmets better
        val paddingX = width * 1.0f  // Increased from 0.5f
        val paddingY = height * 0.8f // Increased from 0.5f

        // Ensure head box has appropriate aspect ratio
        // For head detection, we want a more square-like box
        if (height < width) {
            // Stretch height
            val centerY = (minY + maxY) / 2
            minY = centerY - width / 2
            maxY = centerY + width / 2
        }

        // Apply padding and clamp to image bounds
        return RectF(
            max(0f, minX - paddingX),
            max(0f, minY - paddingY),
            min(imageWidth.toFloat(), maxX + paddingX),
            min(imageHeight.toFloat(), maxY + paddingY)
        )
    }

    private fun applyNMS(detections: List<PersonDetection>): List<PersonDetection> {
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