package alex.kaplenkov.safetyalert.presentation.viewmodel

import alex.kaplenkov.safetyalert.data.repository.LocalViolationRepository
import alex.kaplenkov.safetyalert.domain.model.Violation
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ViolationViewModel @Inject constructor(
    private val localViolationRepository: LocalViolationRepository
) : ViewModel() {

    private val _currentSessionId = MutableStateFlow(generateSessionId())
    val currentSessionId: StateFlow<String> = _currentSessionId

    private var _hasActiveSession = MutableStateFlow(false)
    val hasActiveSession: StateFlow<Boolean> = _hasActiveSession

    val allViolations = localViolationRepository
        .getAllViolations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val violations: StateFlow<List<Violation>> = allViolations
        .map { list -> list.filter { it.sessionId == currentSessionId.value } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val totalViolationsCount: StateFlow<Int> = allViolations
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    fun saveViolation(violation: Violation, bitmap: Bitmap) {
        viewModelScope.launch {
            localViolationRepository.saveViolation(violation, bitmap)
        }
    }

    fun startNewSession() {
        if (!_hasActiveSession.value) {
            _currentSessionId.value = generateSessionId()
            _hasActiveSession.value = true
        }
    }

    fun endSession() {
        _hasActiveSession.value = false
    }

    fun getViolationById(id: Long): Flow<Violation?> {
        return localViolationRepository.getViolationById(id)
    }

    private fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }

    fun deleteViolation(violationId: Long) {
        viewModelScope.launch {
            localViolationRepository.deleteViolation(violationId)
        }
    }

    fun getViolationsForSession(sessionId: String): Flow<List<Violation>> {
        return if (sessionId.isEmpty()) {
            violations
        } else {
            allViolations.map { list -> list.filter { it.sessionId == sessionId } }
        }
    }
}