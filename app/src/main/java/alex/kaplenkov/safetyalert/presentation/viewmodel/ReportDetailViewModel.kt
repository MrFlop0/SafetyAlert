package alex.kaplenkov.safetyalert.presentation.viewmodel

import alex.kaplenkov.safetyalert.domain.model.DetectionEntry
import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.usecase.detection.GetDetectionImageUseCase
import alex.kaplenkov.safetyalert.domain.usecase.report.GetReportByIdUseCase
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val getReportByIdUseCase: GetReportByIdUseCase,
    private val getDetectionImageUseCase: GetDetectionImageUseCase,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ReportDetailViewState())
    val state: StateFlow<ReportDetailViewState> = _state

    init {
        savedStateHandle.get<String>("reportId")?.let { reportId ->
            loadReport(reportId)
        }
    }

    fun onEvent(event: ReportDetailEvent) {
        when (event) {
            is ReportDetailEvent.LoadReport -> loadReport(event.reportId)
        }
    }

    private fun loadReport(reportId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val report = getReportByIdUseCase(reportId)

                if (report != null) {
                     
                    val entryImages = report.entries.associateWith { entry ->
                        getDetectionImageUseCase(entry.imagePath)
                    }

                    _state.update {
                        it.copy(
                            report = report,
                            entryImages = entryImages,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Report not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load report: ${e.message}"
                    )
                }
            }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "$hours h ${minutes % 60} m ${seconds % 60} s"
            minutes > 0 -> "$minutes m ${seconds % 60} s"
            else -> "$seconds s"
        }
    }
}

data class ReportDetailViewState(
    val report: DetectionReport? = null,
    val entryImages: Map<DetectionEntry, Bitmap?> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class ReportDetailEvent {
    data class LoadReport(val reportId: String) : ReportDetailEvent()
}