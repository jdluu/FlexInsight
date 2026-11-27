package com.jdluu.flexinsight.domain.repository

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.ExerciseHistory
import kotlinx.coroutines.flow.Flow

/**
 * Interface for exercise-related operations.
 */
interface ExerciseRepository {
    fun invalidateApiService()

    suspend fun getExerciseTemplateMapping(): Result<Map<String, String>>

    /** Template ID → exercise title (for AI routine matching). */
    suspend fun getExerciseTemplateNameMapping(): Result<Map<String, String>>

    suspend fun getMuscleGroupForExercise(exercise: Exercise): String?

    fun getExercisesByWorkoutId(workoutId: String): Flow<List<Exercise>>

    fun getAllExercises(): Flow<List<Exercise>>

    suspend fun getExercisesByWorkoutIdSuspend(workoutId: String): List<Exercise>

    suspend fun getExerciseById(exerciseId: String): Exercise?

    suspend fun getExerciseHistory(templateId: String): Result<ExerciseHistory>
}
