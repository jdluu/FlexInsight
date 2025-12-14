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
import com.example.flexinsight.data.model.Routine
import com.example.flexinsight.data.model.RoutineResponse
import com.example.flexinsight.data.preferences.ApiKeyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Repository for routine-related operations.
 * Handles fetching and caching routines from API.
 */
class RoutineRepositoryImpl(
    private val apiKeyManager: ApiKeyManager,
    private val networkMonitor: NetworkMonitor,
    private val apiClient: FlexApiClient,
    private val cacheManager: CacheManager,
    private val exerciseRepository: ExerciseRepository
) : RoutineRepository {
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
        cacheManager.invalidate(CacheKeys.ROUTINES)
    }
    
    /**
     * Sync routines from API
     */
    override suspend fun syncRoutines(): Result<Unit> {
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
            var page = 1
            var hasMore = true
            val allRoutineResponses = mutableListOf<RoutineResponse>()

            // Fetch all pages of routines
            while (hasMore) {
                val response = apiService.getRoutines(page, 50)

                if (response.isSuccessful) {
                    val paginatedResponse = response.body() ?: return Result.error(
                        ApiError.Unknown("Empty response body")
                    )
                    val routinesList = paginatedResponse.routines

                    if (routinesList != null && routinesList.isNotEmpty()) {
                        allRoutineResponses.addAll(routinesList)
                    }

                    // Check if there are more pages
                    hasMore = page < paginatedResponse.pageCount
                    page++
                } else {
                    val error = ErrorHandler.handleHttpException(
                        retrofit2.HttpException(response)
                    )
                    
                    if (error is ApiError.AuthError) {
                        invalidateApiService()
                    }
                    
                    return Result.error(error)
                }
            }

            // Get exercise template mapping for routine conversion
            val exerciseTemplateMappingResult = exerciseRepository.getExerciseTemplateMapping()
            val exerciseTemplateMapping = if (exerciseTemplateMappingResult is Result.Success) {
                exerciseTemplateMappingResult.data
            } else {
                emptyMap()
            }

            val routinesList = allRoutineResponses.map { routineResponse ->
                routineResponse.toRoutine(exerciseTemplateMapping)
            }

            // Cache routines
            cacheManager.put(CacheKeys.ROUTINES, routinesList)

            Result.success(Unit)
        } catch (e: Exception) {
            val error = ErrorHandler.handleError(e)
            Result.error(error)
        }
    }
    
    /**
     * Get all routines - returns cached data immediately
     */
    override fun getRoutines(): Flow<List<Routine>> {
        return flow {
            val cached = cacheManager.get<List<Routine>>(
                CacheKeys.ROUTINES,
                CacheTTL.ROUTINES
            )
            emit(cached ?: emptyList())
        }
    }
    
    /**
     * Get routine by ID
     */
    override suspend fun getRoutineById(routineId: String): Result<Routine> {
        val routinesResult = getRoutines()
        val routines = routinesResult.first()
        val routine = routines.firstOrNull { it.id == routineId }
        
        return if (routine != null) {
            Result.success(routine)
        } else {
            // Try to fetch from API if not in cache
            val apiServiceResult = getApiService()
            if (apiServiceResult is Result.Error) {
                return apiServiceResult
            }
            
            if (!networkMonitor.hasNetworkConnection()) {
                return Result.error(ApiError.NetworkError.NoConnection)
            }
            
            val apiService = (apiServiceResult as Result.Success).data
            
            try {
                val response = apiService.getRoutineById(routineId)
                
                if (response.isSuccessful) {
                    val routineResponse = response.body() ?: return Result.error(
                        ApiError.Unknown("Empty response body")
                    )
                    
                    // Get exercise template mapping
                    val exerciseTemplateMappingResult = exerciseRepository.getExerciseTemplateMapping()
                    val exerciseTemplateMapping = if (exerciseTemplateMappingResult is Result.Success) {
                        exerciseTemplateMappingResult.data
                    } else {
                        emptyMap()
                    }
                    
                    val routine = routineResponse.toRoutine(exerciseTemplateMapping)
                    Result.success(routine)
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
    }
}
