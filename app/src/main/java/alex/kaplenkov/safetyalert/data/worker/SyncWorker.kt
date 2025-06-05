package alex.kaplenkov.safetyalert.data.worker

import alex.kaplenkov.safetyalert.data.network.NetworkMonitor
import alex.kaplenkov.safetyalert.data.repository.SyncRepository
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val networkMonitor: NetworkMonitor
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncAAA", "SyncWorker started")

            var isConnected = false
            networkMonitor.networkStatus.collect {
                isConnected = it
                if (isConnected) {
                    syncRepository.syncPendingViolations()
                    return@collect
                }
            }

            return@withContext if (isConnected) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}