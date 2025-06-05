package alex.kaplenkov.safetyalert.domain.usecase

import alex.kaplenkov.safetyalert.domain.model.Violation
import alex.kaplenkov.safetyalert.domain.model.ViolationStatistics
import alex.kaplenkov.safetyalert.domain.repository.ViolationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class CalculateViolationStatisticsUseCase @Inject constructor(
    private val violationRepository: ViolationRepository
) {
    operator fun invoke(): Flow<ViolationStatistics> {
        return violationRepository.getAllViolations()
            .map { violations ->
                val violationsByType = violations.groupingBy { it.type }.eachCount()
                val violationsByWeek = calculateViolationsByWeek(violations)
                val mostCommonType = violationsByType.entries.maxByOrNull { it.value }?.key

                ViolationStatistics(
                    totalCount = violations.size,
                    violationsByType = violationsByType,
                    violationsByWeek = violationsByWeek,
                    mostCommonViolationType = mostCommonType
                )
            }
    }

    private fun calculateViolationsByWeek(violations: List<Violation>): Map<String, Int> {
        return violations
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
}