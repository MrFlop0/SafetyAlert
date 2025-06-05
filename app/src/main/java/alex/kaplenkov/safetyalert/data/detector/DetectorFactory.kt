package alex.kaplenkov.safetyalert.data.detector

import alex.kaplenkov.safetyalert.data.model.ViolationType
import alex.kaplenkov.safetyalert.domain.detector.Detector
import android.content.Context

interface DetectorFactory {
    fun createHelmetDetector(): Detector<*>
    fun createPoseDetector(): Detector<*>
    fun createStairSafetyDetector(): Detector<*>
    fun createDetectorForType(type: ViolationType): Detector<*>?
}

class DetectorFactoryImpl(private val context: Context) : DetectorFactory {

    companion object {
        private const val POSE_MODEL_PATH = "yolov11n_pose.tflite"
        private const val HELMET_MODEL_PATH = "helmet_detection.tflite"
        private const val STAIR_SAFETY_MODEL_PATH = "stair_safety_detection.tflite"
    }

    override fun createHelmetDetector(): Detector<*> {
        return HelmetDetectorImpl(context, HELMET_MODEL_PATH)
    }

    override fun createPoseDetector(): Detector<*> {
        return PoseDetectorImpl(context, POSE_MODEL_PATH)
    }

    override fun createStairSafetyDetector(): Detector<*> {
        return StairSafetyDetectorImpl(context, STAIR_SAFETY_MODEL_PATH)
    }

    override fun createDetectorForType(type: ViolationType): Detector<*>? {
        return when (type) {
            ViolationType.HELMET -> createHelmetDetector()
            ViolationType.HANDRAIL -> createStairSafetyDetector()
            else -> null
        }
    }
}