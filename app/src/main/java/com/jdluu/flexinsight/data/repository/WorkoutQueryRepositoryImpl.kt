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
import com.jdluu.flexinsight.data.mapper.WorkoutMapper
import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.Workout
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import kotlinx.coroutines.flow.Flow

/**
 * Read/query implementation for workouts.
 * Serves local Room data first; falls back to the API only for single-workout fetches
 * that are not cached locally.
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

    /**
     * Gets API service, creating it if needed
     */
    private suspend fun getApiService(): Result<FlexApiService> {
        val apiKey = apiKeyManager.getApiKey() ?: return Result.error(
            ApiError.AuthError.InvalidApiKey
        )

        // Recreate service if API key has changed
        if (apiService == null || currentApiKey != apiKey) {
            apiService = apiClient.createApiService(apiKey)
            currentApiKey = apiKey
        }

        val service = apiService ?: return Result.error(ApiError.Unknown("API service not initialized"))
        return Result.success(service)
    }

    /**
     * Invalidates the API service (useful when API key is updated)
     */
    override fun invalidateApiService() {
        apiService = null
        currentApiKey = null
    }

    /**
     * Get all workouts - returns Flow from Room immediately
     */
    override fun getWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkoutsFlow()
    }

    /**
     * Get recent workouts
     */
    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> {
        return workoutDao.getRecentWorkoutsFlow(limit)
    }

    /**
     * Get workout by ID - checks Room first, then API if not found
     */
    override suspend fun getWorkoutById(workoutId: String): Result<Workout> {
        // Check local database first
        val cached = workoutDao.getWorkoutById(workoutId)
        if (cached != null) {
            return Result.success(cached)
        }

        // Try to fetch from API
        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return apiServiceResult
        }

        val apiService = (apiServiceResult as Result.Success).data

        return try {
            // Check network before API call
            if (!networkMonitor.hasNetworkConnection()) {
                return Result.error(ApiError.NetworkError.NoConnection)
            }

            val response = apiService.getWorkoutById(workoutId)

            if (response.isSuccessful) {
                val workoutResponse = response.body() ?: return Result.error(
                    ApiError.Unknown("Empty response body")
                )
                val workout = WorkoutMapper.toWorkout(workoutResponse)

                // Save to database
                mutationRepository.saveWorkoutWithExercisesAndSets(workoutResponse)

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

    /**
     * Get workout by ID as Flow - returns database data immediately
     */
    override fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> {
        return workoutDao.getWorkoutByIdFlow(workoutId)
    }

    /**
     * Get workout count
     */
    override fun getWorkoutCount(): Flow<Int> {
        return workoutDao.getWorkoutCountFlow()
    }

    /**
     * Get remote workout count from API
     */
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
        return exerciseDao.getExercisesByWorkoutId(workoutId)
    }

    override suspend fun getSetsByExerciseId(exerciseId: String): List<Set> {
        return setDao.getSetsByExerciseId(exerciseId)
    }

    /**
     * Get workouts by date range
     */
    override fun getWorkoutsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<Workout>> {
        return workoutDao.getWorkoutsByDateRangeFlow(startTimestamp, endTimestamp)
    }

    /**
     * Get most recent synced timestamp
     */
    override suspend fun getMostRecentSyncedTimestamp(): Long? {
        return workoutDao.getMostRecentSyncedTimestamp()
    }
}
