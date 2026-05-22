package com.jdluu.flexinsight

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jdluu.flexinsight.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FlexInsightApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Schedule periodic background sync
        syncManager.schedulePeriodicSync()
    }
}

