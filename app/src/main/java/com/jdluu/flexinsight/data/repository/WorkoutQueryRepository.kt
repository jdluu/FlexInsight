package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.Workout
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

    suspend fun getSetsByExerciseId(exerciseId: String): List<Set>

    suspend fun getRemoteWorkoutCount(): Result<Int>

    fun getWorkoutsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<Workout>>

    suspend fun getMostRecentSyncedTimestamp(): Long?
}
