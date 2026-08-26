package com.jdluu.flexinsight.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.jdluu.flexinsight.core.errors.ApiError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackgroundSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncSource: HevySyncSource,
    private val syncCoordinator: SyncCoordinator
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        val syncResult = syncSource.syncAll()
        return if (syncResult.isSuccess) {
            syncCoordinator.onSyncComplete()
            Log.d(TAG, "Periodic sync worker success")
            ListenableWorker.Result.success()
        } else {
            val error = (syncResult as com.jdluu.flexinsight.core.errors.Result.Error).error
            Log.e(TAG, "Periodic sync worker failed: ${error.message}")
            when {
                error.isAuthError -> ListenableWorker.Result.failure()
                error.isRetryable -> ListenableWorker.Result.retry()
                else -> ListenableWorker.Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "BackgroundSyncWorker"
        const val WORK_NAME = "background_sync_work"
    }
}
