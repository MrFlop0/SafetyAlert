package alex.kaplenkov.safetyalert.data.datasource.local

import alex.kaplenkov.safetyalert.data.model.DetectionEntryDto
import alex.kaplenkov.safetyalert.data.model.ReportDto
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportLocalDataSource @Inject constructor(
    private val context: Context
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val baseDirectory: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "detection_reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val reportsDirectory: File by lazy {
        val dir = File(baseDirectory, "reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val entriesDirectory: File by lazy {
        val dir = File(baseDirectory, "entries")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val imagesDirectory: File by lazy {
        val dir = File(baseDirectory, "images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    suspend fun saveReport(report: ReportDto): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(reportsDirectory, "${report.id}.json")
            file.writeText(json.encodeToString(report))
            Log.d(TAG, "Saved report to: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save report", e)
            false
        }
    }

    suspend fun saveDetectionEntry(entry: DetectionEntryDto): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(entriesDirectory, "${entry.id}.json")
            file.writeText(json.encodeToString(entry))
            Log.d(TAG, "Saved entry to: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save entry", e)
            false
        }
    }

    suspend fun saveImage(bitmap: Bitmap, filename: String): String = withContext(Dispatchers.IO) {
        val imageFile = File(imagesDirectory, filename)
        try {
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Log.d(TAG, "Saved image to: ${imageFile.absolutePath}")
            imageFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image", e)
            ""
        }
    }

    suspend fun getReport(reportId: String): ReportDto? = withContext(Dispatchers.IO) {
        try {
            val file = File(reportsDirectory, "$reportId.json")
            if (!file.exists()) return@withContext null

            json.decodeFromString<ReportDto>(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get report: $reportId", e)
            null
        }
    }

    suspend fun getAllReports(): Flow<List<ReportDto>> = flow {
        try {
            val reportFiles = reportsDirectory.listFiles { file ->
                file.isFile && file.name.endsWith(".json")
            } ?: emptyArray()

            val reports = reportFiles.mapNotNull { file ->
                try {
                    json.decodeFromString<ReportDto>(file.readText())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse report: ${file.name}", e)
                    null
                }
            }.sortedByDescending { it.startTime }

            emit(reports)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all reports", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getDetectionEntry(entryId: String): DetectionEntryDto? = withContext(Dispatchers.IO) {
        try {
            val file = File(entriesDirectory, "$entryId.json")
            if (!file.exists()) return@withContext null

            json.decodeFromString<DetectionEntryDto>(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get entry: $entryId", e)
            null
        }
    }

    suspend fun getDetectionEntries(reportId: String): List<DetectionEntryDto> = withContext(Dispatchers.IO) {
        try {
            val report = getReport(reportId) ?: return@withContext emptyList()

            report.entryIds.mapNotNull { entryId ->
                getDetectionEntry(entryId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get entries for report: $reportId", e)
            emptyList()
        }
    }

    suspend fun getImage(path: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext null

            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get image: $path", e)
            null
        }
    }

    suspend fun deleteReport(reportId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val report = getReport(reportId) ?: return@withContext false

            // Delete entries
            report.entryIds.forEach { entryId ->
                val entry = getDetectionEntry(entryId)
                entry?.let {
                    // Delete entry file
                    val entryFile = File(entriesDirectory, "$entryId.json")
                    entryFile.delete()

                    // Delete image file
                    val imageFile = File(entry.imagePath)
                    if (imageFile.exists()) {
                        imageFile.delete()
                    }
                }
            }

            // Delete report file
            val reportFile = File(reportsDirectory, "$reportId.json")
            reportFile.delete()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete report: $reportId", e)
            false
        }
    }

    companion object {
        private const val TAG = "ReportLocalDataSource"
    }
}