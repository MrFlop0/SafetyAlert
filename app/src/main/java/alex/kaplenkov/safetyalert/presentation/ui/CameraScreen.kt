package alex.kaplenkov.safetyalert.presentation.ui

import alex.kaplenkov.safetyalert.data.ImageAnalyzer
import alex.kaplenkov.safetyalert.data.detector.DetectionManagerImpl
import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.data.model.HelmetDetection
import alex.kaplenkov.safetyalert.data.model.KeypointType
import alex.kaplenkov.safetyalert.data.model.PersonDetection
import alex.kaplenkov.safetyalert.data.model.ViolationType
import alex.kaplenkov.safetyalert.domain.model.Violation
import alex.kaplenkov.safetyalert.presentation.viewmodel.ViolationViewModel
import alex.kaplenkov.safetyalert.utils.BitmapUtils
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.serialization.Serializable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "CameraScreen"
private const val TARGET_RESOLUTION_WIDTH = 640
private const val TARGET_RESOLUTION_HEIGHT = 480
private const val VIOLATION_SAVE_INTERVAL_MS = 5000

@Composable
fun CameraScreen(
    navController: NavController,
    violationViewModel: ViolationViewModel = hiltViewModel(),
    violationType: String = ViolationType.HELMET.displayName
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val detectionType = remember {
        ViolationType.fromDisplayName(violationType) ?: ViolationType.HELMET
    }

    LaunchedEffect(Unit) {
        violationViewModel.startNewSession()
        Log.d("CameraScreen", "Session ID at launch: ${violationViewModel.currentSessionId.value}")
    }

    var isPaused by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var detectionResult by remember { mutableStateOf<DetectionResult?>(null) }
    var previewWidth by remember { mutableIntStateOf(0) }
    var previewHeight by remember { mutableIntStateOf(0) }

    val isSavingViolation = remember { AtomicBoolean(false) }

    var lastSaveTimestamp by remember { mutableLongStateOf(0L) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val detectionManager = remember { DetectionManagerImpl(context) }

    LaunchedEffect(detectionType) {
        detectionManager.setDetectionType(detectionType)
    }

    LaunchedEffect(detectionResult) {
        detectionResult?.let { result ->
            val currentTime = System.currentTimeMillis()


            val hasViolation = when (detectionType) {
                ViolationType.HELMET -> result.personDetections.any { !it.hasHelmet }
                ViolationType.HANDRAIL -> result.unsafeScore > 0.9f && !result.holdsHandrail
                else -> false
            }

            if (hasViolation &&
                currentTime - lastSaveTimestamp > VIOLATION_SAVE_INTERVAL_MS &&
                !isSavingViolation.get()
            ) {
                if (isSavingViolation.compareAndSet(false, true)) {
                    try {
                        Log.d(TAG, "Starting violation save process. Time since last save: ${currentTime - lastSaveTimestamp}ms")
                        lastSaveTimestamp = currentTime

                        Log.d("SessionId", "${violationViewModel.currentSessionId.value} in CameraScreen")
                        result.bitmap?.let { originalBitmap ->
                            val violationBitmap = BitmapUtils.createViolationBitmap(originalBitmap, result)

                            when (detectionType) {
                                ViolationType.HELMET -> {

                                    result.personDetections.filter { !it.hasHelmet }.forEach { person ->
                                        val violation = Violation(
                                            type = "Safety Helmet Violation",
                                            confidence = person.confidence,
                                            description = "Человек без защитного шлема",
                                            location = "Worksite area",
                                            sessionId = violationViewModel.currentSessionId.value
                                        )
                                        violationViewModel.saveViolation(violation, violationBitmap)
                                    }
                                }
                                ViolationType.HANDRAIL -> {

                                    val violation = Violation(
                                        type = "Stair Safety Violation",
                                        confidence = result.unsafeScore,
                                        description = "Человек не держится за перила",
                                        location = "Staircase area",
                                        sessionId = violationViewModel.currentSessionId.value
                                    )
                                    violationViewModel.saveViolation(violation, violationBitmap)
                                }
                                else -> {}
                            }
                        }
                    } finally {
                        isSavingViolation.set(false)
                    }
                }
            }
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            detectionManager.close()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }

                previewView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    previewWidth = previewView.width
                    previewHeight = previewView.height
                    Log.d(TAG, "Preview size updated: $previewWidth x $previewHeight")
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()


                    val targetResolution =
                        android.util.Size(TARGET_RESOLUTION_WIDTH, TARGET_RESOLUTION_HEIGHT)


                    val preview = Preview.Builder()
                        .setTargetResolution(targetResolution)
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }


                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(targetResolution)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                cameraExecutor,
                                ImageAnalyzer(
                                    isProcessing = { isPaused },
                                    detectionManager = detectionManager
                                ) { result, bitmap ->

                                    result?.let {
                                        it.bitmap = bitmap
                                        detectionResult = it
                                    }
                                }
                            )
                        }


                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {

                        cameraProvider.unbindAll()


                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )


                        camera.cameraControl.setZoomRatio(1.0f)

                    } catch (exc: Exception) {
                        Log.e(TAG, "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        detectionResult?.let { detection ->
            if (previewWidth > 0 && previewHeight > 0) {
                DetectionResultOverlay(
                    detectionResult = detection,
                    imageWidth = detection.imageWidth,
                    imageHeight = detection.imageHeight,
                    previewWidth = previewWidth,
                    previewHeight = previewHeight,
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                )
            }

        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    isPaused = !isPaused

                },

                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) Color.Red else Color.Green
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(if (isPaused) "Продолжить" else "Пауза")
            }

            Button(
                onClick = {
                    showExitDialog = true
                    isPaused = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Назад")
            }
        }
    }


    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Выйти?") },
            text = { Text("Вы уверены что хотите закончить сьемку?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        val currentSessionId = violationViewModel.currentSessionId.value
                        Log.d("CameraScreen", "Navigating to report with session ID: $currentSessionId")
                        navController.navigate(ReportScreen(sessionId = violationViewModel.currentSessionId.value))
                    }
                ) {
                    Text("Да")
                }
            },
            dismissButton = {
                Button(onClick = { showExitDialog = false }) {
                    Text("Нет")
                }
            }
        )
    }
}

