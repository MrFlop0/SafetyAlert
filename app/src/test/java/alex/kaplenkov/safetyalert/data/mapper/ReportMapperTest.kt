package alex.kaplenkov.safetyalert.data.mapper

import alex.kaplenkov.safetyalert.data.model.DetectionEntryDto
import alex.kaplenkov.safetyalert.data.model.ReportDto
import alex.kaplenkov.safetyalert.domain.model.DetectionEntry
import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class ReportMapperTest {

    @Test
    fun `ReportDto toDomain maps all fields correctly`() {
        // Arrange
        val reportId = "report123"
        val startTime = 1672531200000L // 2023-01-01 00:00:00
        val endTime = 1672531260000L   // 2023-01-01 00:01:00

        val reportDto = ReportDto(
            id = reportId,
            startTime = startTime,
            endTime = endTime,
            entryIds = listOf("entry1", "entry2")
        )

        val entries = listOf(
            DetectionEntry(
                id = "entry1",
                timestamp = Date(1672531220000L),
                imagePath = "path/to/image1.jpg",
                personCount = 3,
                peopleWithHelmets = 2,
                peopleWithoutHelmets = 1,
                processingTimeMs = 150
            ),
            DetectionEntry(
                id = "entry2",
                timestamp = Date(1672531240000L),
                imagePath = "path/to/image2.jpg",
                personCount = 2,
                peopleWithHelmets = 1,
                peopleWithoutHelmets = 1,
                processingTimeMs = 160
            )
        )

        // Act
        val result = reportDto.toDomain(entries)

        // Assert
        assertEquals(reportId, result.id)
        assertEquals(Date(startTime), result.startTime)
        assertEquals(Date(endTime), result.endTime)
        assertEquals(entries, result.entries)
    }

    @Test
    fun `ReportDto toDomain handles null endTime correctly`() {
        // Arrange
        val reportDto = ReportDto(
            id = "report123",
            startTime = 1672531200000L,
            endTime = null,
            entryIds = emptyList()
        )

        // Act
        val result = reportDto.toDomain(emptyList())

        // Assert
        assertEquals(null, result.endTime)
    }

    @Test
    fun `DetectionReport toDto maps all fields correctly`() {
        // Arrange
        val reportId = "report123"
        val startTime = Date(1672531200000L)
        val endTime = Date(1672531260000L)

        val entries = listOf(
            DetectionEntry(
                id = "entry1",
                timestamp = Date(1672531220000L),
                imagePath = "path/to/image1.jpg",
                personCount = 3,
                peopleWithHelmets = 2,
                peopleWithoutHelmets = 1,
                processingTimeMs = 150
            ),
            DetectionEntry(
                id = "entry2",
                timestamp = Date(1672531240000L),
                imagePath = "path/to/image2.jpg",
                personCount = 2,
                peopleWithHelmets = 1,
                peopleWithoutHelmets = 1,
                processingTimeMs = 160
            )
        )

        val report = DetectionReport(
            id = reportId,
            startTime = startTime,
            endTime = endTime,
            entries = entries
        )

        // Act
        val result = report.toDto()

        // Assert
        assertEquals(reportId, result.id)
        assertEquals(startTime.time, result.startTime)
        assertEquals(endTime.time, result.endTime)
        assertEquals(listOf("entry1", "entry2"), result.entryIds)
    }

    @Test
    fun `DetectionReport toDto handles null endTime correctly`() {
        // Arrange
        val report = DetectionReport(
            id = "report123",
            startTime = Date(1672531200000L),
            endTime = null,
            entries = emptyList()
        )

        // Act
        val result = report.toDto()

        // Assert
        assertEquals(null, result.endTime)
    }

    @Test
    fun `DetectionEntryDto toDomain maps all fields correctly`() {
        // Arrange
        val entryDto = DetectionEntryDto(
            id = "entry1",
            reportId = "report123",
            timestamp = 1672531220000L,
            imagePath = "path/to/image.jpg",
            personCount = 5,
            peopleWithHelmets = 3,
            peopleWithoutHelmets = 2,
            processingTimeMs = 200
        )

        // Act
        val result = entryDto.toDomain()

        // Assert
        assertEquals("entry1", result.id)
        assertEquals(Date(1672531220000L), result.timestamp)
        assertEquals("path/to/image.jpg", result.imagePath)
        assertEquals(5, result.personCount)
        assertEquals(3, result.peopleWithHelmets)
        assertEquals(2, result.peopleWithoutHelmets)
        assertEquals(200, result.processingTimeMs)
    }

    @Test
    fun `DetectionEntry toDto maps all fields correctly`() {
        // Arrange
        val entry = DetectionEntry(
            id = "entry1",
            timestamp = Date(1672531220000L),
            imagePath = "path/to/image.jpg",
            personCount = 5,
            peopleWithHelmets = 3,
            peopleWithoutHelmets = 2,
            processingTimeMs = 200
        )

        val reportId = "report123"

        // Act
        val result = entry.toDto(reportId)

        // Assert
        assertEquals("entry1", result.id)
        assertEquals("report123", result.reportId)
        assertEquals(1672531220000L, result.timestamp)
        assertEquals("path/to/image.jpg", result.imagePath)
        assertEquals(5, result.personCount)
        assertEquals(3, result.peopleWithHelmets)
        assertEquals(2, result.peopleWithoutHelmets)
        assertEquals(200, result.processingTimeMs)
    }
}