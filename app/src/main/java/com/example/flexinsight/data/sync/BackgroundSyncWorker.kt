package com.example.flexinsight.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/**
 * WorkManager worker for periodic background sync.
 * Syncs data from Hevy API when network is available.
 *
 * Note: Repository injection will be handled by WorkManagerFactory
 */
class BackgroundSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Get repository from application
            val app = applicationContext as? com.example.flexinsight.FlexInsightApplication
            if (app == null) {
                return Result.failure()
            }

            // Perform sync
            app.repository.syncAllData()
            Result.success()
        } catch (e: Exception) {
            // Retry on failure (WorkManager will handle retry logic)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "background_sync_work"
    }
}
