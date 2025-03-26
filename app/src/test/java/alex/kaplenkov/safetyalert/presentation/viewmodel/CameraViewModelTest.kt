package alex.kaplenkov.safetyalert.presentation.viewmodel

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.data.model.PersonDetection
import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.usecase.detection.SaveDetectionUseCase
import alex.kaplenkov.safetyalert.domain.usecase.report.EndReportUseCase
import alex.kaplenkov.safetyalert.domain.usecase.report.GetReportByIdUseCase
import alex.kaplenkov.safetyalert.domain.usecase.report.StartReportUseCase
import android.graphics.Bitmap
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CameraViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var startReportUseCase: StartReportUseCase
    private lateinit var endReportUseCase: EndReportUseCase
    private lateinit var getReportByIdUseCase: GetReportByIdUseCase
    private lateinit var saveDetectionUseCase: SaveDetectionUseCase
    private lateinit var viewModel: CameraViewModel

    private lateinit var mockBitmap: Bitmap
    private lateinit var mockDetectionResult: DetectionResult
    private lateinit var mockReport: DetectionReport

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        startReportUseCase = mock()
        endReportUseCase = mock()
        getReportByIdUseCase = mock()
        saveDetectionUseCase = mock()

        mockBitmap = mock()
        mockReport = mock()
        whenever(mockReport.id).thenReturn("report123")

        mockDetectionResult = DetectionResult(
            personDetections = listOf(
                PersonDetection(
                    confidence = 0.95f,
                    hasHelmet = true,
                    boundingBox = RectF(),
                    keypoints = emptyList(),
                ),
                PersonDetection(
                    confidence = 0.85f,
                    hasHelmet = false,
                    boundingBox = RectF(),
                    keypoints = emptyList(),
                )
            ),
            processingTimeMs = 100,
            helmetDetections = emptyList(),
            imageWidth = 10,
            imageHeight = 10
        )

        viewModel = CameraViewModel(
            startReportUseCase = startReportUseCase,
            endReportUseCase = endReportUseCase,
            getReportByIdUseCase = getReportByIdUseCase,
            saveDetectionUseCase = saveDetectionUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() {
        val state = viewModel.state.value

        assertFalse(state.isRecording)
        assertFalse(state.isPaused)
        assertNull(state.activeReportId)
        assertNull(state.latestDetection)
        assertFalse(state.showExitDialog)
        assertFalse(state.hasViolation)
        assertFalse(state.exitApp)
    }

    @Test
    fun `toggleRecording starts recording and updates state`() = runTest {
        whenever(startReportUseCase()).thenReturn(mockReport)

        viewModel.onEvent(CameraEvent.ToggleRecording)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isRecording)
        assertEquals("report123", state.activeReportId)
        verify(startReportUseCase).invoke()
    }

    @Test
    fun `toggleRecording stops recording and updates state`() = runTest {
        // First start recording
        whenever(startReportUseCase()).thenReturn(mockReport)
        viewModel.onEvent(CameraEvent.ToggleRecording)
        advanceUntilIdle()

        // Then stop recording
        viewModel.onEvent(CameraEvent.ToggleRecording)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isRecording)
        assertNull(state.activeReportId)
        verify(endReportUseCase).invoke("report123")
    }

    @Test
    fun `togglePause toggles pause state`() {
        viewModel.onEvent(CameraEvent.TogglePause)
        assertTrue(viewModel.state.value.isPaused)

        viewModel.onEvent(CameraEvent.TogglePause)
        assertFalse(viewModel.state.value.isPaused)
    }

    @Test
    fun `updateDetectionResult updates state with latest result and violation flag`() {
        viewModel.onEvent(CameraEvent.UpdateDetectionResult(mockDetectionResult))

        val state = viewModel.state.value
        assertEquals(mockDetectionResult, state.latestDetection)
        assertTrue(state.hasViolation) // Because one person doesn't have a helmet
    }

    @Test
    fun `captureDetection does nothing when activeReportId is null`() = runTest {
        viewModel.onEvent(CameraEvent.CaptureDetection(mockBitmap))
        advanceUntilIdle()

        verify(saveDetectionUseCase, never()).invoke(any(), any(), any())
    }

    @Test
    fun `captureDetection does nothing when currentDetection is null`() = runTest {
        // Set activeReportId but not currentDetection
        whenever(startReportUseCase()).thenReturn(mockReport)
        viewModel.onEvent(CameraEvent.ToggleRecording)
        advanceUntilIdle()

        viewModel.onEvent(CameraEvent.CaptureDetection(mockBitmap))
        advanceUntilIdle()

        verify(saveDetectionUseCase, never()).invoke(any(), any(), any())
    }

    @Test
    fun `captureDetection saves detection when there is a violation`() = runTest {
        // Set activeReportId and detection with violation
        whenever(startReportUseCase()).thenReturn(mockReport)
        viewModel.onEvent(CameraEvent.ToggleRecording)
        viewModel.onEvent(CameraEvent.UpdateDetectionResult(mockDetectionResult))
        advanceUntilIdle()

        viewModel.onEvent(CameraEvent.CaptureDetection(mockBitmap))
        advanceUntilIdle()

        verify(saveDetectionUseCase).invoke("report123", mockDetectionResult, mockBitmap)
    }

    @Test
    fun `showExitDialog updates state correctly`() {
        viewModel.onEvent(CameraEvent.ShowExitDialog)

        val state = viewModel.state.value
        assertTrue(state.isPaused)
        assertTrue(state.showExitDialog)
    }

    @Test
    fun `dismissExitDialog hides dialog`() {
        viewModel.onEvent(CameraEvent.ShowExitDialog)
        viewModel.onEvent(CameraEvent.DismissExitDialog)

        assertFalse(viewModel.state.value.showExitDialog)
    }

    @Test
    fun `exitCamera ends report and updates state to exit`() = runTest {
        // First start recording
        whenever(startReportUseCase()).thenReturn(mockReport)
        viewModel.onEvent(CameraEvent.ToggleRecording)
        advanceUntilIdle()

        // Then exit
        viewModel.onEvent(CameraEvent.ExitCamera)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isRecording)
        assertNull(state.activeReportId)
        assertFalse(state.showExitDialog)
        assertTrue(state.exitApp)
        verify(endReportUseCase).invoke("report123")
    }
}