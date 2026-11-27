package com.jdluu.flexinsight.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.logger.AppLogger
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
            AppLogger.d("Periodic sync worker success", TAG)
            ListenableWorker.Result.success()
        } else {
            val error = (syncResult as com.jdluu.flexinsight.core.errors.Result.Error).error
            AppLogger.e("Periodic sync worker failed: ${error.message}", tag = TAG)
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
