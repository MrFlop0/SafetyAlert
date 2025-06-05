package alex.kaplenkov.safetyalert.data.detector

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.data.model.HelmetDetection
import alex.kaplenkov.safetyalert.data.model.ViolationType
import alex.kaplenkov.safetyalert.domain.detector.DetectionManager
import alex.kaplenkov.safetyalert.domain.detector.DetectorWithHeatmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import kotlin.math.max
import kotlin.math.min

class DetectionManagerImpl(context: Context) : DetectionManager {

    companion object {
        private const val TAG = "DetectionManager"
        private const val IOU_THRESHOLD = 0.25f
    }

    private val detectorFactory = DetectorFactoryImpl(context)

    private val poseDetector = detectorFactory.createPoseDetector() as PoseDetectorImpl

    private val helmetDetector = detectorFactory.createHelmetDetector() as HelmetDetectorImpl

    private val stairSafetyDetector = detectorFactory.createStairSafetyDetector() as StairSafetyDetectorImpl

    private var activeDetectionType: ViolationType = ViolationType.HELMET

    override fun setDetectionType(type: ViolationType) {
        activeDetectionType = type
        if (type == ViolationType.HANDRAIL) {
            stairSafetyDetector.clear()
        }
    }

    override fun runDetection(bitmap: Bitmap): DetectionResult {
        return when (activeDetectionType) {
            ViolationType.HELMET -> detectHelmets(bitmap)
            ViolationType.HANDRAIL -> detectHandrailSafety(bitmap)
            else -> DetectionResult(detectionType = activeDetectionType)
        }
    }

    private fun detectHelmets(bitmap: Bitmap): DetectionResult {
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

                person.copy(hasHelmet = hasHelmet)
            } else {
                person
            }
        }

        val processingTime = System.currentTimeMillis() - startTime

        return DetectionResult(
            personDetections = updatedPersonDetections,
            helmetDetections = helmetDetections,
            detectionType = ViolationType.HELMET,
            processingTimeMs = processingTime,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )
    }

    private fun detectHandrailSafety(bitmap: Bitmap): DetectionResult {
        val startTime = System.currentTimeMillis()

        val personDetections = poseDetector.detect(bitmap)
        val poseHeatmap = (poseDetector as? DetectorWithHeatmap)?.getLastHeatmapFlattened()

        var holdsHandrail = false
        var handrailScore = 0.0f
        var unsafeScore = 0.0f

        if (poseHeatmap != null) {
            stairSafetyDetector.addPoseHeatmap(poseHeatmap)

            if (stairSafetyDetector.hasSufficientData()) {
                val (safeScore, unsafe) = stairSafetyDetector.detect(bitmap)
                handrailScore = safeScore
                unsafeScore = unsafe

                val status = stairSafetyDetector.isHandrailSafe(Pair(safeScore, unsafe))
                holdsHandrail = status == StairSafetyDetectorImpl.HandrailStatus.SAFE

                Log.d(TAG, "Stair safety status: $status (safe: $safeScore, unsafe: $unsafe)")
            }
        }

        val updatedPersonDetections = personDetections.map { person ->
            person.copy(holdsHandrail = holdsHandrail)
        }

        val processingTime = System.currentTimeMillis() - startTime

        return DetectionResult(
            personDetections = updatedPersonDetections,
            holdsHandrail = holdsHandrail,
            handrailScore = handrailScore,
            unsafeScore = unsafeScore,
            detectionType = ViolationType.HANDRAIL,
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

    override fun close() {
        poseDetector.close()
        helmetDetector.close()
        stairSafetyDetector.close()
    }
}