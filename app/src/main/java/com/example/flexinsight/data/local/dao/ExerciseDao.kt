package com.example.flexinsight.data.local.dao

import androidx.room.*
import com.example.flexinsight.data.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY id")
    fun getExercisesByWorkoutIdFlow(workoutId: String): Flow<List<Exercise>>
    
    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY id")
    suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise>
    
    @Query("SELECT * FROM exercises ORDER BY id DESC LIMIT 100")
    fun getAllExercises(): Flow<List<Exercise>>
    
    @Query("SELECT * FROM exercises WHERE id = :exerciseId LIMIT 1")
    suspend fun getExerciseById(exerciseId: String): Exercise?
    
    @Query("SELECT * FROM exercises WHERE exerciseTemplateId = :exerciseTemplateId ORDER BY id")
    fun getExercisesByTemplateIdFlow(exerciseTemplateId: String): Flow<List<Exercise>>
    
    @Query("SELECT * FROM exercises WHERE needsSync = 1")
    suspend fun getExercisesNeedingSync(): List<Exercise>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)
    
    @Update
    suspend fun updateExercise(exercise: Exercise)
    
    @Delete
    suspend fun deleteExercise(exercise: Exercise)
    
    @Query("DELETE FROM exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesByWorkoutId(workoutId: String)
    
    @Query("UPDATE exercises SET lastSynced = :timestamp, needsSync = 0 WHERE id = :exerciseId")
    suspend fun markExerciseSynced(exerciseId: String, timestamp: Long = System.currentTimeMillis())
}

