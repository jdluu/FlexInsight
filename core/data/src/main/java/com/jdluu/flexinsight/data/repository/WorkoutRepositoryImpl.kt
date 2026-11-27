package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.Set
import com.jdluu.flexinsight.domain.model.Workout
import com.jdluu.flexinsight.domain.repository.WorkoutMutationRepository
import com.jdluu.flexinsight.domain.repository.WorkoutQueryRepository
import com.jdluu.flexinsight.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow

/**
 * Facade over [WorkoutQueryRepository] and [WorkoutMutationRepository].
 */
class WorkoutRepositoryImpl(
    private val queryRepository: WorkoutQueryRepository,
    private val mutationRepository: WorkoutMutationRepository
) : WorkoutRepository {
    override fun invalidateApiService() {
        queryRepository.invalidateApiService()
        mutationRepository.invalidateApiService()
    }

    override fun getWorkouts(): Flow<List<Workout>> = queryRepository.getWorkouts()

    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> = queryRepository.getRecentWorkouts(limit)

    override suspend fun getWorkoutById(workoutId: String): Result<Workout> = queryRepository.getWorkoutById(workoutId)

    override fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> = queryRepository.getWorkoutByIdFlow(workoutId)

    override fun getWorkoutCount(): Flow<Int> = queryRepository.getWorkoutCount()

    override suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise> =
        queryRepository.getExercisesByWorkoutId(workoutId)

    override suspend fun getExercisesByWorkoutIds(workoutIds: List<String>): List<Exercise> =
        queryRepository.getExercisesByWorkoutIds(workoutIds)

    override suspend fun getSetsByExerciseId(exerciseId: String): List<Set> =
        queryRepository.getSetsByExerciseId(exerciseId)

    override suspend fun getSetsByExerciseIds(exerciseIds: List<String>): List<Set> =
        queryRepository.getSetsByExerciseIds(exerciseIds)

    override suspend fun getRemoteWorkoutCount(): Result<Int> = queryRepository.getRemoteWorkoutCount()

    override fun getWorkoutsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<Workout>> =
        queryRepository.getWorkoutsByDateRange(startTimestamp, endTimestamp)

    override suspend fun getMostRecentSyncedTimestamp(): Long? = queryRepository.getMostRecentSyncedTimestamp()

    override suspend fun syncWorkouts(): Result<Unit> = mutationRepository.syncWorkouts()

    override suspend fun saveWorkout(workout: Workout, exercises: List<Exercise>, sets: List<Set>) {
        mutationRepository.saveWorkout(workout, exercises, sets)
    }

    override suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean, endTime: Long?): Result<Unit> =
        mutationRepository.updateWorkoutStatus(workoutId, isCompleted, endTime)

    override suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit> =
        mutationRepository.rescheduleWorkout(workoutId, newStartTime)
}
