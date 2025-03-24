package alex.kaplenkov.safetyalert.presentation.viewmodel

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.domain.usecase.detection.SaveDetectionUseCase
import alex.kaplenkov.safetyalert.domain.usecase.report.EndReportUseCase
import alex.kaplenkov.safetyalert.domain.usecase.report.GetReportByIdUseCase
import alex.kaplenkov.safetyalert.domain.usecase.report.StartReportUseCase
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val startReportUseCase: StartReportUseCase,
    private val endReportUseCase: EndReportUseCase,
    private val getReportByIdUseCase: GetReportByIdUseCase,
    private val saveDetectionUseCase: SaveDetectionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CameraViewState())
    val state: StateFlow<CameraViewState> = _state

    private var currentDetection: DetectionResult? = null

    fun onEvent(event: CameraEvent) {
        when (event) {
            is CameraEvent.ToggleRecording -> toggleRecording()
            is CameraEvent.TogglePause -> togglePause()
            is CameraEvent.UpdateDetectionResult -> updateDetectionResult(event.result)
            is CameraEvent.CaptureDetection -> captureDetection(event.bitmap)
            is CameraEvent.ShowExitDialog -> showExitDialog()
            is CameraEvent.DismissExitDialog -> dismissExitDialog()
            is CameraEvent.ExitCamera -> exitCamera()
        }
    }

    private fun toggleRecording() {
        viewModelScope.launch {
            if (_state.value.isRecording) {
                _state.value.activeReportId?.let { reportId ->
                    endReportUseCase(reportId)
                }
                _state.update { it.copy(
                    isRecording = false,
                    activeReportId = null
                )}
            } else {
                val report = startReportUseCase()
                _state.update { it.copy(
                    isRecording = true,
                    activeReportId = report.id
                )}
            }
        }
    }

    private fun togglePause() {
        _state.update { it.copy(isPaused = !it.isPaused) }
    }

    private fun updateDetectionResult(result: DetectionResult) {
        currentDetection = result
        _state.update { it.copy(
            latestDetection = result,
            hasViolation = result.personDetections.any { person -> !person.hasHelmet }
        )}
    }

    private fun captureDetection(bitmap: Bitmap) {
        val activeReportId = _state.value.activeReportId ?: return
        val detection = currentDetection ?: return

        // Only capture frames with violations (people without helmets)
        if (!_state.value.hasViolation) return

        viewModelScope.launch {
            saveDetectionUseCase(activeReportId, detection, bitmap)
        }
    }

    private fun showExitDialog() {
        _state.update {
            it.copy(
                isPaused = true,
                showExitDialog = true
            )
        }
    }

    private fun dismissExitDialog() {
        _state.update { it.copy(showExitDialog = false) }
    }

    private fun exitCamera() {
        viewModelScope.launch {
            _state.value.activeReportId?.let { reportId ->
                endReportUseCase(reportId)
            }
            _state.update { it.copy(
                isRecording = false,
                activeReportId = null,
                showExitDialog = false,
                exitApp = true
            )}
        }
    }
}

data class CameraViewState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val activeReportId: String? = null,
    val latestDetection: DetectionResult? = null,
    val showExitDialog: Boolean = false,
    val hasViolation: Boolean = false,
    val exitApp: Boolean = false
)

sealed class CameraEvent {
    data object ToggleRecording : CameraEvent()
    data object TogglePause : CameraEvent()
    data class UpdateDetectionResult(val result: DetectionResult) : CameraEvent()
    data class CaptureDetection(val bitmap: Bitmap) : CameraEvent()
    data object ShowExitDialog : CameraEvent()
    data object DismissExitDialog : CameraEvent()
    data object ExitCamera : CameraEvent()
}