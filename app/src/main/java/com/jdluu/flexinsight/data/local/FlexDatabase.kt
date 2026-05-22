package com.jdluu.flexinsight.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jdluu.flexinsight.data.local.dao.ExerciseDao
import com.jdluu.flexinsight.data.local.dao.SetDao
import com.jdluu.flexinsight.data.local.dao.WorkoutDao
import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.Workout

@Database(
    entities = [Workout::class, Exercise::class, Set::class],
    version = 2,
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

