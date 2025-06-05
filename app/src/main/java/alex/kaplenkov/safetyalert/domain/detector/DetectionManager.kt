package alex.kaplenkov.safetyalert.domain.detector

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.data.model.ViolationType
import android.graphics.Bitmap

interface DetectionManager {
    fun setDetectionType(type: ViolationType)
    fun runDetection(bitmap: Bitmap): DetectionResult
    fun close()
}