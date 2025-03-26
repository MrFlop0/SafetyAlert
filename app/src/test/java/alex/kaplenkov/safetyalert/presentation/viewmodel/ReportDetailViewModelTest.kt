package alex.kaplenkov.safetyalert.presentation.viewmodel

import alex.kaplenkov.safetyalert.domain.model.DetectionEntry
import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.usecase.detection.GetDetectionImageUseCase
import alex.kaplenkov.safetyalert.domain.usecase.report.GetReportByIdUseCase
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
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
import org.mockito.kotlin.whenever
import java.util.Date

@ExperimentalCoroutinesApi
class ReportDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getReportByIdUseCase: GetReportByIdUseCase
    private lateinit var getDetectionImageUseCase: GetDetectionImageUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ReportDetailViewModel

    private lateinit var mockReport: DetectionReport
    private lateinit var mockEntry1: DetectionEntry
    private lateinit var mockEntry2: DetectionEntry
    private lateinit var mockBitmap: Bitmap
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getReportByIdUseCase = mock()
        getDetectionImageUseCase = mock()
        savedStateHandle = SavedStateHandle()
        mockContext = mock()
        mockBitmap = mock()

        // Create mock entries
        mockEntry1 = DetectionEntry(
            id = "entry1",
            timestamp = Date(1672531220000L),
            imagePath = "path/to/image1.jpg",
            personCount = 3,
            peopleWithHelmets = 2,
            peopleWithoutHelmets = 1,
            processingTimeMs = 150
        )

        mockEntry2 = DetectionEntry(
            id = "entry2",
            timestamp = Date(1672531240000L),
            imagePath = "path/to/image2.jpg",
            personCount = 2,
            peopleWithHelmets = 1,
            peopleWithoutHelmets = 1,
            processingTimeMs = 160
        )

        // Create mock report
        mockReport = DetectionReport(
            id = "report123",
            startTime = Date(1672531200000L),
            endTime = Date(1672531260000L),
            entries = listOf(mockEntry1, mockEntry2)
        )

        whenever(mockContext.cacheDir).thenReturn(mock())

        viewModel = ReportDetailViewModel(
            getReportByIdUseCase = getReportByIdUseCase,
            getDetectionImageUseCase = getDetectionImageUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() {
        val state = viewModel.state.value

        assertNull(state.report)
        assertTrue(state.entryImages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `constructor loads report when reportId is in savedStateHandle`() = runTest {
        whenever(getReportByIdUseCase("report123")).thenReturn(mockReport)
        whenever(getDetectionImageUseCase(any())).thenReturn(mockBitmap)

        savedStateHandle["reportId"] = "report123"
        viewModel = ReportDetailViewModel(getReportByIdUseCase, getDetectionImageUseCase, savedStateHandle)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(mockReport, state.report)
        assertEquals(2, state.entryImages.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadReport updates state with report and images`() = runTest {
        whenever(getReportByIdUseCase("report123")).thenReturn(mockReport)
        whenever(getDetectionImageUseCase(any())).thenReturn(mockBitmap)

        viewModel.onEvent(ReportDetailEvent.LoadReport("report123"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(mockReport, state.report)
        assertEquals(2, state.entryImages.size)
        assertTrue(state.entryImages.containsKey(mockEntry1))
        assertTrue(state.entryImages.containsKey(mockEntry2))
        assertEquals(mockBitmap, state.entryImages[mockEntry1])
        assertEquals(mockBitmap, state.entryImages[mockEntry2])
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadReport updates state with error when report not found`() = runTest {
        whenever(getReportByIdUseCase("nonexistent")).thenReturn(null)

        viewModel.onEvent(ReportDetailEvent.LoadReport("nonexistent"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.report)
        assertTrue(state.entryImages.isEmpty())
        assertFalse(state.isLoading)
        assertEquals("Report not found", state.error)
    }

    @Test
    fun `loadReport updates state with error when exception occurs`() = runTest {
        whenever(getReportByIdUseCase("report123")).thenThrow(RuntimeException("Network error"))

        viewModel.onEvent(ReportDetailEvent.LoadReport("report123"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.report)
        assertTrue(state.entryImages.isEmpty())
        assertFalse(state.isLoading)
        assertEquals("Failed to load report: Network error", state.error)
    }

    @Test
    fun `formatDuration formats correctly for hours minutes seconds`() {
        // Access private method using reflection
        val method = ReportDetailViewModel::class.java.getDeclaredMethod(
            "formatDuration",
            Long::class.java
        )
        method.isAccessible = true

        val result = method.invoke(viewModel, 3661000L) as String // 1h 1m 1s
        assertEquals("1 h 1 m 1 s", result)
    }

    @Test
    fun `formatDuration formats correctly for minutes seconds`() {
        val method = ReportDetailViewModel::class.java.getDeclaredMethod(
            "formatDuration",
            Long::class.java
        )
        method.isAccessible = true

        val result = method.invoke(viewModel, 61000L) as String // 1m 1s
        assertEquals("1 m 1 s", result)
    }

    @Test
    fun `formatDuration formats correctly for seconds only`() {
        val method = ReportDetailViewModel::class.java.getDeclaredMethod(
            "formatDuration",
            Long::class.java
        )
        method.isAccessible = true

        val result = method.invoke(viewModel, 5000L) as String // 5s
        assertEquals("5 s", result)
    }
}