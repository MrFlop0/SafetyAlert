package alex.kaplenkov.safetyalert.data

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class ImageAnalyzer(
    private val isProcessing: () -> Boolean,
    private val detectionManager: DetectionManager,
    private val callback: (DetectionResult?) -> Unit,
) : ImageAnalysis.Analyzer {

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing()) {
            callback(null)
            imageProxy.close()
            return
        }

        // Convert ImageProxy to Bitmap
        val bitmap = imageProxyToBitmap(imageProxy)

        // Process image in a background thread
        if (bitmap != null) {
            // Save the original image dimensions
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            // Log dimensions for debugging
            Log.d(TAG, "Processing image: $originalWidth x $originalHeight")

            // Run detection
            val result = detectionManager.runDetection(bitmap)

            callback(result)

            // Update UI on main thread
//            runOnUiThread {
//                binding.overlay.setDetectionResult(
//                    result,
//                    originalWidth,
//                    originalHeight,
//                    previewWidth,
//                    previewHeight
//                )
//                isProcessing = false
//            }
        }

        imageProxy.close()

    }

    @androidx.camera.core.ExperimentalGetImage
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null

        // Get the YUV data
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U and V are swapped
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

        // Rotate the bitmap based on the image rotation
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