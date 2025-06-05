package alex.kaplenkov.safetyalert.domain.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManagementUseCase @Inject constructor() {

    private val _currentSessionId = MutableStateFlow(generateSessionId())
    val currentSessionId: StateFlow<String> = _currentSessionId

    private var _hasActiveSession = MutableStateFlow(false)
    val hasActiveSession: StateFlow<Boolean> = _hasActiveSession

    fun startNewSession(): String {
        if (!_hasActiveSession.value) {
            _currentSessionId.value = generateSessionId()
            _hasActiveSession.value = true
        }
        return _currentSessionId.value
    }

    fun endSession() {
        _hasActiveSession.value = false
    }

    private fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }
}