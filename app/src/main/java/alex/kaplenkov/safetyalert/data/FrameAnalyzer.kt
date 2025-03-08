package alex.kaplenkov.safetyalert.data

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class FrameAnalyzer(private val isPaused: () -> Boolean) : ImageAnalysis.Analyzer {
    private var frameCount = 0

    override fun analyze(image: ImageProxy) {
        if (isPaused()) {
            image.close()
            return
        }

        frameCount++
        if (frameCount % 5 == 0) {
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            //TODO(add analysis by ml model)
        }
        image.close()
    }
}