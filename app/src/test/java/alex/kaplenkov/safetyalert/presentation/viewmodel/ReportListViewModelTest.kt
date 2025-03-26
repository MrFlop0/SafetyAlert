package alex.kaplenkov.safetyalert.presentation.viewmodel

import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.usecase.report.DeleteReportUseCase
import alex.kaplenkov.safetyalert.domain.usecase.report.GetReportsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ReportListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getReportsUseCase: GetReportsUseCase
    private lateinit var deleteReportUseCase: DeleteReportUseCase
    private lateinit var viewModel: ReportListViewModel

    private lateinit var mockReports: List<DetectionReport>
    private lateinit var mockReport1: DetectionReport
    private lateinit var mockReport2: DetectionReport

    @Before
    fun setup() = runTest {
        Dispatchers.setMain(testDispatcher)

        getReportsUseCase = mock()
        deleteReportUseCase = mock()

        mockReport1 = mock<DetectionReport>().apply {
            whenever(id).thenReturn("report1")
        }
        mockReport2 = mock<DetectionReport>().apply {
            whenever(id).thenReturn("report2")
        }
        mockReports = listOf(mockReport1, mockReport2)

        whenever(getReportsUseCase()).thenReturn(flowOf(mockReports))

        viewModel = ReportListViewModel(
            getReportsUseCase = getReportsUseCase,
            deleteReportUseCase = deleteReportUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads reports`() = runTest {
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(mockReports, state.reports)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.selectedReportId)
        assertNull(state.reportToDelete)
    }

    @Test
    fun `loadReports updates state with reports`() = runTest {
        // Reset the viewModel to clear the initial load
        viewModel = ReportListViewModel(getReportsUseCase, deleteReportUseCase)

        // Set a new list of reports
        val newReports = listOf(mockReport1)
        whenever(getReportsUseCase()).thenReturn(flowOf(newReports))

        viewModel.onEvent(ReportListEvent.LoadReports)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(newReports, state.reports)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadReports updates state with error when exception occurs`() = runTest {
        // Reset the viewModel
        viewModel = ReportListViewModel(getReportsUseCase, deleteReportUseCase)

        // Mock exception
        whenever(getReportsUseCase()).thenThrow(RuntimeException("Database error"))

        viewModel.onEvent(ReportListEvent.LoadReports)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.reports.isEmpty())
        assertFalse(state.isLoading)
        assertEquals("Database error", state.error)
    }

    @Test
    fun `selectReport updates selectedReportId`() {
        viewModel.onEvent(ReportListEvent.SelectReport("report1"))

        assertEquals("report1", viewModel.state.value.selectedReportId)
    }

    @Test
    fun `showDeleteDialog sets reportToDelete`() = runTest {
        advanceUntilIdle() // Wait for initial load

        viewModel.onEvent(ReportListEvent.ShowDeleteDialog(mockReport1))

        assertEquals(mockReport1, viewModel.state.value.reportToDelete)
    }

    @Test
    fun `dismissDeleteDialog clears reportToDelete`() {
        viewModel.onEvent(ReportListEvent.ShowDeleteDialog(mockReport1))
        viewModel.onEvent(ReportListEvent.DismissDeleteDialog)

        assertNull(viewModel.state.value.reportToDelete)
    }

    @Test
    fun `deleteReport deletes report and reloads reports`() = runTest {
        // First set report to delete
        advanceUntilIdle() // Wait for initial load
        viewModel.onEvent(ReportListEvent.ShowDeleteDialog(mockReport1))

        // Mock successful deletion
        whenever(deleteReportUseCase("report1")).thenReturn(true)

        viewModel.onEvent(ReportListEvent.DeleteReport)
        advanceUntilIdle()

        // Verify report was deleted and dialog was dismissed
        verify(deleteReportUseCase).invoke("report1")
        assertNull(viewModel.state.value.reportToDelete)
    }

    @Test
    fun `deleteReport does nothing when reportToDelete is null`() = runTest {
        viewModel.onEvent(ReportListEvent.DeleteReport)
        advanceUntilIdle()

        // No deletion should occur
        verify(deleteReportUseCase, times(0)).invoke(any())
    }

    @Test
    fun `deleteReport updates error state when exception occurs`() = runTest {
        // First set report to delete
        advanceUntilIdle() // Wait for initial load
        viewModel.onEvent(ReportListEvent.ShowDeleteDialog(mockReport1))

        // Mock exception
        whenever(deleteReportUseCase("report1")).thenThrow(RuntimeException("Permission denied"))

        viewModel.onEvent(ReportListEvent.DeleteReport)
        advanceUntilIdle()

        assertEquals("Failed to delete report: Permission denied", viewModel.state.value.error)
    }
}