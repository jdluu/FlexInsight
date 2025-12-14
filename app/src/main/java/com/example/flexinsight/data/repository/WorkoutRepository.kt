package com.example.flexinsight.data.repository

import com.example.flexinsight.core.errors.ApiError
import com.example.flexinsight.core.errors.ErrorHandler
import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.core.network.NetworkMonitor
import com.example.flexinsight.data.api.FlexApiClient
import com.example.flexinsight.data.api.FlexApiService
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.local.dao.WorkoutDao
import com.example.flexinsight.data.cache.CacheKeys
import com.example.flexinsight.data.cache.CacheManager
import com.example.flexinsight.data.cache.CacheTTL
import com.example.flexinsight.data.model.Exercise
import com.example.flexinsight.data.model.Set
import com.example.flexinsight.data.model.Workout
import com.example.flexinsight.data.model.WorkoutEvent
import com.example.flexinsight.data.model.WorkoutResponse
import com.example.flexinsight.data.preferences.ApiKeyManager
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
    private val apiClient: FlexApiClient,
    private val cacheManager: CacheManager
) {
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
     * Tries events endpoint first for incremental sync, falls back to regular workouts endpoint
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
        
        // Check if we have any workouts in the database (for incremental sync)
        val mostRecentSynced = workoutDao.getMostRecentSyncedTimestamp()
        val isIncrementalSync = mostRecentSynced != null
        
        // Try events endpoint first for incremental sync
        if (isIncrementalSync) {
            val eventsResult = syncWorkoutsFromEvents()
            if (eventsResult is Result.Success) {
                return eventsResult
            }
            // If events endpoint fails (404, etc.), fall through to regular sync
            // Don't treat this as a critical error - just fallback
        }
        
        // Fallback to regular workouts endpoint (for initial sync or if events failed)
        val apiService = (apiServiceResult as Result.Success).data
        
        return try {
            
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
                    
                    // Extract exercise template IDs from workouts for caching
                    val templateIdToNameMap = mutableMapOf<String, String>()
                    workoutsList.forEach { workoutResponse ->
                        workoutResponse.exercises?.forEach { exerciseResponse ->
                            exerciseResponse.exerciseTemplateId?.let { templateId ->
                                templateIdToNameMap[templateId] = exerciseResponse.title
                            }
                        }
                    }
                    
                    // Cache exercise template data extracted from workouts
                    if (templateIdToNameMap.isNotEmpty()) {
                        val cachedTemplateMap = cacheManager.get<Map<String, String>>(
                            CacheKeys.EXERCISE_TEMPLATES_FROM_EVENTS,
                            CacheTTL.EXERCISE_TEMPLATES_FROM_EVENTS
                        ) ?: emptyMap()
                        
                        val mergedMap = (cachedTemplateMap + templateIdToNameMap).toMap()
                        cacheManager.put(CacheKeys.EXERCISE_TEMPLATES_FROM_EVENTS, mergedMap)
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
    
    /**
     * Converts timestamp (milliseconds) to ISO 8601 format for API
     */
    private fun timestampToIso8601(timestampMillis: Long): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return dateFormat.format(java.util.Date(timestampMillis))
    }
    
    /**
     * Sync workouts from events endpoint
     * Uses incremental sync with 'since' parameter based on lastSynced timestamp
     */
    suspend fun syncWorkoutsFromEvents(): Result<Unit> {
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
            // Get most recent synced timestamp
            val mostRecentSynced = workoutDao.getMostRecentSyncedTimestamp()
            val sinceParam = mostRecentSynced?.let { timestampToIso8601(it) }
            
            // If no previous sync, return error to fallback to regular sync
            if (sinceParam == null) {
                return Result.error(ApiError.Unknown("No previous sync timestamp, use regular sync"))
            }
            
            var page = 1
            var hasMore = true
            val exerciseTemplateIds = mutableSetOf<String>()
            val templateIdToNameMap = mutableMapOf<String, String>()
            
            while (hasMore) {
                val response = apiService.getWorkoutEvents(page = page, pageSize = 10, since = sinceParam)
                
                if (response.isSuccessful) {
                    val eventsResponse = response.body() ?: return Result.error(
                        ApiError.Unknown("Empty response body")
                    )
                    val events = eventsResponse.events
                    
                    if (events == null || events.isEmpty()) {
                        hasMore = false
                        continue
                    }
                    
                    // Process each event
                    events.forEach { event ->
                        when (event.type) {
                            "created", "updated" -> {
                                // Save workout with exercises and sets
                                event.workout?.let { workoutResponse ->
                                    saveWorkoutWithExercisesAndSets(workoutResponse)
                                    
                                    // Extract exercise template IDs and names
                                    workoutResponse.exercises?.forEach { exerciseResponse ->
                                        exerciseResponse.exerciseTemplateId?.let { templateId ->
                                            exerciseTemplateIds.add(templateId)
                                            templateIdToNameMap[templateId] = exerciseResponse.title
                                        }
                                    }
                                }
                            }
                            "deleted" -> {
                                // Remove workout from database (cascade delete will handle exercises and sets)
                                event.workout?.id?.let { workoutId ->
                                    workoutDao.deleteWorkoutById(workoutId)
                                }
                            }
                        }
                    }
                    
                    // Check if there are more pages
                    hasMore = page < eventsResponse.pageCount
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
            
            // Cache exercise template data extracted from events
            // Build a map of exercise_template_id -> exercise_name
            if (templateIdToNameMap.isNotEmpty()) {
                val cachedTemplateMap: Map<String, String> = cacheManager.get<Map<String, String>>(
                    CacheKeys.EXERCISE_TEMPLATES_FROM_EVENTS,
                    CacheTTL.EXERCISE_TEMPLATES_FROM_EVENTS
                ) ?: emptyMap()
                
                // Merge with existing cache (new data takes precedence)
                val mergedMap: Map<String, String> = cachedTemplateMap + templateIdToNameMap
                cacheManager.put(CacheKeys.EXERCISE_TEMPLATES_FROM_EVENTS, mergedMap)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            val error = ErrorHandler.handleError(e)
            Result.error(error)
        }
    }

    /**
     * Update workout status (completed/incomplete)
     */
    suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean, endTime: Long?): Result<Unit> {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return Result.error(ApiError.Unknown("Workout not found"))
        
        val updatedWorkout = workout.copy(
            endTime = if (isCompleted) (endTime ?: System.currentTimeMillis()) else null,
            needsSync = true
        )
        
        workoutDao.updateWorkout(updatedWorkout)
        
        // In a real app with sync, we would queue this for sync here
        
        return Result.success(Unit)
    }

    /**
     * Reschedule a workout
     */
    suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit> {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return Result.error(ApiError.Unknown("Workout not found"))
        
        val updatedWorkout = workout.copy(
            startTime = newStartTime,
            needsSync = true
        )
        
        workoutDao.updateWorkout(updatedWorkout)
        
        // In a real app with sync, we would queue this for sync here
        
        return Result.success(Unit)
    }
}
