package com.example.flexinsight.data.repository

import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.data.model.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * Interface for exercise-related operations.
 */
interface ExerciseRepository {
    fun invalidateApiService()

    suspend fun getExerciseTemplateMapping(): Result<Map<String, String>>

    suspend fun getMuscleGroupForExercise(exercise: Exercise): String?

    fun getExercisesByWorkoutId(workoutId: String): Flow<List<Exercise>>

    fun getAllExercises(): Flow<List<Exercise>>

    suspend fun getExercisesByWorkoutIdSuspend(workoutId: String): List<Exercise>

    suspend fun getExerciseById(exerciseId: String): Exercise?

    suspend fun getExerciseHistory(templateId: String): Result<com.example.flexinsight.data.model.ExerciseHistoryResponse>
}
