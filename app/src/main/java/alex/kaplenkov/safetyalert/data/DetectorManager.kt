package alex.kaplenkov.safetyalert.data

import alex.kaplenkov.safetyalert.data.detector.HelmetDetector
import alex.kaplenkov.safetyalert.data.detector.PoseDetector
import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.data.model.HelmetDetection
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import kotlin.math.max
import kotlin.math.min

class DetectionManager(context: Context) {

    companion object {
        private const val TAG = "DetectionManager"
        private const val POSE_MODEL_PATH = "yolov11n_pose.tflite"
        private const val HELMET_MODEL_PATH = "helmet_detection.tflite"
        private const val IOU_THRESHOLD = 0.25f
    }

    private val poseDetector: PoseDetector = PoseDetector(context, POSE_MODEL_PATH)
    private val helmetDetector: HelmetDetector = HelmetDetector(context, HELMET_MODEL_PATH)

    fun runDetection(bitmap: Bitmap): DetectionResult {
        val startTime = System.currentTimeMillis()

         
        val personDetections = poseDetector.detect(bitmap)

         
        val helmetDetections = helmetDetector.detect(bitmap).toMutableList()

        Log.d(TAG, "Found ${personDetections.size} people and ${helmetDetections.size} helmets")

         
        val updatedPersonDetections = personDetections.map { person ->
            val headBbox = person.headBoundingBox
            if (headBbox != null) {
                val (hasHelmet, helmetIndex) = checkHelmet(headBbox, helmetDetections)

                if (helmetIndex >= 0) {
                    helmetDetections[helmetIndex] = helmetDetections[helmetIndex].copy(isAssociated = true)
                }

                Log.d(TAG, "Person has helmet: $hasHelmet (IoU match: ${helmetIndex >= 0})")
                person.copy(hasHelmet = hasHelmet)
            } else {
                Log.d(TAG, "Person has no detected head")
                person
            }
        }

        val processingTime = System.currentTimeMillis() - startTime

        return DetectionResult(
            personDetections = updatedPersonDetections,
            helmetDetections = helmetDetections,
            processingTimeMs = processingTime,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )
    }

    private fun checkHelmet(headBbox: RectF, helmetDetections: List<HelmetDetection>): Pair<Boolean, Int> {
        if (helmetDetections.isEmpty()) return Pair(false, -1)

        var maxIoU = 0f
        var bestHelmetIdx = -1

        helmetDetections.forEachIndexed { index, helmet ->
            val iou = calculateIoU(headBbox, helmet.boundingBox)

            if (iou > maxIoU) {
                maxIoU = iou
                bestHelmetIdx = index
            }
        }

        val hasHelmet = maxIoU > IOU_THRESHOLD || checkOverlap(headBbox,
            helmetDetections.getOrNull(bestHelmetIdx)?.boundingBox)

        return Pair(hasHelmet, bestHelmetIdx)
    }

     
    private fun checkOverlap(headBox: RectF, helmetBox: RectF?): Boolean {
        if (helmetBox == null) return false

        return !(headBox.right < helmetBox.left ||
                headBox.left > helmetBox.right ||
                headBox.bottom < helmetBox.top ||
                headBox.top > helmetBox.bottom)
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
        poseDetector.close()
        helmetDetector.close()
    }
}