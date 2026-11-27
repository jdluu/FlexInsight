package com.jdluu.flexinsight.domain.repository

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.Set
import com.jdluu.flexinsight.domain.model.Workout
import kotlinx.coroutines.flow.Flow

/**
 * Read/query concerns for workouts: local Room reads plus remote single-workout reads.
 * Mutations and sync writes live in [WorkoutMutationRepository].
 */
interface WorkoutQueryRepository {
    fun invalidateApiService()

    fun getWorkouts(): Flow<List<Workout>>

    fun getRecentWorkouts(limit: Int = 10): Flow<List<Workout>>

    suspend fun getWorkoutById(workoutId: String): Result<Workout>

    fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?>

    fun getWorkoutCount(): Flow<Int>

    suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise>

    suspend fun getExercisesByWorkoutIds(workoutIds: List<String>): List<Exercise> =
        workoutIds.flatMap { getExercisesByWorkoutId(it) }

    suspend fun getSetsByExerciseId(exerciseId: String): List<Set>

    suspend fun getSetsByExerciseIds(exerciseIds: List<String>): List<Set> =
        exerciseIds.flatMap { getSetsByExerciseId(it) }

    suspend fun getRemoteWorkoutCount(): Result<Int>

    fun getWorkoutsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<Workout>>

    suspend fun getMostRecentSyncedTimestamp(): Long?
}
