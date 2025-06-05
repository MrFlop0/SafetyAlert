package alex.kaplenkov.safetyalert.domain.detector

import android.graphics.Bitmap

interface Detector<T> {
    fun detect(bitmap: Bitmap): T
    fun close()
}