package com.example.hevyinsight.data.repository

import com.example.hevyinsight.core.errors.ApiError
import com.example.hevyinsight.core.errors.ErrorHandler
import com.example.hevyinsight.core.errors.Result
import com.example.hevyinsight.core.network.NetworkMonitor
import com.example.hevyinsight.data.api.HevyApiClient
import com.example.hevyinsight.data.api.HevyApiService
import com.example.hevyinsight.data.cache.CacheKeys
import com.example.hevyinsight.data.cache.CacheManager
import com.example.hevyinsight.data.cache.CacheTTL
import com.example.hevyinsight.data.local.dao.ExerciseDao
import com.example.hevyinsight.data.model.Exercise
import com.example.hevyinsight.data.model.ExerciseTemplate
import com.example.hevyinsight.data.model.ExerciseTemplateResponse
import com.example.hevyinsight.data.preferences.ApiKeyManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository for exercise-related operations.
 * Handles exercise templates and muscle group mapping.
 */
class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val apiKeyManager: ApiKeyManager,
    private val networkMonitor: NetworkMonitor,
    private val apiClient: HevyApiClient,
    private val cacheManager: CacheManager
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
    fun invalidateApiService() {
        apiService = null
        currentApiKey = null
        cacheManager.invalidate(CacheKeys.EXERCISE_TEMPLATES)
    }
    
    /**
     * Fetch and cache exercise templates from API
     * Returns mapping of templateId -> muscleGroup
     */
    suspend fun getExerciseTemplateMapping(): Result<Map<String, String>> {
        // Check cache first
        val cached = cacheManager.get<Map<String, String>>(
            CacheKeys.EXERCISE_TEMPLATES,
            CacheTTL.EXERCISE_TEMPLATES
        )
        if (cached != null) {
            return Result.success(cached)
        }
        
        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return apiServiceResult
        }
        
        // Check network
        if (!networkMonitor.hasNetworkConnection()) {
            return Result.error(ApiError.NetworkError.NoConnection)
        }
        
        val apiService = (apiServiceResult as Result.Success).data
        
        return try {
            val response = apiService.getExerciseTemplates()
            
            if (response.isSuccessful) {
                val templates = response.body() ?: return Result.error(
                    ApiError.Unknown("Empty response body")
                )
                val mapping = templates.mapNotNull { templateResponse ->
                    val template = templateResponse.toExerciseTemplate()
                    template.muscleGroup?.let { template.id to it }
                }.toMap()
                
                // Cache the mapping
                cacheManager.put(CacheKeys.EXERCISE_TEMPLATES, mapping)
                
                Result.success(mapping)
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
     * Get muscle group for an exercise by looking up its template ID
     * Falls back to name-based mapping if template not found
     */
    suspend fun getMuscleGroupForExercise(exercise: Exercise): String? {
        // First try to get from exercise template
        if (exercise.exerciseTemplateId != null) {
            val templateMappingResult = getExerciseTemplateMapping()
            if (templateMappingResult is Result.Success) {
                templateMappingResult.data[exercise.exerciseTemplateId]?.let { return it }
            }
        }
        
        // Fall back to name-based mapping
        return getMuscleGroupFromExerciseName(exercise.name)
    }
    
    /**
     * Get muscle group from exercise name (fallback method)
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
            name.contains("extension") || (name.contains("curl") && name.contains("leg"))) {
            return "Legs"
        }
        
        // Shoulders
        if (name.contains("shoulder") || name.contains("delt") || 
            (name.contains("press") && name.contains("shoulder")) ||
            name.contains("lateral") || name.contains("front raise")) {
            return "Shoulders"
        }
        
        // Arms
        if (name.contains("bicep") || name.contains("tricep") || name.contains("curl") ||
            (name.contains("extension") && name.contains("tricep")) || name.contains("preacher")) {
            return "Arms"
        }
        
        // Core/Abs
        if (name.contains("ab") || name.contains("core") || name.contains("crunch") ||
            name.contains("plank") || name.contains("sit-up")) {
            return "Core"
        }
        
        return null
    }
    
    /**
     * Get exercises by workout ID
     */
    fun getExercisesByWorkoutId(workoutId: String): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByWorkoutIdFlow(workoutId)
    }
    
    /**
     * Get exercises by workout ID (suspend)
     */
    suspend fun getExercisesByWorkoutIdSuspend(workoutId: String): List<Exercise> {
        return exerciseDao.getExercisesByWorkoutId(workoutId)
    }
    
    /**
     * Get exercise by ID
     */
    suspend fun getExerciseById(exerciseId: String): Exercise? {
        return exerciseDao.getExerciseById(exerciseId)
    }
}
