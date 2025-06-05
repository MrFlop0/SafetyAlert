package alex.kaplenkov.safetyalert.domain.detector

import android.graphics.Bitmap

interface SequentialDetector<T> : Detector<T> {
    fun addFrame(bitmap: Bitmap)
    fun hasSufficientData(): Boolean
    fun clear()
}