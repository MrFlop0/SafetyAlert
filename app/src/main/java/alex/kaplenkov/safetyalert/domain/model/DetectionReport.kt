package alex.kaplenkov.safetyalert.domain.model

import java.util.Date
import java.util.UUID

data class DetectionReport(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Date = Date(),
    val endTime: Date? = null,
    val entries: List<DetectionEntry> = emptyList()
) {
    val duration: Long get() =
        endTime?.let { it.time - startTime.time } ?: (Date().time - startTime.time)

    val totalDetections: Int get() = entries.sumOf { it.personCount }

    val totalPeopleWithHelmets: Int get() = entries.sumOf { it.peopleWithHelmets }

    val totalPeopleWithoutHelmets: Int get() = entries.sumOf { it.peopleWithoutHelmets }

    val isActive: Boolean get() = endTime == null
}

data class DetectionEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Date = Date(),
    val imagePath: String,
    val personCount: Int,
    val peopleWithHelmets: Int,
    val peopleWithoutHelmets: Int,
    val processingTimeMs: Long
)