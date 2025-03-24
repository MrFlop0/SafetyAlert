package alex.kaplenkov.safetyalert.data.mapper

import alex.kaplenkov.safetyalert.data.model.ReportDto
import alex.kaplenkov.safetyalert.data.model.DetectionEntryDto
import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.model.DetectionEntry
import java.util.Date

fun ReportDto.toDomain(entries: List<DetectionEntry>): DetectionReport {
    return DetectionReport(
        id = id,
        startTime = Date(startTime),
        endTime = endTime?.let { Date(it) },
        entries = entries
    )
}

fun DetectionReport.toDto(): ReportDto {
    return ReportDto(
        id = id,
        startTime = startTime.time,
        endTime = endTime?.time,
        entryIds = entries.map { it.id }
    )
}

fun DetectionEntryDto.toDomain(): DetectionEntry {
    return DetectionEntry(
        id = id,
        timestamp = Date(timestamp),
        imagePath = imagePath,
        personCount = personCount,
        peopleWithHelmets = peopleWithHelmets,
        peopleWithoutHelmets = peopleWithoutHelmets,
        processingTimeMs = processingTimeMs
    )
}

fun DetectionEntry.toDto(reportId: String): DetectionEntryDto {
    return DetectionEntryDto(
        id = id,
        reportId = reportId,
        timestamp = timestamp.time,
        imagePath = imagePath,
        personCount = personCount,
        peopleWithHelmets = peopleWithHelmets,
        peopleWithoutHelmets = peopleWithoutHelmets,
        processingTimeMs = processingTimeMs
    )
}