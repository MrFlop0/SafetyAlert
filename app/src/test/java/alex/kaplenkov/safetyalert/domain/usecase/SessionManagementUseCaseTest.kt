package alex.kaplenkov.safetyalert.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionManagementUseCaseTest {

    private lateinit var useCase: SessionManagementUseCase

    @Before
    fun setup() {
        useCase = SessionManagementUseCase()
    }

    @Test
    fun `startNewSession activates session and generates id`() = runTest {
        val oldId = useCase.currentSessionId.value
        useCase.startNewSession()
        val newId = useCase.currentSessionId.value

        assertTrue(useCase.hasActiveSession.value)
        assertNotEquals(oldId, newId)
        assertTrue(newId.isNotBlank())
    }

    @Test
    fun `endSession deactivates session`() = runTest {
        useCase.startNewSession()
        useCase.endSession()
        assertFalse(useCase.hasActiveSession.value)
    }
}