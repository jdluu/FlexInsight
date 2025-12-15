package com.example.flexinsight.di

import com.example.flexinsight.data.api.FlexApiClient
import com.example.flexinsight.data.api.FlexApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideFlexApiClient(): FlexApiClient {
        return FlexApiClient()
    }
}
