package alex.kaplenkov.safetyalert.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Formats a Date object to a human-readable string.
 *
 * @param date The Date object to format
 * @param pattern Optional date format pattern, defaults to "yyyy-MM-dd HH:mm:ss"
 * @return Formatted date string
 */
fun formatDate(date: Date, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(date)
}

/**
 * Converts a duration in milliseconds to a human-readable string.
 *
 * @param durationMs Duration in milliseconds
 * @return Formatted duration string (e.g., "2h 30m 15s" or "45m 20s" or "10s")
 */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0s"

    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}