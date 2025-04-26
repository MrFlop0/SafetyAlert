package alex.kaplenkov.safetyalert.data.api.model.response

data class OrderResponse(
    val id: String,
    val name: String,
    val status_id: String,
    // другие поля из ответа
)