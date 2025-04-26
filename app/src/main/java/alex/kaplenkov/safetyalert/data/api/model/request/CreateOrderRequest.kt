package alex.kaplenkov.safetyalert.data.api.model.request

data class CreateOrderRequest(
    val name: String,
    val description: String? = null,
    val priority_id: String = "df22a3db-df94-4728-b6e7-c1c210fca945", // Высокий приоритет
    val parent_id: String? = null,
    val planed_start_date: Long? = null,
    val planed_end_date: Long? = null,
    val assigneeUsers: String? = null,
    val technicalObject: Boolean? = null,
    val technicalObjects: List<String>? = null,
    val folder_id: String? = null,
    val ordered_by: String? = null
)

