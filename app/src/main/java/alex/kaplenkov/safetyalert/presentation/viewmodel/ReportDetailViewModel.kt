package alex.kaplenkov.safetyalert.presentation.viewmodel

import alex.kaplenkov.safetyalert.domain.model.DetectionEntry
import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.usecase.detection.GetDetectionImageUseCase
import alex.kaplenkov.safetyalert.domain.usecase.report.GetReportByIdUseCase
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val getReportByIdUseCase: GetReportByIdUseCase,
    private val getDetectionImageUseCase: GetDetectionImageUseCase,
    private val savedStateHandle: SavedStateHandle
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
            is ReportDetailEvent.ShareReport -> shareReport(event.context)
        }
    }

    private fun loadReport(reportId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val report = getReportByIdUseCase(reportId)

                if (report != null) {
                    // Load image thumbnails for each entry
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

    private fun shareReport(context: Context) {
        val report = _state.value.report ?: return

        viewModelScope.launch {
            try {
                // Create text summary
                val summaryText = buildString {
                    append("Safety Alert Detection Report\n\n")
                    append("Date: ${report.startTime}\n")
                    append("Duration: ${formatDuration(report.duration)}\n")
                    append("Total Detections: ${report.totalDetections}\n")
                    append("People with helmets: ${report.totalPeopleWithHelmets}\n")
                    append("People without helmets: ${report.totalPeopleWithoutHelmets}\n\n")

                    report.entries.forEachIndexed { index, entry ->
                        append("Detection #${index + 1}\n")
                        append("Time: ${entry.timestamp}\n")
                        append("People detected: ${entry.personCount}\n")
                        append("People without helmets: ${entry.peopleWithoutHelmets}\n\n")
                    }
                }

                // Save summary to file
                val summaryFile = File(context.cacheDir, "report_summary.txt")
                FileOutputStream(summaryFile).use { it.write(summaryText.toByteArray()) }

                // Create share intent
                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_SUBJECT, "Safety Alert Report - ${report.startTime}")
                    putExtra(Intent.EXTRA_TEXT, summaryText)

                    // Add summary and images to intent
                    val uris = ArrayList<Uri>()

                    // Add summary file
                    val summaryUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        summaryFile
                    )
                    uris.add(summaryUri)

                    // Add images
                    for (entry in report.entries) {
                        val imageFile = File(entry.imagePath)
                        if (imageFile.exists()) {
                            val imageUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                imageFile
                            )
                            uris.add(imageUri)
                        }
                    }

                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to share report: ${e.message}") }
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
    data class ShareReport(val context: Context) : ReportDetailEvent()
}