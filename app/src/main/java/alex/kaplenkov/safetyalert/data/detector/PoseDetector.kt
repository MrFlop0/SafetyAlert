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
import kotlin.math.exp
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
    private var lastPoseHeatmap: Array<Array<FloatArray>>? = null

    init {
        val options = Interpreter.Options()
        interpreter = Interpreter(FileUtil.loadMappedFile(context, modelPath), options)
        inputBuffer = ByteBuffer.allocateDirect(1 * IMG_SIZE * IMG_SIZE * 3 * 4)  
        inputBuffer.order(ByteOrder.nativeOrder())
    }

    fun detect(bitmap: Bitmap): List<PersonDetection> {
         
        val preprocessedImage = ImageUtils.preprocessImage(bitmap, IMG_SIZE, IMG_SIZE)
        val (scaledBitmap, ratio, xOffset, yOffset) = preprocessedImage

         
        inputBuffer.rewind()
        val pixels = IntArray(scaledBitmap.width * scaledBitmap.height)
        scaledBitmap.getPixels(pixels, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)

        for (pixel in pixels) {
             
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)  
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)   
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)           
        }

         
         
        val outputShape = intArrayOf(1, 56, 8400)
        val outputBuffer = Array(outputShape[0]) {
            Array(outputShape[1]) {
                FloatArray(outputShape[2])
            }
        }

         
        interpreter.run(inputBuffer, outputBuffer)

        lastPoseHeatmap = outputBuffer
        val detections = mutableListOf<PersonDetection>()
        val confidences = outputBuffer[0][4]  

         
        for (i in confidences.indices) {
            if (confidences[i] > CONF_THRESHOLD) {
                 
                val x1 = outputBuffer[0][0][i]
                val y1 = outputBuffer[0][1][i]
                val x2 = outputBuffer[0][2][i]
                val y2 = outputBuffer[0][3][i]

                 
                val originalX1 = (x1 * IMG_SIZE - xOffset) / ratio
                val originalY1 = (y1 * IMG_SIZE - yOffset) / ratio
                val originalX2 = (x2 * IMG_SIZE - xOffset) / ratio
                val originalY2 = (y2 * IMG_SIZE - yOffset) / ratio

                 
                val keypoints = mutableListOf<Keypoint>()
                var validKeypointsCount = 0

                for (k in 0 until NUM_KEYPOINTS) {
                    val baseIdx = 5 + k * 3
                    val kpX = outputBuffer[0][baseIdx][i]
                    val kpY = outputBuffer[0][baseIdx + 1][i]
                    val kpConf = outputBuffer[0][baseIdx + 2][i]

                     
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

         
        val result = applyNMS(detections)
        Log.d(TAG, "Found ${result.size} people after NMS")
        return result
    }

    private fun calculateImprovedBoundingBox(keypoints: List<Keypoint>, imageWidth: Int, imageHeight: Int): RectF? {
         
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

    private fun calculateHeadBoundingBox(keypoints: List<Keypoint>, imageWidth: Int, imageHeight: Int): RectF? {
         
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
                if (calculateIoU(boxA, boxB) > 0.45f) {
                    keep[j] = false
                }
            }
        }

        return sortedDetections.filterIndexed { index, _ -> keep[index] }
    }

    fun getLastPoseHeatmap(): Array<Array<FloatArray>>? {
        return lastPoseHeatmap
    }

    /**
     * Этот метод преобразует выходные данные YOLOv11n_pose (1 x 56 x 8400)
     * в формат, подходящий для модели stair-safety (64 x 48 x 17)
     */
    fun getLastPoseHeatmapFlattened(): FloatArray? {
        if (lastPoseHeatmap == null) {
            Log.e(TAG, "lastPoseHeatmap is null")
            return null
        }

        try {
            Log.d(TAG, "Converting pose heatmap with dimensions: ${lastPoseHeatmap!!.size} x ${lastPoseHeatmap!![0].size} x ${lastPoseHeatmap!![0][0].size}")

            // Создаем искусственный heatmap с нужными размерами (64 x 48 x 17)
            val heatmapHeight = 64
            val heatmapWidth = 48
            val numKeypoints = 17
            val result = FloatArray(heatmapHeight * heatmapWidth * numKeypoints)

            // Простая логика преобразования: экстракция данных ключевых точек из выхода YOLO
            // Индексы 5-56 в выходе YOLO содержат информацию о ключевых точках (17 точек x 3 значения)
            // В нашем случае у нас [1, 56, 8400] - значит, у нас данные для 8400 потенциальных объектов

            // Находим объект с наивысшим confidence
            val batch = 0
            var bestObjectIndex = -1
            var maxConfidence = 0.0f

            // YOLO output format: [batch, 4+1+51, num_objects]
            // Индекс 4 - это confidence score для объекта
            for (i in 0 until lastPoseHeatmap!![batch][0].size) {
                val confidence = lastPoseHeatmap!![batch][4][i]
                if (confidence > maxConfidence) {
                    maxConfidence = confidence
                    bestObjectIndex = i
                }
            }

            Log.d(TAG, "Best object index: $bestObjectIndex with confidence: $maxConfidence")

            if (bestObjectIndex >= 0) {
                // Извлекаем данные ключевых точек для лучшего объекта
                // Преобразуем в формат (64 x 48 x 17)
                var resultIndex = 0

                // Создаем простую карту активации на основе ключевых точек
                for (h in 0 until heatmapHeight) {
                    for (w in 0 until heatmapWidth) {
                        for (k in 0 until numKeypoints) {
                            // База - индекс начала данных ключевых точек (после bbox+confidence)
                            val baseIdx = 5 + k * 3

                            // Координаты ключевой точки (x, y) и её confidence
                            val kpX = lastPoseHeatmap!![batch][baseIdx][bestObjectIndex]     // x
                            val kpY = lastPoseHeatmap!![batch][baseIdx + 1][bestObjectIndex] // y
                            val kpConf = lastPoseHeatmap!![batch][baseIdx + 2][bestObjectIndex] // confidence

                            // Нормализуем координаты к размеру heatmap
                            val normX = kpX * heatmapWidth
                            val normY = kpY * heatmapHeight

                            // Создаем гауссово распределение вокруг точки
                            val sigma = 1.0f
                            val dx = w - normX
                            val dy = h - normY
                            val distance = dx * dx + dy * dy

                            // Значение в текущей точке heatmap
                            val value = kpConf * exp((-distance / (2 * sigma * sigma)).toDouble()).toFloat()

                            result[resultIndex++] = value
                        }
                    }
                }

                // Логируем статистику результата
                var sum = 0.0f
                var max = Float.MIN_VALUE
                var min = Float.MAX_VALUE
                for (value in result) {
                    sum += value
                    max = max.coerceAtLeast(value)
                    min = min.coerceAtMost(value)
                }

                Log.d(TAG, "Created heatmap with stats - Size: ${result.size}, Min: $min, Max: $max, Mean: ${sum / result.size}")

                return result
            } else {
                Log.w(TAG, "No valid objects detected")
            }

            Log.d(TAG, "Returning empty heatmap")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing heatmap for stair safety: ${e.message}")
            e.printStackTrace()
            return null
        }
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