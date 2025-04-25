package alex.kaplenkov.safetyalert.presentation.viewmodel

import alex.kaplenkov.safetyalert.data.repository.LocalViolationRepository
import alex.kaplenkov.safetyalert.domain.model.Violation
import android.graphics.Bitmap
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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

    val violationsByType: StateFlow<Map<String, Int>> = allViolations
        .map { violations ->
            violations.groupingBy { it.type }.eachCount()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    val violationsByWeek: StateFlow<Map<String, Int>> = allViolations
        .map { violations ->
            violations
                .groupBy { violation ->
                    try {
                        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .parse(violation.timestamp)
                        val calendar = Calendar.getInstance()
                        calendar.time = date!!
                        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
                        val year = calendar.get(Calendar.YEAR)
                        "Week $weekOfYear, $year"
                    } catch (e: Exception) {
                        "Unknown"
                    }
                }
                .mapValues { it.value.size }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    val mostCommonViolationType: StateFlow<String?> = violationsByType
        .map { typeMap ->
            typeMap.entries.maxByOrNull { it.value }?.key
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun getViolationsForSession(sessionId: String): Flow<List<Violation>> {
        return if (sessionId.isEmpty()) {
            violations
        } else {
            allViolations.map { list -> list.filter { it.sessionId == sessionId } }
        }
    }
}