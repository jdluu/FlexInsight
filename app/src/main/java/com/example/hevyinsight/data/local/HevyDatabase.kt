package com.example.hevyinsight.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.hevyinsight.data.local.dao.ExerciseDao
import com.example.hevyinsight.data.local.dao.SetDao
import com.example.hevyinsight.data.local.dao.WorkoutDao
import com.example.hevyinsight.data.model.Exercise
import com.example.hevyinsight.data.model.Set
import com.example.hevyinsight.data.model.Workout

@Database(
    entities = [Workout::class, Exercise::class, Set::class],
    version = 1,
    exportSchema = false
)
abstract class HevyDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun setDao(): SetDao
    
    companion object {
        const val DATABASE_NAME = "hevy_database"
    }
}

