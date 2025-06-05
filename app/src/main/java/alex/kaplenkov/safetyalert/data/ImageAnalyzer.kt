package alex.kaplenkov.safetyalert.data

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.domain.detector.DetectionManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class ImageAnalyzer(
    private val isProcessing: () -> Boolean,
    private val detectionManager: DetectionManager,
    private val callback: (DetectionResult?, Bitmap?) -> Unit,
) : ImageAnalysis.Analyzer {

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing()) {
            callback(null, null)
            imageProxy.close()
            return
        }


        val bitmap = imageProxyToBitmap(imageProxy)


        if (bitmap != null) {

            val originalWidth = bitmap.width
            val originalHeight = bitmap.height


            Log.d(TAG, "Processing image: $originalWidth x $originalHeight")


            val result = detectionManager.runDetection(bitmap)

            callback(result, bitmap)
        } else {
            callback(null, null)
        }

        imageProxy.close()

    }

    @androidx.camera.core.ExperimentalGetImage
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null


        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null
        )

        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, image.width, image.height), 100, out
        )

        val imageBytes = out.toByteArray()
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    companion object {
        private const val TAG = "ImageAnalyzer"
    }
}