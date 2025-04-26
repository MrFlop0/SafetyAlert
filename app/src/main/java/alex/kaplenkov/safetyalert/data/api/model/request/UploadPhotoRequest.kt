package alex.kaplenkov.safetyalert.data.api.model.request

data class UploadPhotoRequest(
    val name: String,
    val orderId: String,
    val file: String // base64-encoded image
)