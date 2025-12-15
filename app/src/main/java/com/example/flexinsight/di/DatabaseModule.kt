package com.example.flexinsight.di

import android.content.Context
import androidx.room.Room
import com.example.flexinsight.data.local.FlexDatabase
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.local.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFlexDatabase(
        @ApplicationContext context: Context
    ): FlexDatabase {
        return Room.databaseBuilder(
            context,
            FlexDatabase::class.java,
            FlexDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideWorkoutDao(database: FlexDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideExerciseDao(database: FlexDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    fun provideSetDao(database: FlexDatabase): SetDao {
        return database.setDao()
    }
}
