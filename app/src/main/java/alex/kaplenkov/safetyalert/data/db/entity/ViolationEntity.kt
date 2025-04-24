package alex.kaplenkov.safetyalert.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "violations")
data class ViolationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val confidence: Float,
    val imagePath: String,
    val timestamp: String = LocalDateTime.now().toString(),
    val description: String? = null,
    val location: String? = null,
    val sessionId: String = ""
)