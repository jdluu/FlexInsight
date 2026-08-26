package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.model.WorkoutResponse

/**
 * Mutation concerns for workouts: persisting API payloads, syncing with the remote API,
 * and updating local workout state.
 * Reads/queries live in [WorkoutQueryRepository].
 */
interface WorkoutMutationRepository {
    fun invalidateApiService()

    suspend fun saveWorkoutWithExercisesAndSets(workoutResponse: WorkoutResponse)

    suspend fun syncWorkouts(): Result<Unit>

    suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean, endTime: Long?): Result<Unit>

    suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit>
}
