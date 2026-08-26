package com.jdluu.flexinsight.di

import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.data.preferences.ApiKeyStatusSource
import com.jdluu.flexinsight.data.sync.ManualSyncScheduler
import com.jdluu.flexinsight.data.sync.SyncCompleteListener
import com.jdluu.flexinsight.data.sync.SyncCoordinator
import com.jdluu.flexinsight.data.sync.SyncManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds narrow sync/api-key abstractions used by view models so they can be
 * faked in unit tests without touching WorkManager or encrypted storage.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindApiKeyStatusSource(apiKeyManager: ApiKeyManager): ApiKeyStatusSource

    @Binds
    @Singleton
    abstract fun bindManualSyncScheduler(syncManager: SyncManager): ManualSyncScheduler

    @Binds
    @Singleton
    abstract fun bindSyncCompleteListener(syncCoordinator: SyncCoordinator): SyncCompleteListener
}
