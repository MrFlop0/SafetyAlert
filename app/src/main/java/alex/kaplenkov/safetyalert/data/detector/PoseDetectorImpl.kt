package alex.kaplenkov.safetyalert.data.detector

import alex.kaplenkov.safetyalert.data.detector.base.BaseTensorFlowDetector
import alex.kaplenkov.safetyalert.data.model.*
import alex.kaplenkov.safetyalert.domain.detector.DetectorWithHeatmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class PoseDetectorImpl(
    context: Context,
    modelPath: String
) : BaseTensorFlowDetector<List<PersonDetection>>(context, modelPath, INPUT_SIZE),
    DetectorWithHeatmap {

    companion object {
        private const val TAG = "PoseDetector"
        private const val INPUT_SIZE = 640
        private const val CONF_THRESHOLD = 0.25f
        private const val KEYPOINT_THRESHOLD = 0.2f
        private const val MIN_KEYPOINTS = 3
        private const val NUM_KEYPOINTS = 17
        private const val NMS_THRESHOLD = 0.45f
    }

    private var lastPoseHeatmap: Array<Array<FloatArray>>? = null

    override fun detect(bitmap: Bitmap): List<PersonDetection> {
        val (scaledBitmap, ratio, offsets) = preprocessImage(bitmap)
        val (xOffset, yOffset) = offsets

        fillInputBuffer(scaledBitmap)

        val outputShape = intArrayOf(1, 56, 8400)
        val outputBuffer = Array(outputShape[0]) {
            Array(outputShape[1]) {
                FloatArray(outputShape[2])
            }
        }

        interpreter.run(inputBuffer, outputBuffer)

        lastPoseHeatmap = outputBuffer
        val detections = parseDetections(outputBuffer, bitmap, ratio, xOffset, yOffset)

        Log.d(TAG, "Found ${detections.size} people before NMS")

        val result = applyNMS(detections)
        Log.d(TAG, "Found ${result.size} people after NMS")

        return result
    }

    override fun getLastHeatmap(): Array<Array<FloatArray>>? = lastPoseHeatmap

    override fun getLastHeatmapFlattened(): FloatArray? {
        // Ваша существующая логика преобразования heatmap
        return convertHeatmapToStairSafetyFormat()
    }

    private fun parseDetections(
        outputBuffer: Array<Array<FloatArray>>,
        originalBitmap: Bitmap,
        ratio: Float,
        xOffset: Float,
        yOffset: Float
    ): List<PersonDetection> {
        val detections = mutableListOf<PersonDetection>()
        val confidences = outputBuffer[0][4]

        for (i in confidences.indices) {
            if (confidences[i] > CONF_THRESHOLD) {
                val x1 = outputBuffer[0][0][i]
                val y1 = outputBuffer[0][1][i]
                val x2 = outputBuffer[0][2][i]
                val y2 = outputBuffer[0][3][i]

                val originalX1 = (x1 * INPUT_SIZE - xOffset) / ratio
                val originalY1 = (y1 * INPUT_SIZE - yOffset) / ratio
                val originalX2 = (x2 * INPUT_SIZE - xOffset) / ratio
                val originalY2 = (y2 * INPUT_SIZE - yOffset) / ratio

                val keypoints = parseKeypoints(outputBuffer, i, ratio, xOffset, yOffset)
                val validKeypointsCount = keypoints.count { it.confidence > KEYPOINT_THRESHOLD }

                if (validKeypointsCount >= MIN_KEYPOINTS) {
                    val bbox = RectF(originalX1, originalY1, originalX2, originalY2)
                    val improvedBbox = calculateImprovedBoundingBox(
                        keypoints,
                        originalBitmap.width,
                        originalBitmap.height
                    )
                    val headBbox = calculateHeadBoundingBox(
                        keypoints,
                        originalBitmap.width,
                        originalBitmap.height
                    )

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

        return detections
    }

    private fun parseKeypoints(
        outputBuffer: Array<Array<FloatArray>>,
        objectIndex: Int,
        ratio: Float,
        xOffset: Float,
        yOffset: Float
    ): List<Keypoint> {
        val keypoints = mutableListOf<Keypoint>()

        for (k in 0 until NUM_KEYPOINTS) {
            val baseIdx = 5 + k * 3
            val kpX = outputBuffer[0][baseIdx][objectIndex]
            val kpY = outputBuffer[0][baseIdx + 1][objectIndex]
            val kpConf = outputBuffer[0][baseIdx + 2][objectIndex]

            val originalKpX = (kpX * INPUT_SIZE - xOffset) / ratio
            val originalKpY = (kpY * INPUT_SIZE - yOffset) / ratio

            keypoints.add(
                Keypoint(
                    position = Point(originalKpX, originalKpY),
                    confidence = kpConf,
                    type = KeypointType.fromId(k)
                )
            )
        }

        return keypoints
    }

    private fun calculateImprovedBoundingBox(
        keypoints: List<Keypoint>,
        imageWidth: Int,
        imageHeight: Int
    ): RectF? {
        // Ваша существующая логика
        val visibleKeypoints = keypoints.filter { it.confidence > KEYPOINT_THRESHOLD }
        if (visibleKeypoints.isEmpty()) return null

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

        val width = maxX - minX
        val height = maxY - minY
        val centerX = (minX + maxX) / 2
        val centerY = (minY + maxY) / 2

        val aspectRatio = height / width
        val targetAspectRatio = 2.0f

        if (aspectRatio < targetAspectRatio) {
            val newHeight = width * targetAspectRatio
            minY = centerY - newHeight / 2
            maxY = centerY + newHeight / 2
        } else if (aspectRatio > 3.0f) {
            val newWidth = height / targetAspectRatio
            minX = centerX - newWidth / 2
            maxX = centerX + newWidth / 2
        }

        val paddingX = width * 0.2f
        val paddingY = height * 0.2f

        return RectF(
            max(0f, minX - paddingX),
            max(0f, minY - paddingY),
            min(imageWidth.toFloat(), maxX + paddingX),
            min(imageHeight.toFloat(), maxY + paddingY)
        )
    }

    private fun calculateHeadBoundingBox(
        keypoints: List<Keypoint>,
        imageWidth: Int,
        imageHeight: Int
    ): RectF? {
        // Ваша существующая логика
        val headKeypoints = keypoints.filter {
            KeypointType.HEAD_KEYPOINTS.contains(it.type) && it.confidence > KEYPOINT_THRESHOLD
        }

        if (headKeypoints.isEmpty()) return null

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

        val width = maxX - minX
        val height = maxY - minY
        val paddingX = width * 1.0f
        val paddingY = height * 0.8f

        if (height < width) {
            val centerY = (minY + maxY) / 2
            minY = centerY - width / 2
            maxY = centerY + width / 2
        }

        return RectF(
            max(0f, minX - paddingX),
            max(0f, minY - paddingY),
            min(imageWidth.toFloat(), maxX + paddingX),
            min(imageHeight.toFloat(), maxY + paddingY)
        )
    }

    private fun convertHeatmapToStairSafetyFormat(): FloatArray? {
        if (lastPoseHeatmap == null) {
            Log.e(TAG, "lastPoseHeatmap is null")
            return null
        }

        try {
            val heatmapHeight = 64
            val heatmapWidth = 48
            val numKeypoints = 17
            val result = FloatArray(heatmapHeight * heatmapWidth * numKeypoints)

            val batch = 0
            var bestObjectIndex = -1
            var maxConfidence = 0.0f

            for (i in 0 until lastPoseHeatmap!![batch][0].size) {
                val confidence = lastPoseHeatmap!![batch][4][i]
                if (confidence > maxConfidence) {
                    maxConfidence = confidence
                    bestObjectIndex = i
                }
            }

            if (bestObjectIndex >= 0) {
                var resultIndex = 0

                for (h in 0 until heatmapHeight) {
                    for (w in 0 until heatmapWidth) {
                        for (k in 0 until numKeypoints) {
                            val baseIdx = 5 + k * 3
                            val kpX = lastPoseHeatmap!![batch][baseIdx][bestObjectIndex]
                            val kpY = lastPoseHeatmap!![batch][baseIdx + 1][bestObjectIndex]
                            val kpConf = lastPoseHeatmap!![batch][baseIdx + 2][bestObjectIndex]

                            val normX = kpX * heatmapWidth
                            val normY = kpY * heatmapHeight

                            val sigma = 1.0f
                            val dx = w - normX
                            val dy = h - normY
                            val distance = dx * dx + dy * dy

                            val value =
                                kpConf * exp((-distance / (2 * sigma * sigma)).toDouble()).toFloat()
                            result[resultIndex++] = value
                        }
                    }
                }

                return result
            }

            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing heatmap for stair safety: ${e.message}")
            return null
        }
    }

    private fun applyNMS(detections: List<PersonDetection>): List<PersonDetection> {
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