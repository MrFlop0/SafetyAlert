package alex.kaplenkov.safetyalert.domain.model

data class ViolationStatistics(
    val totalCount: Int,
    val violationsByType: Map<String, Int>,
    val violationsByWeek: Map<String, Int>,
    val mostCommonViolationType: String?
)