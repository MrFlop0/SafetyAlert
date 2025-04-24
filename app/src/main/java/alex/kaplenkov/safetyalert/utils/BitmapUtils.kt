package alex.kaplenkov.safetyalert.utils

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.data.model.KeypointType
import alex.kaplenkov.safetyalert.data.model.PersonDetection
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

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            isFakeBoldText = true
            setShadowLayer(8f, 2f, 2f, Color.BLACK)
        }

        val keyPointPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        val warningPaint = Paint().apply {
            color = Color.RED
            textSize = 60f
            isFakeBoldText = true
            setShadowLayer(10f, 3f, 3f, Color.BLACK)
        }


        when (result.detectionType) {
            ViolationType.HELMET -> {

                val peopleWithoutHelmets = result.personDetections.filter { !it.hasHelmet }

                for (person in peopleWithoutHelmets) {
                    drawHelmetViolation(canvas, person, boundingBoxPaint, textPaint, keyPointPaint, linePaint)
                }

                if (peopleWithoutHelmets.isNotEmpty()) {
                    canvas.drawText(
                        "Обнаружено нарушение! Отсутствие каски",
                        50f,
                        100f,
                        warningPaint
                    )
                }
            }

            ViolationType.HANDRAIL -> {

                val unsafePeople = result.personDetections.filter { !it.holdsHandrail }

                for (person in unsafePeople) {
                    drawHandrailViolation(canvas, person, boundingBoxPaint, textPaint, keyPointPaint, linePaint)
                }

                if (result.unsafeScore > 0.9f && !result.holdsHandrail) {
                    canvas.drawText(
                        "Обнаружено нарушение! Не держится за перила",
                        50f,
                        100f,
                        warningPaint
                    )


                    val scorePaint = Paint(textPaint).apply {
                        textSize = 40f
                    }
                    canvas.drawText(
                        "Unsafe score: ${String.format("%.2f", result.unsafeScore)}",
                        50f,
                        160f,
                        scorePaint
                    )
                }
            }

            else -> {

                for (person in result.personDetections) {
                    drawGenericViolation(canvas, person, boundingBoxPaint, textPaint, keyPointPaint, linePaint)
                }

                canvas.drawText(
                    "Обнаружено нарушение!",
                    50f,
                    100f,
                    warningPaint
                )
            }
        }

        return outputBitmap
    }

    private fun drawHelmetViolation(
        canvas: Canvas,
        person: PersonDetection,
        boundingBoxPaint: Paint,
        textPaint: Paint,
        keyPointPaint: Paint,
        linePaint: Paint
    ) {

        val bbox = person.boundingBox
        canvas.drawRect(bbox, boundingBoxPaint)


        person.headBoundingBox?.let { headBox ->
            val headPaint = Paint(boundingBoxPaint)
            headPaint.color = Color.MAGENTA
            canvas.drawRect(headBox, headPaint)


            canvas.drawText(
                "ОТСУТСТВИЕ КАСКИ!",
                headBox.left,
                headBox.top - 20,
                textPaint
            )
        }


        drawSkeleton(canvas, person, keyPointPaint, linePaint)


        val confidenceText = "Уверенность: ${(person.confidence * 100).toInt()}%"
        canvas.drawText(
            confidenceText,
            bbox.left + 10,
            bbox.bottom - 20,
            textPaint
        )
    }

    private fun drawHandrailViolation(
        canvas: Canvas,
        person: PersonDetection,
        boundingBoxPaint: Paint,
        textPaint: Paint,
        keyPointPaint: Paint,
        linePaint: Paint
    ) {

        val handrailBoundingBoxPaint = Paint(boundingBoxPaint).apply {
            color = Color.rgb(255, 165, 0)
        }

        val handrailLinePaint = Paint(linePaint).apply {
            color = Color.rgb(255, 165, 0)
            strokeWidth = 5f
        }


        val bbox = person.boundingBox
        canvas.drawRect(bbox, handrailBoundingBoxPaint)


        val armKeypoints = listOf(
            KeypointType.LEFT_SHOULDER, KeypointType.LEFT_ELBOW, KeypointType.LEFT_WRIST,
            KeypointType.RIGHT_SHOULDER, KeypointType.RIGHT_ELBOW, KeypointType.RIGHT_WRIST
        )


        person.keypoints.forEach { keypoint ->
            if (keypoint.confidence > 0.2f) {
                val pointColor = if (keypoint.type in armKeypoints) {
                    Color.RED
                } else {
                    Color.YELLOW
                }

                val pointPaint = Paint(keyPointPaint).apply { color = pointColor }
                canvas.drawCircle(
                    keypoint.position.x,
                    keypoint.position.y,
                    if (keypoint.type in armKeypoints) 15f else 10f,
                    pointPaint
                )
            }
        }


        KeypointType.POSE_PAIRS.forEach { (first, second) ->
            val firstPoint = person.keypoints.find { it.type == first }
            val secondPoint = person.keypoints.find { it.type == second }

            if (firstPoint != null && secondPoint != null &&
                firstPoint.confidence > 0.2f && secondPoint.confidence > 0.2f
            ) {
                val isArmConnection = (first in armKeypoints) && (second in armKeypoints)
                val connectionPaint = if (isArmConnection) {
                    Paint(handrailLinePaint).apply { strokeWidth = 8f }
                } else {
                    handrailLinePaint
                }

                canvas.drawLine(
                    firstPoint.position.x,
                    firstPoint.position.y,
                    secondPoint.position.x,
                    secondPoint.position.y,
                    connectionPaint
                )
            }
        }


        canvas.drawText(
            "НЕ ДЕРЖИТСЯ ЗА ПЕРИЛА!",
            bbox.left,
            bbox.top - 20,
            textPaint
        )


        val confidenceText = "Уверенность: ${(person.confidence * 100).toInt()}%"
        canvas.drawText(
            confidenceText,
            bbox.left + 10,
            bbox.bottom - 20,
            textPaint
        )
    }

    private fun drawGenericViolation(
        canvas: Canvas,
        person: PersonDetection,
        boundingBoxPaint: Paint,
        textPaint: Paint,
        keyPointPaint: Paint,
        linePaint: Paint
    ) {

        val bbox = person.boundingBox
        canvas.drawRect(bbox, boundingBoxPaint)


        drawSkeleton(canvas, person, keyPointPaint, linePaint)


        val confidenceText = "Уверенность: ${(person.confidence * 100).toInt()}%"
        canvas.drawText(
            confidenceText,
            bbox.left + 10,
            bbox.bottom - 20,
            textPaint
        )
    }

    private fun drawSkeleton(
        canvas: Canvas,
        person: PersonDetection,
        keyPointPaint: Paint,
        linePaint: Paint
    ) {

        person.keypoints.forEach { keypoint ->
            if (keypoint.confidence > 0.2f) {
                val pointColor = if (keypoint.type in KeypointType.HEAD_KEYPOINTS) {
                    Color.RED
                } else {
                    Color.YELLOW
                }

                val pointPaint = Paint(keyPointPaint).apply { color = pointColor }
                canvas.drawCircle(
                    keypoint.position.x,
                    keypoint.position.y,
                    10f,
                    pointPaint
                )
            }
        }


        KeypointType.POSE_PAIRS.forEach { (first, second) ->
            val firstPoint = person.keypoints.find { it.type == first }
            val secondPoint = person.keypoints.find { it.type == second }

            if (firstPoint != null && secondPoint != null &&
                firstPoint.confidence > 0.2f && secondPoint.confidence > 0.2f
            ) {
                canvas.drawLine(
                    firstPoint.position.x,
                    firstPoint.position.y,
                    secondPoint.position.x,
                    secondPoint.position.y,
                    linePaint
                )
            }
        }
    }
}