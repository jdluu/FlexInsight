package com.example.flexinsight.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.flexinsight.data.repository.FlexRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for periodic background sync.
 * Syncs data from Hevy API when network is available.
 */
@HiltWorker
class BackgroundSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: FlexRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("BackgroundSyncWorker", "Running periodic sync worker")
            // Perform sync
            repository.syncAllData()
            Log.d("BackgroundSyncWorker", "Periodic sync worker success")
            Result.success()
        } catch (e: Exception) {
            Log.e("BackgroundSyncWorker", "Periodic sync worker failed", e)
            // Retry on failure (WorkManager will handle retry logic)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "background_sync_work"
    }
}
