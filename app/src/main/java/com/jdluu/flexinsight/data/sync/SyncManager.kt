package com.jdluu.flexinsight.data.sync

import android.content.Context
import androidx.work.*
import com.jdluu.flexinsight.core.logger.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Trigger for user-initiated immediate syncs (e.g. pull to refresh, AI Trainer pre-chat). */
interface ManualSyncScheduler {
    fun syncNow()
}

/**
 * Manages background synchronization tasks using WorkManager.
 * Centralizes sync scheduling for the entire application.
 */
@Singleton
class SyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ManualSyncScheduler {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val TAG = "SyncManager"
        const val MANUAL_SYNC_WORK_NAME = "manual_sync_work"
        const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"
    }

    /**
     * Enqueues an immediate one-time sync task.
     * This is useful when the user completes a workout or manually pulls to refresh.
     */
    override fun syncNow() {
        AppLogger.d("Requesting immediate background sync", tag = TAG)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()

        workManager.enqueueUniqueWork(
            MANUAL_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    /**
     * Schedules a periodic background sync to keep the local database fresh.
     */
    fun schedulePeriodicSync(intervalHours: Long = 6) {
        AppLogger.d("Scheduling periodic sync every $intervalHours hours", tag = TAG)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .addTag(TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }

    /**
     * Stop all background sync tasks.
     */
    fun cancelAllSync() {
        AppLogger.d("Cancelling all sync tasks", tag = TAG)
        workManager.cancelUniqueWork(MANUAL_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
    }
}
