package alex.kaplenkov.safetyalert.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReportDto(
    val id: String,
    val startTime: Long,
    val endTime: Long? = null,
    val entryIds: List<String> = emptyList()
)

@Serializable
data class DetectionEntryDto(
    val id: String,
    val reportId: String,
    val timestamp: Long,
    val imagePath: String,
    val personCount: Int,
    val peopleWithHelmets: Int,
    val peopleWithoutHelmets: Int,
    val processingTimeMs: Long
)