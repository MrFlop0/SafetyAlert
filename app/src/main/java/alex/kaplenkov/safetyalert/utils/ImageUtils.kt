package alex.kaplenkov.safetyalert.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import kotlin.math.min

data class PreprocessedImage(
    val bitmap: Bitmap,
    val ratio: Float,
    val xOffset: Float,
    val yOffset: Float
)

object ImageUtils {
    fun preprocessImage(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): PreprocessedImage {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // Calculate scaling ratio
        val ratio = min(
            targetWidth.toFloat() / originalWidth,
            targetHeight.toFloat() / originalHeight
        )

        // Calculate new dimensions
        val newWidth = (originalWidth * ratio).toInt()
        val newHeight = (originalHeight * ratio).toInt()

        // Calculate padding
        val xOffset = (targetWidth - newWidth) / 2f
        val yOffset = (targetHeight - newHeight) / 2f

        // Create target bitmap with padding
        val scaledBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)

        // Fill with black
        canvas.drawColor(Color.BLACK)

        // Resize original image
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        // Draw resized image with padding
        canvas.drawBitmap(resizedBitmap, xOffset, yOffset, null)

        // Clean up
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        return PreprocessedImage(scaledBitmap, ratio, xOffset, yOffset)
    }

    fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}