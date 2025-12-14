package com.example.flexinsight.data.repository

import com.example.flexinsight.core.errors.ApiError
import com.example.flexinsight.core.errors.ErrorHandler
import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.core.network.NetworkMonitor
import com.example.flexinsight.data.api.FlexApiClient
import com.example.flexinsight.data.api.FlexApiService
import com.example.flexinsight.data.cache.CacheKeys
import com.example.flexinsight.data.cache.CacheManager
import com.example.flexinsight.data.cache.CacheTTL
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.model.Exercise
import com.example.flexinsight.data.model.ExerciseTemplate
import com.example.flexinsight.data.model.ExerciseTemplateResponse
import com.example.flexinsight.data.preferences.ApiKeyManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository for exercise-related operations.
 * Handles exercise templates and muscle group mapping.
 */
class ExerciseRepositoryImpl(
    private val exerciseDao: com.example.flexinsight.data.local.dao.ExerciseDao,
    private val apiKeyManager: ApiKeyManager,
    private val networkMonitor: com.example.flexinsight.core.network.NetworkMonitor,
    private val apiClient: com.example.flexinsight.data.api.FlexApiClient,
    private val cacheManager: com.example.flexinsight.data.cache.CacheManager
) : ExerciseRepository {
    private var apiService: FlexApiService? = null
    private var currentApiKey: String? = null
    
    /**
     * Gets API service, creating it if needed
     */
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
    
    /**
     * Invalidates the API service
     */
    override fun invalidateApiService() {
        apiService = null
        currentApiKey = null
        cacheManager.invalidate(CacheKeys.EXERCISE_TEMPLATES)
    }
    
    /**
     * Fetch and cache exercise templates from API
     * Returns mapping of templateId -> muscleGroup
     * Prefers data extracted from workout events, falls back to API cache, then API call
     */
    override suspend fun getExerciseTemplateMapping(): Result<Map<String, String>> {
        // 1. First check event-extracted templates (from workout events)
        // Note: Events data has templateId -> exerciseName, not muscleGroup
        // But we check it to see if we have template IDs available
        val eventsCached = cacheManager.get<Map<String, String>>(
            CacheKeys.EXERCISE_TEMPLATES_FROM_EVENTS,
            CacheTTL.EXERCISE_TEMPLATES_FROM_EVENTS
        )
        
        // 2. Check regular API cache (has templateId -> muscleGroup)
        val cached = cacheManager.get<Map<String, String>>(
            CacheKeys.EXERCISE_TEMPLATES,
            CacheTTL.EXERCISE_TEMPLATES
        )
        if (cached != null) {
            return Result.success(cached)
        }
        
        // 3. If we have events data but no API cache, and network is available, try API
        // Otherwise, if no network, return empty map (we can't get muscle groups without API)
        if (!networkMonitor.hasNetworkConnection()) {
            // If we have events data, return empty map (we have IDs but not muscle groups)
            // This allows the code to continue without failing
            return if (eventsCached != null) {
                Result.success(emptyMap())
            } else {
                Result.error(ApiError.NetworkError.NoConnection)
            }
        }
        
        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return apiServiceResult
        }
        
        val apiService = (apiServiceResult as Result.Success).data
        
        return try {
            var page = 1
            var hasMore = true
            val allTemplates = mutableListOf<ExerciseTemplateResponse>()
            
            // Fetch all pages of exercise templates
            while (hasMore) {
                val response = apiService.getExerciseTemplates(page, 50)
                
                if (response.isSuccessful) {
                    val paginatedResponse = response.body() ?: return Result.error(
                        ApiError.Unknown("Empty response body")
                    )
                    val templatesList = paginatedResponse.exerciseTemplates
                    
                    if (templatesList != null && templatesList.isNotEmpty()) {
                        allTemplates.addAll(templatesList)
                    }
                    
                    // Check if there are more pages
                    hasMore = page < paginatedResponse.pageCount
                    page++
                } else {
                    // If API call fails but we have events data, return empty map instead of error
                    // This allows the app to continue functioning
                    if (eventsCached != null) {
                        return Result.success(emptyMap())
                    } else {
                        val error = ErrorHandler.handleHttpException(
                            retrofit2.HttpException(response)
                        )
                        return Result.error(error)
                    }
                }
            }
            
            // Build mapping from all accumulated templates
            val mapping = allTemplates.mapNotNull { templateResponse ->
                val template = templateResponse.toExerciseTemplate()
                template.muscleGroup?.let { template.id to it }
            }.toMap()
            
            // Cache the mapping
            cacheManager.put(CacheKeys.EXERCISE_TEMPLATES, mapping)
            
            Result.success(mapping)
        } catch (e: Exception) {
            // If exception occurs but we have events data, return empty map instead of error
            if (eventsCached != null) {
                Result.success(emptyMap())
            } else {
                val error = ErrorHandler.handleError(e)
                Result.error(error)
            }
        }
    }
    
    /**
     * Get muscle group for an exercise by looking up its template ID.
     * Falls back to name-based mapping if template not found.
     * API data is preferred but name-matching provides reasonable defaults.
     */
    override suspend fun getMuscleGroupForExercise(exercise: Exercise): String? {
        // First try to get from exercise template (most accurate)
        if (exercise.exerciseTemplateId != null) {
            val templateMappingResult = getExerciseTemplateMapping()
            if (templateMappingResult is Result.Success) {
                templateMappingResult.data[exercise.exerciseTemplateId]?.let { return it }
            }
        }
        
        // Fall back to name-based mapping when API data unavailable
        return getMuscleGroupFromExerciseName(exercise.name)
    }
    
    /**
     * Get muscle group from exercise name (fallback method)
     * Used when exercise template data isn't available
     */
    private fun getMuscleGroupFromExerciseName(exerciseName: String): String? {
        val name = exerciseName.lowercase()
        
        // Chest exercises
        if (name.contains("bench") || name.contains("chest") || name.contains("pec") ||
            name.contains("fly") || (name.contains("press") && (name.contains("incline") || name.contains("decline")))) {
            return "Chest"
        }
        
        // Back exercises  
        if (name.contains("row") || name.contains("pull") || name.contains("lat") ||
            name.contains("deadlift") || name.contains("shrug") || name.contains("rear delt")) {
            return "Back"
        }
        
        // Legs exercises
        if (name.contains("squat") || name.contains("leg") || name.contains("quad") ||
            name.contains("hamstring") || name.contains("calf") || name.contains("lunge") ||
            name.contains("glute") || name.contains("hip")) {
            return "Legs"
        }
        
        // Shoulders
        if (name.contains("shoulder") || name.contains("delt") || 
            (name.contains("press") && (name.contains("overhead") || name.contains("military"))) ||
            name.contains("lateral raise") || name.contains("front raise")) {
            return "Shoulders"
        }
        
        // Arms (biceps/triceps)
        if (name.contains("bicep") || name.contains("tricep") || name.contains("curl") ||
            name.contains("pushdown") || name.contains("dip") || name.contains("preacher")) {
            return "Arms"
        }
        
        // Core/Abs
        if (name.contains("ab") || name.contains("core") || name.contains("crunch") ||
            name.contains("plank") || name.contains("sit-up") || name.contains("oblique")) {
            return "Core"
        }
        
        // Cardio
        if (name.contains("run") || name.contains("bike") || name.contains("cardio") ||
            name.contains("walk") || name.contains("elliptical") || name.contains("stair")) {
            return "Cardio"
        }
        
        return null
    }
    
    /**
     * Get exercises by workout ID
     */
    override fun getExercisesByWorkoutId(workoutId: String): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByWorkoutIdFlow(workoutId)
    }

    /**
     * Get all exercises
     */
    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises()
    }
    
    /**
     * Get exercises by workout ID (suspend)
     */
    override suspend fun getExercisesByWorkoutIdSuspend(workoutId: String): List<Exercise> {
        return exerciseDao.getExercisesByWorkoutId(workoutId)
    }
    
    /**
     * Get exercise by ID
     */
    override suspend fun getExerciseById(exerciseId: String): Exercise? {
        return exerciseDao.getExerciseById(exerciseId)
    }

    /**
     * Get exercise history from API
     */
    override suspend fun getExerciseHistory(templateId: String): Result<com.example.flexinsight.data.model.ExerciseHistoryResponse> {
        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return Result.error(apiServiceResult.error)
        }
        
        if (!networkMonitor.hasNetworkConnection()) {
            return Result.error(ApiError.NetworkError.NoConnection)
        }

        val apiService = (apiServiceResult as Result.Success).data
        
        return try {
            val response = apiService.getExerciseHistory(templateId)
            
            if (response.isSuccessful) {
                val historyResponse = response.body() ?: return Result.error(
                    ApiError.Unknown("Empty response body")
                )
                Result.success(historyResponse)
            } else {
                val error = ErrorHandler.handleHttpException(retrofit2.HttpException(response))
                Result.error(error)
            }
        } catch (e: Exception) {
            val error = ErrorHandler.handleError(e)
            Result.error(error)
        }
    }
}
