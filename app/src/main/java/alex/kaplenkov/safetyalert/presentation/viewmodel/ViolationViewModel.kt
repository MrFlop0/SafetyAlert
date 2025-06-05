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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViolationViewModel @Inject constructor(
    private val getAllViolationsUseCase: GetAllViolationsUseCase,
    private val saveViolationUseCase: SaveViolationUseCase,
    private val deleteViolationUseCase: DeleteViolationUseCase,
    private val getViolationByIdUseCase: GetViolationByIdUseCase,
    private val getViolationsForSessionUseCase: GetViolationsForSessionUseCase,
    private val calculateViolationStatisticsUseCase: CalculateViolationStatisticsUseCase,
    private val sessionManagementUseCase: SessionManagementUseCase
) : ViewModel() {

    val currentSessionId: StateFlow<String> = sessionManagementUseCase.currentSessionId
    val hasActiveSession: StateFlow<Boolean> = sessionManagementUseCase.hasActiveSession

    val allViolations = getAllViolationsUseCase()
        .stateIn(
            scope =  viewModelScope,
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

    val violationStatistics = calculateViolationStatisticsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ViolationStatistics(
                totalCount = 0,
                violationsByType = emptyMap(),
                violationsByWeek = emptyMap(),
                mostCommonViolationType = null
            )
        )

    val totalViolationsCount: StateFlow<Int> = violationStatistics
        .map { it.totalCount }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    fun saveViolation(violation: Violation, bitmap: Bitmap) {
        viewModelScope.launch {
            saveViolationUseCase(violation, bitmap)
        }
    }

    fun startNewSession() {
        sessionManagementUseCase.startNewSession()
    }

    fun endSession() {
        sessionManagementUseCase.endSession()
    }

    fun getViolationById(id: Long): Flow<Violation?> {
        return getViolationByIdUseCase(id)
    }

    fun deleteViolation(violationId: Long) {
        viewModelScope.launch {
            deleteViolationUseCase(violationId)
        }
    }

    fun getViolationsForSession(sessionId: String): Flow<List<Violation>> {
        return if (sessionId.isEmpty()) {
            violations
        } else {
            getViolationsForSessionUseCase(sessionId)
        }
    }
}