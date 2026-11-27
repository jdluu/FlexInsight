package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.ErrorHandler
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.core.network.NetworkMonitor
import com.jdluu.flexinsight.data.api.FlexApiClient
import com.jdluu.flexinsight.data.api.FlexApiService
import com.jdluu.flexinsight.data.local.dao.ExerciseDao
import com.jdluu.flexinsight.data.local.dao.SetDao
import com.jdluu.flexinsight.data.local.dao.WorkoutDao
import com.jdluu.flexinsight.data.mapper.EntityMapper
import com.jdluu.flexinsight.data.mapper.WorkoutMapper
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.Set
import com.jdluu.flexinsight.domain.model.Workout
import com.jdluu.flexinsight.domain.repository.WorkoutMutationRepository
import com.jdluu.flexinsight.domain.repository.WorkoutQueryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Read/query implementation for workouts.
 */
class WorkoutQueryRepositoryImpl(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
    private val apiKeyManager: ApiKeyManager,
    private val networkMonitor: NetworkMonitor,
    private val apiClient: FlexApiClient,
    private val mutationRepository: WorkoutMutationRepository
) : WorkoutQueryRepository {
    private var apiService: FlexApiService? = null
    private var currentApiKey: String? = null

    private suspend fun getApiService(): Result<FlexApiService> {
        val apiKey = apiKeyManager.getApiKey() ?: return Result.error(
            ApiError.AuthError.InvalidApiKey
        )

        if (apiService == null || currentApiKey != apiKey) {
            apiService = apiClient.createApiService(apiKey)
            currentApiKey = apiKey
        }

        val service = apiService ?: return Result.error(ApiError.Unknown("API service not initialized"))
        return Result.success(service)
    }

    override fun invalidateApiService() {
        apiService = null
        currentApiKey = null
    }

    override fun getWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkoutsFlow().map { list -> list.map(EntityMapper::toDomain) }
    }

    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> {
        return workoutDao.getRecentWorkoutsFlow(limit).map { list -> list.map(EntityMapper::toDomain) }
    }

    override suspend fun getWorkoutById(workoutId: String): Result<Workout> {
        val cached = workoutDao.getWorkoutById(workoutId)
        if (cached != null) {
            return Result.success(EntityMapper.toDomain(cached))
        }

        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return apiServiceResult
        }

        val apiService = (apiServiceResult as Result.Success).data

        return try {
            if (!networkMonitor.hasNetworkConnection()) {
                return Result.error(ApiError.NetworkError.NoConnection)
            }

            val response = apiService.getWorkoutById(workoutId)

            if (response.isSuccessful) {
                val workoutResponse = response.body() ?: return Result.error(
                    ApiError.Unknown("Empty response body")
                )
                val workout = WorkoutMapper.toWorkout(workoutResponse)
                val exercises = mutableListOf<Exercise>()
                val sets = mutableListOf<Set>()
                workoutResponse.exercises?.forEach { exerciseResponse ->
                    val exercise = WorkoutMapper.toExercise(exerciseResponse, workoutResponse.id)
                    exercises.add(exercise)
                    exerciseResponse.sets?.forEach { setResponse ->
                        sets.add(WorkoutMapper.toSet(setResponse, exercise.id))
                    }
                }
                mutationRepository.saveWorkout(workout, exercises, sets)

                Result.success(workout)
            } else {
                val error = ErrorHandler.handleHttpException(
                    retrofit2.HttpException(response)
                )
                Result.error(error)
            }
        } catch (e: Exception) {
            val error = ErrorHandler.handleError(e)
            Result.error(error)
        }
    }

    override fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> {
        return workoutDao.getWorkoutByIdFlow(workoutId).map { it?.let(EntityMapper::toDomain) }
    }

    override fun getWorkoutCount(): Flow<Int> {
        return workoutDao.getWorkoutCountFlow()
    }

    override suspend fun getRemoteWorkoutCount(): Result<Int> {
        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return Result.error(apiServiceResult.error)
        }

        val apiService = (apiServiceResult as Result.Success).data

        return try {
            if (!networkMonitor.hasNetworkConnection()) {
                return Result.error(ApiError.NetworkError.NoConnection)
            }

            val response = apiService.getWorkoutCount()
            if (response.isSuccessful) {
                val countResponse = response.body()
                if (countResponse != null) {
                    Result.success(countResponse.workoutCount)
                } else {
                    Result.error(ApiError.Unknown("Empty response body"))
                }
            } else {
                val error = ErrorHandler.handleHttpException(retrofit2.HttpException(response))
                Result.error(error)
            }
        } catch (e: Exception) {
            val error = ErrorHandler.handleError(e)
            Result.error(error)
        }
    }

    override suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise> {
        return exerciseDao.getExercisesByWorkoutId(workoutId).map(EntityMapper::toDomain)
    }

    override suspend fun getExercisesByWorkoutIds(workoutIds: List<String>): List<Exercise> {
        if (workoutIds.isEmpty()) return emptyList()
        return workoutIds.distinct().chunked(SQLITE_MAX_BIND_PARAMS).flatMap { chunk ->
            exerciseDao.getExercisesByWorkoutIds(chunk).map(EntityMapper::toDomain)
        }
    }

    override suspend fun getSetsByExerciseId(exerciseId: String): List<Set> {
        return setDao.getSetsByExerciseId(exerciseId).map(EntityMapper::toDomain)
    }

    override suspend fun getSetsByExerciseIds(exerciseIds: List<String>): List<Set> {
        if (exerciseIds.isEmpty()) return emptyList()
        return exerciseIds.distinct().chunked(SQLITE_MAX_BIND_PARAMS).flatMap { chunk ->
            setDao.getSetsByExerciseIds(chunk).map(EntityMapper::toDomain)
        }
    }

    override fun getWorkoutsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<Workout>> {
        return workoutDao.getWorkoutsByDateRangeFlow(startTimestamp, endTimestamp).map { list ->
            list.map(EntityMapper::toDomain)
        }
    }

    override suspend fun getMostRecentSyncedTimestamp(): Long? {
        return workoutDao.getMostRecentSyncedTimestamp()
    }

    companion object {
        private const val SQLITE_MAX_BIND_PARAMS = 500
    }
}
