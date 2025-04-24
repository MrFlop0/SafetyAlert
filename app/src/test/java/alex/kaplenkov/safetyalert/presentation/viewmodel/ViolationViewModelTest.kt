package alex.kaplenkov.safetyalert.presentation.viewmodel

import alex.kaplenkov.safetyalert.data.repository.LocalViolationRepository
import alex.kaplenkov.safetyalert.domain.model.Violation
import android.graphics.Bitmap
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class ViolationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ViolationViewModel
    private lateinit var repository: LocalViolationRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    //private var mockedLog: MockedStatic<Log> = Mockito.mockStatic(Log::class.java)

    private val mockViolationsList = listOf(
        Violation(
            id = 1L,
            type = "Type1",
            confidence = 0.9f,
            imageUri = mock(Uri::class.java),
            timestamp = LocalDateTime.now().toString(),
            description = "Description 1",
            location = "Location 1",
            sessionId = "session-1"
        ),
        Violation(
            id = 2L,
            type = "Type2",
            confidence = 0.8f,
            imageUri = mock(Uri::class.java),
            timestamp = LocalDateTime.now().toString(),
            description = "Description 2",
            location = "Location 2",
            sessionId = "session-1"
        ),
        Violation(
            id = 3L,
            type = "Type3",
            confidence = 0.7f,
            imageUri = mock(Uri::class.java),
            timestamp = LocalDateTime.now().toString(),
            description = "Description 3",
            location = "Location 3",
            sessionId = "session-2"
        )
    )

    private val mockViolationsFlow = MutableStateFlow(mockViolationsList)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mock(LocalViolationRepository::class.java)

        `when`(repository.getAllViolations()).thenReturn(mockViolationsFlow)

        mockViolationsList.forEach { violation ->
            `when`(repository.getViolationById(violation.id)).thenReturn(
                flowOf(violation)
            )
        }

        viewModel = ViolationViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = testScope.runTest {
        assertFalse(viewModel.hasActiveSession.value)
        assertEquals(0, viewModel.violations.value.size)
        assertEquals(3, viewModel.totalViolationsCount.value)
        assertTrue(viewModel.currentSessionId.value.isNotEmpty())
    }

    @Test
    fun `startNewSession should generate new session id and set active session`() = testScope.runTest {
        val initialSessionId = viewModel.currentSessionId.value

        viewModel.startNewSession()
        advanceUntilIdle()

        assertTrue(viewModel.hasActiveSession.value)
        assertNotEquals(initialSessionId, viewModel.currentSessionId.value)
    }

    @Test
    fun `startNewSession should not change session id if already active`() = testScope.runTest {
        viewModel.startNewSession()
        advanceUntilIdle()

        val activeSessionId = viewModel.currentSessionId.value

        viewModel.startNewSession()
        advanceUntilIdle()

        assertEquals(activeSessionId, viewModel.currentSessionId.value)
    }

    @Test
    fun `endSession should set hasActiveSession to false`() = testScope.runTest {
        viewModel.startNewSession()
        advanceUntilIdle()

        viewModel.endSession()
        advanceUntilIdle()

        assertFalse(viewModel.hasActiveSession.value)
    }

    @Test
    fun `saveViolation should call repository`() = testScope.runTest {
        val violation = Violation(
            id = 4L,
            type = "Type4",
            confidence = 0.6f,
            sessionId = viewModel.currentSessionId.value
        )
        val bitmap = mock(Bitmap::class.java)

        viewModel.saveViolation(violation, bitmap)
        advanceUntilIdle()

        verify(repository).saveViolation(violation, bitmap)
    }

    @Test
    fun `deleteViolation should call repository`() = testScope.runTest {
        val violationId = 1L

        viewModel.deleteViolation(violationId)
        advanceUntilIdle()

        verify(repository).deleteViolation(violationId)
    }

    @Test
    fun `getViolationById should return correct violation`() = testScope.runTest {
        val violationId = 2L

        val result = viewModel.getViolationById(violationId).first()

        assertEquals(violationId, result?.id)
        assertEquals("Type2", result?.type)
    }

    @Test
    fun `violations flow should filter by current session id`() = testScope.runTest {
        val newViewModel = ViolationViewModel(repository)

        val currentSessionId = newViewModel.currentSessionId.value

        val newViolation = Violation(
            id = 4L,
            type = "Type4",
            confidence = 0.6f,
            sessionId = currentSessionId
        )

        val updatedList = mockViolationsList + newViolation
        mockViolationsFlow.value = updatedList
        advanceUntilIdle()

        assertEquals(1, newViewModel.violations.value.size)
        assertEquals(newViolation.id, newViewModel.violations.value.first().id)
    }

    @Test
    fun `getViolationsForSession should filter by session id`() = testScope.runTest {
        val sessionId = "session-2"

        val result = viewModel.getViolationsForSession(sessionId).first()

        assertEquals(1, result.size)
        assertEquals(3L, result.first().id)
    }

    @Test
    fun `getViolationsForSession with empty session id should return current session violations`() = testScope.runTest {
        val result = viewModel.getViolationsForSession("").first()

        assertEquals(viewModel.violations.value, result)
    }

    @Test
    fun `totalViolationsCount should reflect total violations`() = testScope.runTest {
        assertEquals(3, viewModel.totalViolationsCount.value)

        mockViolationsFlow.value = mockViolationsList + Violation(
            id = 4L,
            type = "Type4",
            confidence = 0.6f,
            sessionId = "session-3"
        )
        advanceUntilIdle()

        assertEquals(4, viewModel.totalViolationsCount.value)
    }

    @Test
    fun `generateSessionId should return unique values`() = testScope.runTest {
        val sessionId1 = viewModel.currentSessionId.value

        viewModel.endSession()
        viewModel.startNewSession()

        val sessionId2 = viewModel.currentSessionId.value

        assertNotEquals(sessionId1, sessionId2)
    }
}