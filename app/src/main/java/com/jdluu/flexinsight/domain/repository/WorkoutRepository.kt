package com.jdluu.flexinsight.domain.repository

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.Set
import com.jdluu.flexinsight.domain.model.Workout
import kotlinx.coroutines.flow.Flow

/**
 * Interface for workout-related operations.
 */
interface WorkoutRepository {
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

    suspend fun syncWorkouts(): Result<Unit>

    suspend fun saveWorkout(workout: Workout, exercises: List<Exercise>, sets: List<Set>)

    suspend fun getMostRecentSyncedTimestamp(): Long?

    suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean, endTime: Long?): Result<Unit>

    suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit>
}
