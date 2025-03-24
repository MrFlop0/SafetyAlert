package alex.kaplenkov.safetyalert.presentation.viewmodel

import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.usecase.report.DeleteReportUseCase
import alex.kaplenkov.safetyalert.domain.usecase.report.GetReportsUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportListViewModel @Inject constructor(
    private val getReportsUseCase: GetReportsUseCase,
    private val deleteReportUseCase: DeleteReportUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ReportListViewState())
    val state: StateFlow<ReportListViewState> = _state

    init {
        loadReports()
    }

    fun onEvent(event: ReportListEvent) {
        when (event) {
            is ReportListEvent.LoadReports -> loadReports()
            is ReportListEvent.SelectReport -> selectReport(event.reportId)
            is ReportListEvent.ShowDeleteDialog -> showDeleteDialog(event.report)
            is ReportListEvent.DismissDeleteDialog -> dismissDeleteDialog()
            is ReportListEvent.DeleteReport -> deleteReport()
        }
    }

    private fun loadReports() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                getReportsUseCase().collect { reports ->
                    _state.update {
                        it.copy(
                            reports = reports,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    private fun selectReport(reportId: String) {
        _state.update { it.copy(selectedReportId = reportId) }
    }

    private fun showDeleteDialog(report: DetectionReport) {
        _state.update { it.copy(reportToDelete = report) }
    }

    private fun dismissDeleteDialog() {
        _state.update { it.copy(reportToDelete = null) }
    }

    private fun deleteReport() {
        val reportToDelete = _state.value.reportToDelete ?: return

        viewModelScope.launch {
            try {
                deleteReportUseCase(reportToDelete.id)
                loadReports()
                dismissDeleteDialog()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = "Failed to delete report: ${e.message}"
                    )
                }
            }
        }
    }
}

data class ReportListViewState(
    val reports: List<DetectionReport> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedReportId: String? = null,
    val reportToDelete: DetectionReport? = null
)

sealed class ReportListEvent {
    data object LoadReports : ReportListEvent()
    data class SelectReport(val reportId: String) : ReportListEvent()
    data class ShowDeleteDialog(val report: DetectionReport) : ReportListEvent()
    data object DismissDeleteDialog : ReportListEvent()
    data object DeleteReport : ReportListEvent()
}