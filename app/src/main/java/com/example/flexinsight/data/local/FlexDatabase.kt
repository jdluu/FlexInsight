package com.example.flexinsight.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.local.dao.WorkoutDao
import com.example.flexinsight.data.model.Exercise
import com.example.flexinsight.data.model.Set
import com.example.flexinsight.data.model.Workout

@Database(
    entities = [Workout::class, Exercise::class, Set::class],
    version = 1,
    exportSchema = false
)
abstract class FlexDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun setDao(): SetDao

    companion object {
        const val DATABASE_NAME = "flex_database"
    }
}

