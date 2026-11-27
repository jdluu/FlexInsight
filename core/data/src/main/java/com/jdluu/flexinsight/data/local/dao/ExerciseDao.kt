package com.jdluu.flexinsight.data.local.dao

import androidx.room.*
import com.jdluu.flexinsight.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY id")
    fun getExercisesByWorkoutIdFlow(workoutId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY id")
    suspend fun getExercisesByWorkoutId(workoutId: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises ORDER BY id DESC LIMIT 100")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId LIMIT 1")
    suspend fun getExerciseById(exerciseId: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE exerciseTemplateId = :exerciseTemplateId ORDER BY id")
    fun getExercisesByTemplateIdFlow(exerciseTemplateId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE needsSync = 1")
    suspend fun getExercisesNeedingSync(): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesByWorkoutId(workoutId: String)

    @Query("UPDATE exercises SET lastSynced = :timestamp, needsSync = 0 WHERE id = :exerciseId")
    suspend fun markExerciseSynced(exerciseId: String, timestamp: Long = System.currentTimeMillis())
}
