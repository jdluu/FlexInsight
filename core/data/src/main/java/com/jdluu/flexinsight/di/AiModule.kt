package com.jdluu.flexinsight.di

import com.jdluu.flexinsight.data.ai.GeminiNanoClient
import com.jdluu.flexinsight.data.ai.HevyAiDataAccessor
import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.domain.ai.AiContextProvider
import com.jdluu.flexinsight.domain.ai.FlexAIClient
import com.jdluu.flexinsight.domain.health.HealthConnectDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindFlexAIClient(
        geminiNanoClient: GeminiNanoClient
    ): FlexAIClient

    @Binds
    @Singleton
    abstract fun bindAiContextProvider(
        hevyAiDataAccessor: HevyAiDataAccessor
    ): AiContextProvider

    @Binds
    @Singleton
    abstract fun bindHealthConnectDataSource(
        healthConnectRepository: HealthConnectRepository
    ): HealthConnectDataSource
}
