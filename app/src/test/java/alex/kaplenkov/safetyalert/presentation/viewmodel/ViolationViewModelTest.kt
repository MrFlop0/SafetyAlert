package alex.kaplenkov.safetyalert.presentation.viewmodel

import alex.kaplenkov.safetyalert.domain.model.Violation
import alex.kaplenkov.safetyalert.domain.model.ViolationStatistics
import alex.kaplenkov.safetyalert.domain.usecase.CalculateViolationStatisticsUseCase
import alex.kaplenkov.safetyalert.domain.usecase.DeleteViolationUseCase
import alex.kaplenkov.safetyalert.domain.usecase.GetAllViolationsUseCase
import alex.kaplenkov.safetyalert.domain.usecase.GetViolationByIdUseCase
import alex.kaplenkov.safetyalert.domain.usecase.GetViolationsForSessionUseCase
import alex.kaplenkov.safetyalert.domain.usecase.SaveViolationUseCase
import alex.kaplenkov.safetyalert.domain.usecase.SessionManagementUseCase
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ViolationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Use case mocks
    private val getAllViolationsUseCase: GetAllViolationsUseCase = mock()
    private val saveViolationUseCase: SaveViolationUseCase = mock()
    private val deleteViolationUseCase: DeleteViolationUseCase = mock()
    private val getViolationByIdUseCase: GetViolationByIdUseCase = mock()
    private val getViolationsForSessionUseCase: GetViolationsForSessionUseCase = mock()
    private val calculateViolationStatisticsUseCase: CalculateViolationStatisticsUseCase = mock()
    private val sessionManagementUseCase: SessionManagementUseCase = mock()

    private lateinit var viewModel: ViolationViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // StateFlows for session
        val sessionIdState = MutableStateFlow("test-session")
        whenever(sessionManagementUseCase.currentSessionId).thenReturn(sessionIdState)
        val hasActiveSessionState = MutableStateFlow(true)
        whenever(sessionManagementUseCase.hasActiveSession).thenReturn(hasActiveSessionState)

        // Violations
        val fakeViolations = listOf(
            Violation(
                id = 1, type = "Speed", confidence = 0.9f, imageUri = null,
                timestamp = "2024-06-01T12:00:00", description = "desc", location = "loc", sessionId = "test-session"
            ),
            Violation(
                id = 2, type = "Seatbelt", confidence = 1f, imageUri = null,
                timestamp = "2024-06-01T12:00:00", description = "desc", location = "loc", sessionId = "other-session"
            )
        )
        whenever(getAllViolationsUseCase()).thenReturn(flowOf(fakeViolations))

        // Statistics
        val statistics = ViolationStatistics(
            totalCount = 2,
            violationsByType = mapOf("Speed" to 1, "Seatbelt" to 1),
            violationsByWeek = mapOf("Week 22, 2024" to 2),
            mostCommonViolationType = "Speed"
        )
        whenever(calculateViolationStatisticsUseCase()).thenReturn(flowOf(statistics))

        viewModel = ViolationViewModel(
            getAllViolationsUseCase,
            saveViolationUseCase,
            deleteViolationUseCase,
            getViolationByIdUseCase,
            getViolationsForSessionUseCase,
            calculateViolationStatisticsUseCase,
            sessionManagementUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `allViolations emits correct data`() = runTest {
        val value = viewModel.allViolations.value
        assertEquals(2, value.size)
        assertEquals("Speed", value[0].type)
    }

    @Test
    fun `violations returns only current session violations`() = runTest {
        val value = viewModel.violations.value
        assertEquals(1, value.size)
        assertEquals("test-session", value[0].sessionId)
    }

    @Test
    fun `violationStatistics exposes correct statistics`() = runTest {
        val stats = viewModel.violationStatistics.value
        assertEquals(2, stats.totalCount)
        assertEquals(mapOf("Speed" to 1, "Seatbelt" to 1), stats.violationsByType)
        assertEquals("Speed", stats.mostCommonViolationType)
    }

    @Test
    fun `saveViolation delegates to use case`() = runTest {
        val violation = Violation(
            id = 3, type = "Type", confidence = 0.5f, imageUri = null,
            timestamp = "2024-06-01T12:00:00", description = "desc", location = "loc", sessionId = "test-session"
        )
        val bitmap = mock<Bitmap>()
        viewModel.saveViolation(violation, bitmap)
        advanceUntilIdle() // Wait for coroutine
        verify(saveViolationUseCase).invoke(violation, bitmap)
    }

    @Test
    fun `deleteViolation delegates to use case`() = runTest {
        viewModel.deleteViolation(1L)
        advanceUntilIdle()
        verify(deleteViolationUseCase).invoke(1L)
    }

    @Test
    fun `getViolationById delegates to use case`() = runTest {
        val violation = Violation(1, "Type", 0.9f, null, "2024-06-01T12:00:00", "desc", "loc", "test-session")
        whenever(getViolationByIdUseCase(1L)).thenReturn(flowOf(violation))
        val result = viewModel.getViolationById(1L)
        assertEquals(violation, result.toList().first())
    }

    @Test
    fun `startNewSession calls use case`() {
        viewModel.startNewSession()
        verify(sessionManagementUseCase).startNewSession()
    }

    @Test
    fun `endSession calls use case`() {
        viewModel.endSession()
        verify(sessionManagementUseCase).endSession()
    }

    @Test
    fun `getViolationsForSession with empty session id returns violations StateFlow`() = runTest {
        val result = viewModel.getViolationsForSession("")
        assertSame(viewModel.violations, result)
    }

    @Test
    fun `getViolationsForSession with non-empty session id delegates to use case`() = runTest {
        val expected = listOf(
            Violation(5, "Type", 1f, null, "2024-06-01T12:00:00", "desc", "loc", "another-session")
        )
        whenever(getViolationsForSessionUseCase("another-session")).thenReturn(flowOf(expected))
        val result = viewModel.getViolationsForSession("another-session")
        assertEquals(expected, result.toList().first())
    }
}