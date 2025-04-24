package alex.kaplenkov.safetyalert.domain.model

import android.net.Uri
import java.time.LocalDateTime

data class Violation(
    val id: Long = 0,
    val type: String,
    val confidence: Float,
    val imageUri: Uri? = null,
    val timestamp: String = LocalDateTime.now().toString(),
    val description: String? = null,
    val location: String? = null,
    val sessionId: String = ""
)