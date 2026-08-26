package com.jdluu.flexinsight

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jdluu.flexinsight.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FlexInsightApplication : Application(), Configuration.Provider {

    // workerFactory must be injected before syncManager: SyncManager's constructor
    // initializes WorkManager on demand, which reads workManagerConfiguration.
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: SyncManager

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