@Composable
fun DetectionResultOverlay(
    detectionResult: DetectionResult,
    imageWidth: Int,
    imageHeight: Int,
    previewWidth: Int,
    previewHeight: Int,
    modifier: Modifier = Modifier
) {
    val textStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    val textMeasurer = rememberTextMeasurer()

     
    val scaleFactors = remember(imageWidth, imageHeight, previewWidth, previewHeight) {
        calculateScaleFactors(
            imageWidth.toFloat(),
            imageHeight.toFloat(),
            previewWidth.toFloat(),
            previewHeight.toFloat()
        )
    }

    Canvas(modifier = modifier) {
         
        drawDetectionResults(
            detectionResult,
            scaleFactors.first,   
            scaleFactors.second,  
            scaleFactors.third,   
            scaleFactors.fourth,  
            textMeasurer,
            textStyle
        )
    }
}

private fun calculateScaleFactors(
    imageWidth: Float,
    imageHeight: Float,
    previewWidth: Float,
    previewHeight: Float
): Quadruple<Float, Float, Float, Float> {
     
    val imageAspect = imageWidth / imageHeight
    val previewAspect = previewWidth / previewHeight

     
    val scaleFactor: Float
    val displayWidth: Float
    val displayHeight: Float

    if (imageAspect > previewAspect) {
         
        scaleFactor = previewWidth / imageWidth
        displayWidth = previewWidth
        displayHeight = imageHeight * scaleFactor
    } else {
         
        scaleFactor = previewHeight / imageHeight
        displayHeight = previewHeight
        displayWidth = imageWidth * scaleFactor
    }


    val scaleX = scaleFactor
    val scaleY = scaleFactor


    val offsetX = (previewWidth - displayWidth) / 2
    val offsetY = (previewHeight - displayHeight) / 2

    Log.d(
        TAG,
        "Image size: $imageWidth x $imageHeight, Preview size: $previewWidth x $previewHeight"
    )
    Log.d(TAG, "Display size: $displayWidth x $displayHeight")
    Log.d(TAG, "Scale: $scaleX, $scaleY, Offset: $offsetX, $offsetY")

    return Quadruple(scaleX, scaleY, offsetX, offsetY)
}

private fun DrawScope.drawDetectionResults(
    result: DetectionResult,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle
) {

    result.personDetections.forEach { person ->
        drawPerson(person, scaleX, scaleY, offsetX, offsetY, textMeasurer, textStyle, result.detectionType)
    }


    if (result.detectionType == ViolationType.HELMET) {
        result.helmetDetections.forEach { helmet ->
            if (!helmet.isAssociated) {
                drawHelmet(helmet, scaleX, scaleY, offsetX, offsetY, textMeasurer, textStyle)
            }
        }
    }


    drawStatistics(result, textMeasurer, textStyle)
}

