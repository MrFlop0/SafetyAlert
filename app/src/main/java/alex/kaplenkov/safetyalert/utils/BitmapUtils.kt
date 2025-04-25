package alex.kaplenkov.safetyalert.utils

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.data.model.ViolationType
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object BitmapUtils {

    fun createViolationBitmap(original: Bitmap, result: DetectionResult): Bitmap {

        val outputBitmap = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)


        canvas.drawBitmap(original, 0f, 0f, null)


        val boundingBoxPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }


        when (result.detectionType) {
            ViolationType.HELMET -> {

                val peopleWithoutHelmets = result.personDetections.filter { !it.hasHelmet }
                for (person in peopleWithoutHelmets) {

                    canvas.drawRect(person.boundingBox, boundingBoxPaint)


                    person.headBoundingBox?.let { headBox ->
                        val headPaint = Paint(boundingBoxPaint)
                        headPaint.color = Color.MAGENTA
                        canvas.drawRect(headBox, headPaint)
                    }
                }
            }

            ViolationType.HANDRAIL -> {

                val unsafePeople = result.personDetections.filter { !it.holdsHandrail }
                for (person in unsafePeople) {

                    val handrailBoundingBoxPaint = Paint(boundingBoxPaint).apply {
                        color = Color.rgb(255, 165, 0)
                    }


                    canvas.drawRect(person.boundingBox, handrailBoundingBoxPaint)
                }
            }

            else -> {

                for (person in result.personDetections) {
                    canvas.drawRect(person.boundingBox, boundingBoxPaint)
                }
            }
        }

        return outputBitmap
    }
}