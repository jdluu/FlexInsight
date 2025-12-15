package com.example.flexinsight

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.flexinsight.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FlexInsightApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Schedule periodic background sync
        syncScheduler.schedulePeriodicSync()
    }
}