private fun DrawScope.drawPerson(
    person: PersonDetection,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
    detectionType: ViolationType?
) {

    val color = when (detectionType) {
        ViolationType.HELMET -> if (person.hasHelmet) Color.Green else Color.Red
        ViolationType.HANDRAIL -> if (person.holdsHandrail) Color.Green else Color.Red
        else -> Color.Yellow
    }


    val bbox = person.boundingBox
    val scaledRect = Rect(
        left = bbox.left * scaleX + offsetX,
        top = bbox.top * scaleY + offsetY,
        right = bbox.right * scaleX + offsetX,
        bottom = bbox.bottom * scaleY + offsetY
    )

    drawRect(
        color = color,
        topLeft = Offset(scaledRect.left, scaledRect.top),
        size = Size(scaledRect.width, scaledRect.height),
        style = Stroke(width = 3f)
    )


    val labelText = "Person ${String.format("%.2f", person.confidence)}"
    val textLayoutResult = textMeasurer.measure(
        text = labelText,
        style = textStyle
    )

    val textX = scaledRect.left.coerceAtLeast(5f)
    val textY = (scaledRect.top - textLayoutResult.size.height).coerceAtLeast(5f)

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(textX, textY)
    )


    when (detectionType) {
        ViolationType.HELMET -> {
            if (!person.hasHelmet) {
                val warningText = "NO HELMET!"
                val warningLayoutResult = textMeasurer.measure(
                    text = warningText,
                    style = textStyle.copy(
                        color = Color.Red,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                drawText(
                    textLayoutResult = warningLayoutResult,
                    topLeft = Offset(
                        scaledRect.left + (scaledRect.width - warningLayoutResult.size.width) / 2,
                        scaledRect.top - warningLayoutResult.size.height - 5
                    ).coerceIn(
                        Offset(5f, 5f),
                        Offset(size.width - warningLayoutResult.size.width - 5, size.height - 5)
                    )
                )
            }
        }
        ViolationType.HANDRAIL -> {
            if (!person.holdsHandrail) {
                val warningText = "NOT HOLDING HANDRAIL!"
                val warningLayoutResult = textMeasurer.measure(
                    text = warningText,
                    style = textStyle.copy(
                        color = Color.Red,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                drawText(
                    textLayoutResult = warningLayoutResult,
                    topLeft = Offset(
                        scaledRect.left + (scaledRect.width - warningLayoutResult.size.width) / 2,
                        scaledRect.top - warningLayoutResult.size.height - 5
                    ).coerceIn(
                        Offset(5f, 5f),
                        Offset(size.width - warningLayoutResult.size.width - 5, size.height - 5)
                    )
                )
            }
        }
        else -> {}
    }


    drawSkeleton(person, scaleX, scaleY, offsetX, offsetY, detectionType)


    if (detectionType == ViolationType.HELMET) {
        person.headBoundingBox?.let { headBox ->
            val scaledHeadRect = Rect(
                left = headBox.left * scaleX + offsetX,
                top = headBox.top * scaleY + offsetY,
                right = headBox.right * scaleX + offsetX,
                bottom = headBox.bottom * scaleY + offsetY
            )

            drawRect(
                color = Color.Magenta,
                topLeft = Offset(scaledHeadRect.left, scaledHeadRect.top),
                size = Size(scaledHeadRect.width, scaledHeadRect.height),
                style = Stroke(width = 2f)
            )
        }
    }
}



private fun DrawScope.drawSkeleton(
    person: PersonDetection,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
    detectionType: ViolationType?
) {

    val lineColor = when (detectionType) {
        ViolationType.HELMET -> if (person.hasHelmet) Color.Green else Color(0f, 150f / 255f, 1f)
        ViolationType.HANDRAIL -> if (person.holdsHandrail) Color.Green else Color(0f, 150f / 255f, 1f)
        else -> Color(0f, 150f / 255f, 1f)
    }


    KeypointType.POSE_PAIRS.forEach { (first, second) ->
        val firstPoint = person.keypoints.find { it.type == first }
        val secondPoint = person.keypoints.find { it.type == second }

        if (firstPoint != null && secondPoint != null &&
            firstPoint.confidence > 0.2f && secondPoint.confidence > 0.2f
        ) {
            val startX = firstPoint.position.x * scaleX + offsetX
            val startY = firstPoint.position.y * scaleY + offsetY
            val endX = secondPoint.position.x * scaleX + offsetX
            val endY = secondPoint.position.y * scaleY + offsetY

            drawLine(
                color = lineColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2f
            )
        }
    }


    person.keypoints.forEach { keypoint ->
        if (keypoint.confidence > 0.2f) {

            val pointColor = if (detectionType == ViolationType.HELMET && keypoint.type in KeypointType.HEAD_KEYPOINTS) {
                if (person.hasHelmet) Color.Yellow else Color.Red
            } else {
                lineColor
            }

            val pointX = keypoint.position.x * scaleX + offsetX
            val pointY = keypoint.position.y * scaleY + offsetY

            drawCircle(
                color = pointColor,
                center = Offset(pointX, pointY),
                radius = 5f
            )
        }
    }
}

private fun DrawScope.drawHelmet(
    helmet: HelmetDetection,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle
) {
    val bbox = helmet.boundingBox
    val scaledRect = Rect(
        left = bbox.left * scaleX + offsetX,
        top = bbox.top * scaleY + offsetY,
        right = bbox.right * scaleX + offsetX,
        bottom = bbox.bottom * scaleY + offsetY
    )

    drawRect(
        color = Color.Yellow,
        topLeft = Offset(scaledRect.left, scaledRect.top),
        size = Size(scaledRect.width, scaledRect.height),
        style = Stroke(width = 2f)
    )

     
    val labelText = "Helmet ${String.format("%.2f", helmet.confidence)}"
    val textLayoutResult = textMeasurer.measure(
        text = labelText,
        style = textStyle
    )

     
    val textX = scaledRect.left.coerceAtLeast(5f)
    val textY = (scaledRect.top - textLayoutResult.size.height).coerceAtLeast(5f)

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(textX, textY)
    )
}

private fun DrawScope.drawStatistics(
    result: DetectionResult,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle
) {
    when (result.detectionType) {
        ViolationType.HELMET -> {

            val totalPeople = result.personDetections.size
            val peopleWithHelmets = result.personDetections.count { it.hasHelmet }
            val peopleWithoutHelmets = totalPeople - peopleWithHelmets

            val statsText = "Total: $totalPeople | With Helmets: $peopleWithHelmets | Without Helmets: $peopleWithoutHelmets"
            val statsLayout = textMeasurer.measure(
                text = statsText,
                style = textStyle.copy(fontSize = 16.sp)
            )

            drawText(
                textLayoutResult = statsLayout,
                topLeft = Offset(20f, 50f)
            )

            if (peopleWithoutHelmets > 0) {
                val warningText = "WARNING! $peopleWithoutHelmets ${if (peopleWithoutHelmets == 1) "person" else "people"} without helmet!"
                val warningLayout = textMeasurer.measure(
                    text = warningText,
                    style = textStyle.copy(
                        color = Color.Red,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                drawText(
                    textLayoutResult = warningLayout,
                    topLeft = Offset(20f, 100f)
                )
            }
        }
        ViolationType.HANDRAIL -> {

            val handrailColor = when {
                result.holdsHandrail -> Color.Green
                result.unsafeScore > 0.9f -> Color.Red
                else -> Color.Gray
            }

            val handrailText = when {
                result.holdsHandrail -> "SAFE: Person holds the handrail"
                result.unsafeScore > 0.9f -> "UNSAFE: Person does not hold the handrail!"
                else -> "Monitoring stair safety..."
            }

            val handrailLayout = textMeasurer.measure(
                text = handrailText,
                style = textStyle.copy(
                    color = handrailColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            drawText(
                textLayoutResult = handrailLayout,
                topLeft = Offset(20f, 50f)
            )


            val scoresText = "Safe: ${String.format("%.3f", result.handrailScore)}, Unsafe: ${String.format("%.3f", result.unsafeScore)}"
            val scoresLayout = textMeasurer.measure(
                text = scoresText,
                style = textStyle.copy(fontSize = 16.sp)
            )

            drawText(
                textLayoutResult = scoresLayout,
                topLeft = Offset(20f, 100f)
            )


            val sequenceText = if (result.handrailScore == 0f && result.unsafeScore == 0f)
                "Collecting pose data..." else "Model predictions active"
            val sequenceLayout = textMeasurer.measure(
                text = sequenceText,
                style = textStyle.copy(fontSize = 14.sp, color = Color.Yellow)
            )

            drawText(
                textLayoutResult = sequenceLayout,
                topLeft = Offset(20f, 130f)
            )
        }
        else -> {

        }
    }


    val timeText = "Processing: ${result.processingTimeMs}ms"
    val timeLayout = textMeasurer.measure(
        text = timeText,
        style = textStyle
    )

    drawText(
        textLayoutResult = timeLayout,
        topLeft = Offset(20f, size.height - 20f)
    )
}


private fun Offset.coerceIn(min: Offset, max: Offset): Offset {
    return Offset(
        x.coerceIn(min.x, max.x),
        y.coerceIn(min.y, max.y)
    )
}

 
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Serializable
data class CameraScreen(val violation: String)
