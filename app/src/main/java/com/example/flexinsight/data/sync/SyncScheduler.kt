package com.example.flexinsight.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Manages scheduling of background sync work.
 */
class SyncScheduler(private val context: Context) {
    
    /**
     * Schedules periodic background sync (every 30 minutes when network is available)
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWork = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(BackgroundSyncWorker.WORK_NAME)
            .build()
        
        WorkManager.getInstance(context).enqueue(syncWork)
    }
    
    /**
     * Cancels all scheduled sync work
     */
    fun cancelPeriodicSync() {
        WorkManager.getInstance(context).cancelAllWorkByTag(BackgroundSyncWorker.WORK_NAME)
    }
}
