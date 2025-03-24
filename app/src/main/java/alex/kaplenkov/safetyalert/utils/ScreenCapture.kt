package alex.kaplenkov.safetyalert.utils

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.camera.view.PreviewView
import android.view.View

object ScreenCapture {
    private val TAG = "ScreenCapture"

    fun captureDetectionWithOverlay(
        cameraPreview: PreviewView,
        overlayView: View,
        detectionResult: DetectionResult
    ): Bitmap {
        // Get the preview bitmap
        val previewBitmap = cameraPreview.bitmap ?:
        Bitmap.createBitmap(cameraPreview.width, cameraPreview.height, Bitmap.Config.ARGB_8888)

        // Create a combined bitmap
        val resultBitmap = Bitmap.createBitmap(
            cameraPreview.width, cameraPreview.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Draw the preview first
        canvas.drawBitmap(previewBitmap, 0f, 0f, null)

        // Draw the overlay view (detection visualization)
        overlayView.draw(canvas)

        // Add timestamp and detection info at the bottom
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            style = Paint.Style.FILL
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }

        val timestampFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val timestamp = timestampFormat.format(java.util.Date())

        // Count statistics
        val totalPeople = detectionResult.personDetections.size
        val peopleWithHelmets = detectionResult.personDetections.count { it.hasHelmet }
        val peopleWithoutHelmets = totalPeople - peopleWithHelmets

        // Draw timestamp at top
        canvas.drawText(timestamp, 20f, 40f, paint)

        // Draw detection info at bottom
        val infoText = "People: $totalPeople | With helmets: $peopleWithHelmets | Without helmets: $peopleWithoutHelmets"
        val bounds = Rect()
        paint.getTextBounds(infoText, 0, infoText.length, bounds)
        canvas.drawText(infoText, 20f, resultBitmap.height - bounds.height() - 20f, paint)

        return resultBitmap
    }
}