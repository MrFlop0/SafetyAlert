package alex.kaplenkov.safetyalert.presentation.ui

import alex.kaplenkov.safetyalert.data.DetectionManager
import alex.kaplenkov.safetyalert.data.ImageAnalyzer
import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.data.model.HelmetDetection
import alex.kaplenkov.safetyalert.data.model.KeypointType
import alex.kaplenkov.safetyalert.data.model.PersonDetection
import alex.kaplenkov.safetyalert.presentation.viewmodel.CameraViewModel
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

private const val TAG = "CameraScreen"
private const val TARGET_RESOLUTION_WIDTH = 640
private const val TARGET_RESOLUTION_HEIGHT = 480

@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State
    var isPaused by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var detectionResult by remember { mutableStateOf<DetectionResult?>(null) }
    var previewWidth by remember { mutableIntStateOf(0) }
    var previewHeight by remember { mutableIntStateOf(0) }

    // Resources
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val detectionManager = remember { DetectionManager(context) }

    // Clean up resources when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            detectionManager.close()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
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

                    // Define target resolution
                    val targetResolution =
                        android.util.Size(TARGET_RESOLUTION_WIDTH, TARGET_RESOLUTION_HEIGHT)

                    // Set up preview
                    val preview = Preview.Builder()
                        .setTargetResolution(targetResolution)
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    // Set up image analyzer
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(targetResolution)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                cameraExecutor,
                                ImageAnalyzer({ isPaused }, detectionManager) { result ->
                                    detectionResult = result
                                }
                            )
                        }

                    // Select back camera
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        // Unbind all use cases before rebinding
                        cameraProvider.unbindAll()

                        // Bind use cases to camera
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )

                        // Reset zoom to ensure no zoom is applied
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

        // UI Controls
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
                    //detectionResult = null
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

    // Exit Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Выйти?") },
            text = { Text("Вы уверены что хотите закончить сьемку?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        navController.navigate(SummaryScreen)
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

    // Calculate scaling factors
    val scaleFactors = remember(imageWidth, imageHeight, previewWidth, previewHeight) {
        calculateScaleFactors(
            imageWidth.toFloat(),
            imageHeight.toFloat(),
            previewWidth.toFloat(),
            previewHeight.toFloat()
        )
    }

    Canvas(modifier = modifier) {
        // Draw detection results using the calculated scale factors
        drawDetectionResults(
            detectionResult,
            scaleFactors.first,  // scaleX
            scaleFactors.second, // scaleY
            scaleFactors.third,  // offsetX
            scaleFactors.fourth, // offsetY
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
    // Calculate aspect ratios
    val imageAspect = imageWidth / imageHeight
    val previewAspect = previewWidth / previewHeight

    // Calculate the actual display dimensions of the preview accounting for FIT_CENTER
    val scaleFactor: Float
    val displayWidth: Float
    val displayHeight: Float

    if (imageAspect > previewAspect) {
        // Image is wider than preview - letterboxing on top/bottom
        scaleFactor = previewWidth / imageWidth
        displayWidth = previewWidth
        displayHeight = imageHeight * scaleFactor
    } else {
        // Image is taller than preview - letterboxing on sides
        scaleFactor = previewHeight / imageHeight
        displayHeight = previewHeight
        displayWidth = imageWidth * scaleFactor
    }

    // Calculate scale factors that maintain aspect ratio
    val scaleX = scaleFactor
    val scaleY = scaleFactor

    // Calculate offsets to center the image
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
    // Draw people
    result.personDetections.forEach { person ->
        drawPerson(person, scaleX, scaleY, offsetX, offsetY, textMeasurer, textStyle)
    }

    // Draw unassociated helmets
    result.helmetDetections.forEach { helmet ->
        if (!helmet.isAssociated) {
            drawHelmet(helmet, scaleX, scaleY, offsetX, offsetY, textMeasurer, textStyle)
        }
    }

    // Draw statistics
    drawStatistics(result, textMeasurer, textStyle)
}

private fun DrawScope.drawPerson(
    person: PersonDetection,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle
) {
    val color = if (person.hasHelmet) Color.Green else Color.Red

    // Draw bounding box
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

    // Draw label
    val labelText = "Person ${String.format("%.2f", person.confidence)}"
    val textLayoutResult = textMeasurer.measure(
        text = labelText,
        style = textStyle
    )

    // Ensure text is visible
    val textX = scaledRect.left.coerceAtLeast(5f)
    val textY = (scaledRect.top - textLayoutResult.size.height).coerceAtLeast(5f)

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(textX, textY)
    )

    // Draw warning for people without helmets
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

    // Draw skeleton
    drawSkeleton(person, scaleX, scaleY, offsetX, offsetY)

    // Draw head bounding box (for debugging)
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

private fun DrawScope.drawSkeleton(
    person: PersonDetection,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float
) {
    val lineColor = if (person.hasHelmet) Color.Green else Color(0f, 150f / 255f, 1f)

    // Draw connections first (lines)
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

    // Draw keypoints
    person.keypoints.forEach { keypoint ->
        if (keypoint.confidence > 0.2f) {
            val pointColor = if (keypoint.type in KeypointType.HEAD_KEYPOINTS) {
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

    // Draw label
    val labelText = "Helmet ${String.format("%.2f", helmet.confidence)}"
    val textLayoutResult = textMeasurer.measure(
        text = labelText,
        style = textStyle
    )

    // Ensure text is visible
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
    val totalPeople = result.personDetections.size
    val peopleWithHelmets = result.personDetections.count { it.hasHelmet }
    val peopleWithoutHelmets = totalPeople - peopleWithHelmets

    // Draw statistics
    val statsText =
        "Total: $totalPeople | With Helmets: $peopleWithHelmets | Without Helmets: $peopleWithoutHelmets"
    val statsLayout = textMeasurer.measure(
        text = statsText,
        style = textStyle.copy(fontSize = 16.sp)
    )

    drawText(
        textLayoutResult = statsLayout,
        topLeft = Offset(20f, 50f)
    )

    // Draw warning if there are people without helmets
    if (peopleWithoutHelmets > 0) {
        val warningText =
            "WARNING! $peopleWithoutHelmets ${if (peopleWithoutHelmets == 1) "person" else "people"} without helmet!"
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

    // Draw processing time
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

// Helper class for returning multiple values
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Serializable
object CameraScreen
