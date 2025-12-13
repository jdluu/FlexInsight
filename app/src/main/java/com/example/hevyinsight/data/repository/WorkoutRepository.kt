package com.example.hevyinsight.data.repository

import com.example.hevyinsight.core.errors.ApiError
import com.example.hevyinsight.core.errors.ErrorHandler
import com.example.hevyinsight.core.errors.Result
import com.example.hevyinsight.core.network.NetworkMonitor
import com.example.hevyinsight.data.api.HevyApiClient
import com.example.hevyinsight.data.api.HevyApiService
import com.example.hevyinsight.data.local.dao.ExerciseDao
import com.example.hevyinsight.data.local.dao.SetDao
import com.example.hevyinsight.data.local.dao.WorkoutDao
import com.example.hevyinsight.data.model.Exercise
import com.example.hevyinsight.data.model.Set
import com.example.hevyinsight.data.model.Workout
import com.example.hevyinsight.data.model.WorkoutResponse
import com.example.hevyinsight.data.preferences.ApiKeyManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository for workout-related operations.
 * Handles CRUD operations and sync with API.
 */
class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
    private val apiKeyManager: ApiKeyManager,
    private val networkMonitor: NetworkMonitor,
    private val apiClient: HevyApiClient
) {
    private var apiService: HevyApiService? = null
    private var currentApiKey: String? = null
    
    /**
     * Gets API service, creating it if needed
     */
    private suspend fun getApiService(): Result<HevyApiService> {
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
    fun invalidateApiService() {
        apiService = null
        currentApiKey = null
    }
    
    /**
     * Get all workouts - returns Flow from Room immediately
     */
    fun getWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkoutsFlow()
    }
    
    /**
     * Get recent workouts
     */
    fun getRecentWorkouts(limit: Int = 10): Flow<List<Workout>> {
        return workoutDao.getRecentWorkoutsFlow(limit)
    }
    
    /**
     * Get workout by ID - checks Room first, then API if not found
     */
    suspend fun getWorkoutById(workoutId: String): Result<Workout> {
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
                val workout = workoutResponse.toWorkout()
                
                // Save to database
                saveWorkoutWithExercisesAndSets(workoutResponse)
                
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
    fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> {
        return workoutDao.getWorkoutByIdFlow(workoutId)
    }
    
    /**
     * Get workout count
     */
    fun getWorkoutCount(): Flow<Int> {
        return workoutDao.getWorkoutCountFlow()
    }
    
    /**
     * Get workouts by date range
     */
    fun getWorkoutsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<Workout>> {
        return workoutDao.getWorkoutsByDateRangeFlow(startTimestamp, endTimestamp)
    }
    
    /**
     * Sync workouts from API
     * Implements incremental sync: only fetches new workouts if database already has data
     */
    suspend fun syncWorkouts(): Result<Unit> {
        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return Result.error(apiServiceResult.error)
        }
        
        // Check network before syncing
        if (!networkMonitor.hasNetworkConnection()) {
            return Result.error(ApiError.NetworkError.NoConnection)
        }
        
        val apiService = (apiServiceResult as Result.Success).data
        
        return try {
            // Check if we have any workouts in the database
            val mostRecentSynced = workoutDao.getMostRecentSyncedTimestamp()
            val isIncrementalSync = mostRecentSynced != null
            
            var page = 1
            var hasMore = true
            var allWorkoutsExist = false
            
            while (hasMore && !allWorkoutsExist) {
                val response = apiService.getWorkouts(page, 50)
                
                if (response.isSuccessful) {
                    val paginatedResponse = response.body() ?: return Result.error(
                        ApiError.Unknown("Empty response body")
                    )
                    val workoutsList = paginatedResponse.workouts
                    
                    if (workoutsList == null || workoutsList.isEmpty()) {
                        hasMore = false
                        continue
                    }
                    
                    // For incremental sync, check if all workouts in this page already exist
                    if (isIncrementalSync) {
                        var newWorkoutsCount = 0
                        workoutsList.forEach { workoutResponse ->
                            val existingWorkout = workoutDao.getWorkoutById(workoutResponse.id)
                            if (existingWorkout == null) {
                                newWorkoutsCount++
                            }
                        }
                        
                        // If all workouts already exist, we've caught up - stop syncing
                        if (newWorkoutsCount == 0) {
                            allWorkoutsExist = true
                            break
                        }
                    }
                    
                    // Save workouts, exercises, and sets
                    workoutsList.forEach { workoutResponse ->
                        saveWorkoutWithExercisesAndSets(workoutResponse)
                    }
                    
                    // Check if there are more pages
                    hasMore = page < paginatedResponse.pageCount
                    page++
                } else {
                    val error = if (response.code() == 401 || response.code() == 403) {
                        ApiError.AuthError.InvalidApiKey
                    } else {
                        ErrorHandler.handleHttpException(retrofit2.HttpException(response))
                    }
                    
                    // Invalidate API service on auth error
                    if (error is ApiError.AuthError) {
                        invalidateApiService()
                    }
                    
                    return Result.error(error)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            val error = ErrorHandler.handleError(e)
            Result.error(error)
        }
    }
    
    /**
     * Saves a workout with its exercises and sets to the database
     */
    private suspend fun saveWorkoutWithExercisesAndSets(workoutResponse: WorkoutResponse) {
        val workout = workoutResponse.toWorkout()
        workoutDao.insertWorkout(workout)
        
        // Insert exercises and sets
        workoutResponse.exercises?.forEach { exerciseResponse ->
            val exercise = exerciseResponse.toExercise(workoutResponse.id)
            exerciseDao.insertExercise(exercise)
            
            exerciseResponse.sets?.forEach { setResponse ->
                val set = setResponse.toSet(exercise.id)
                setDao.insertSet(set)
            }
        }
    }
    
    /**
     * Get most recent synced timestamp
     */
    suspend fun getMostRecentSyncedTimestamp(): Long? {
        return workoutDao.getMostRecentSyncedTimestamp()
    }
}
