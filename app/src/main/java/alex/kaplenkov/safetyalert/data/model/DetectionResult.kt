package alex.kaplenkov.safetyalert.data.model

import android.graphics.RectF

data class DetectionResult(
    val personDetections: List<PersonDetection>,
    val helmetDetections: List<HelmetDetection>,
    val processingTimeMs: Long,
    val imageWidth: Int,
    val imageHeight: Int
)

data class PersonDetection(
    val boundingBox: RectF,
    val confidence: Float,
    val keypoints: List<Keypoint>,
    val hasHelmet: Boolean = false,
    val headBoundingBox: RectF? = null
)

data class HelmetDetection(
    val boundingBox: RectF,
    val confidence: Float,
    val isAssociated: Boolean = false
)

data class Keypoint(
    val position: Point,
    val confidence: Float,
    val type: KeypointType
)

data class Point(
    val x: Float,
    val y: Float
)

enum class KeypointType(val id: Int) {
    NOSE(0),
    LEFT_EYE(1),
    RIGHT_EYE(2),
    LEFT_EAR(3),
    RIGHT_EAR(4),
    LEFT_SHOULDER(5),
    RIGHT_SHOULDER(6),
    LEFT_ELBOW(7),
    RIGHT_ELBOW(8),
    LEFT_WRIST(9),
    RIGHT_WRIST(10),
    LEFT_HIP(11),
    RIGHT_HIP(12),
    LEFT_KNEE(13),
    RIGHT_KNEE(14),
    LEFT_ANKLE(15),
    RIGHT_ANKLE(16);

    companion object {
        fun fromId(id: Int): KeypointType = entries.first { it.id == id }

        val HEAD_KEYPOINTS = listOf(NOSE, LEFT_EYE, RIGHT_EYE, LEFT_EAR, RIGHT_EAR)

        val POSE_PAIRS = listOf(
            // Face
            Pair(NOSE, LEFT_EYE), Pair(NOSE, RIGHT_EYE),
            Pair(LEFT_EYE, LEFT_EAR), Pair(RIGHT_EYE, RIGHT_EAR),
            // Arms
            Pair(LEFT_SHOULDER, RIGHT_SHOULDER),
            Pair(LEFT_SHOULDER, LEFT_ELBOW), Pair(LEFT_ELBOW, LEFT_WRIST),
            Pair(RIGHT_SHOULDER, RIGHT_ELBOW), Pair(RIGHT_ELBOW, RIGHT_WRIST),
            // Body
            Pair(LEFT_SHOULDER, LEFT_HIP), Pair(RIGHT_SHOULDER, RIGHT_HIP),
            Pair(LEFT_HIP, RIGHT_HIP),
            // Legs
            Pair(LEFT_HIP, LEFT_KNEE), Pair(LEFT_KNEE, LEFT_ANKLE),
            Pair(RIGHT_HIP, RIGHT_KNEE), Pair(RIGHT_KNEE, RIGHT_ANKLE)
        )
    }
}