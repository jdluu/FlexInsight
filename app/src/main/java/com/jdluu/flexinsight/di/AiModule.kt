package com.jdluu.flexinsight.di

import com.jdluu.flexinsight.data.ai.FlexAIClient
import com.jdluu.flexinsight.data.ai.GeminiNanoClient
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
}
