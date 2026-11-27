package com.jdluu.flexinsight.domain.repository

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.Set
import com.jdluu.flexinsight.domain.model.Workout

/**
 * Mutation concerns for workouts: persisting synced workouts, remote sync,
 * and updating local workout state.
 * Reads/queries live in [WorkoutQueryRepository].
 */
interface WorkoutMutationRepository {
    fun invalidateApiService()

    suspend fun saveWorkout(workout: Workout, exercises: List<Exercise>, sets: List<Set>)

    suspend fun syncWorkouts(): Result<Unit>

    suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean, endTime: Long?): Result<Unit>

    suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit>
}
