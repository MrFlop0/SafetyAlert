package alex.kaplenkov.safetyalert.utils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DateTimeUtilsTest {

    @Test
    fun `formatDate with default pattern works correctly`() {
        // Create a fixed date for testing
        val calendar = Calendar.getInstance()
        calendar.set(2023, Calendar.JANUARY, 15, 10, 30, 45)
        val date = calendar.time

        val result = formatDate(date)

        // Expected format: yyyy-MM-dd HH:mm:ss
        assertEquals("2023-01-15 10:30:45", result)
    }

    @Test
    fun `formatDate with custom pattern works correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2023, Calendar.JANUARY, 15, 10, 30, 45)
        val date = calendar.time

        val result = formatDate(date, "dd/MM/yyyy HH:mm")

        assertEquals("15/01/2023 10:30", result)
    }

    @Test
    fun `formatDuration with hours minutes seconds works correctly`() {
        // 2 hours, 30 minutes, 15 seconds
        val durationMs = (2 * 60 * 60 + 30 * 60 + 15) * 1000L

        val result = formatDuration(durationMs)

        assertEquals("2h 30m 15s", result)
    }

    @Test
    fun `formatDuration with minutes seconds works correctly`() {
        // 45 minutes, 20 seconds
        val durationMs = (45 * 60 + 20) * 1000L

        val result = formatDuration(durationMs)

        assertEquals("45m 20s", result)
    }

    @Test
    fun `formatDuration with only seconds works correctly`() {
        // 10 seconds
        val durationMs = 10 * 1000L

        val result = formatDuration(durationMs)

        assertEquals("10s", result)
    }

    @Test
    fun `formatDuration with zero returns zero seconds`() {
        val result = formatDuration(0)

        assertEquals("0s", result)
    }

    @Test
    fun `formatDuration with negative value returns zero seconds`() {
        val result = formatDuration(-1000)

        assertEquals("0s", result)
    }
}