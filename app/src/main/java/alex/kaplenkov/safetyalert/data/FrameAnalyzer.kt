package alex.kaplenkov.safetyalert.data

import alex.kaplenkov.safetyalert.R
import android.content.Context
import android.media.MediaPlayer
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class FrameAnalyzer(private val context: Context, private val isPaused: () -> Boolean) : ImageAnalysis.Analyzer {
    private var frameCount = 0
    private var mediaPlayer: MediaPlayer? = null

    override fun analyze(image: ImageProxy) {
        if (isPaused()) {
            image.close()
            return
        }

        frameCount++
        if (frameCount % 100 == 0) {
//            val buffer = image.planes[0].buffer
//            val data = ByteArray(buffer.remaining())
//            buffer.get(data)

            playBeepSound()
            //TODO(add analysis by ml model)
        }
        image.close()
    }

    private fun playBeepSound() {
        mediaPlayer?.release() // Release any existing MediaPlayer instance
        mediaPlayer = MediaPlayer.create(context, R.raw.beep)
        mediaPlayer?.start()
    }
}