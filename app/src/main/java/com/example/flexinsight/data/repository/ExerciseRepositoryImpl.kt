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
     * Returns null if template not found - we only use accurate API data.
     */
    override suspend fun getMuscleGroupForExercise(exercise: Exercise): String? {
        // Look up from exercise template
        if (exercise.exerciseTemplateId != null) {
            val templateMappingResult = getExerciseTemplateMapping()
            if (templateMappingResult is Result.Success) {
                return templateMappingResult.data[exercise.exerciseTemplateId]
            }
        }
        
        // Return null - don't guess based on name
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
}
