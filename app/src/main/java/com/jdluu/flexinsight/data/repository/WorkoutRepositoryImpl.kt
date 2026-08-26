package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.Workout
import com.jdluu.flexinsight.data.model.WorkoutResponse
import kotlinx.coroutines.flow.Flow

/**
 * Repository for workout-related operations.
 * Facade over [WorkoutQueryRepository] (reads/queries) and [WorkoutMutationRepository]
 * (mutations and sync writes), kept for backward compatibility with existing
 * ViewModels and use cases.
 */
class WorkoutRepositoryImpl(
    private val queryRepository: WorkoutQueryRepository,
    private val mutationRepository: WorkoutMutationRepository
) : WorkoutRepository {
    override fun invalidateApiService() {
        queryRepository.invalidateApiService()
        mutationRepository.invalidateApiService()
    }

    override fun getWorkouts(): Flow<List<Workout>> {
        return queryRepository.getWorkouts()
    }

    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> {
        return queryRepository.getRecentWorkouts(limit)
    }

    override suspend fun getWorkoutById(workoutId: String): Result<Workout> {
        return queryRepository.getWorkoutById(workoutId)
    }

    override fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> {
        return queryRepository.getWorkoutByIdFlow(workoutId)
    }

    override fun getWorkoutCount(): Flow<Int> {
        return queryRepository.getWorkoutCount()
    }

    override suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise> {
        return queryRepository.getExercisesByWorkoutId(workoutId)
    }

    override suspend fun getSetsByExerciseId(exerciseId: String): List<Set> {
        return queryRepository.getSetsByExerciseId(exerciseId)
    }

    override suspend fun getRemoteWorkoutCount(): Result<Int> {
        return queryRepository.getRemoteWorkoutCount()
    }

    override fun getWorkoutsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<Workout>> {
        return queryRepository.getWorkoutsByDateRange(startTimestamp, endTimestamp)
    }

    override suspend fun getMostRecentSyncedTimestamp(): Long? {
        return queryRepository.getMostRecentSyncedTimestamp()
    }

    override suspend fun syncWorkouts(): Result<Unit> {
        return mutationRepository.syncWorkouts()
    }

    override suspend fun saveWorkoutWithExercisesAndSets(workoutResponse: WorkoutResponse) {
        mutationRepository.saveWorkoutWithExercisesAndSets(workoutResponse)
    }

    override suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean, endTime: Long?): Result<Unit> {
        return mutationRepository.updateWorkoutStatus(workoutId, isCompleted, endTime)
    }

    override suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit> {
        return mutationRepository.rescheduleWorkout(workoutId, newStartTime)
    }
}
