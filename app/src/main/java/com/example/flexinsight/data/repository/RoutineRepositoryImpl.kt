package com.example.flexinsight.data.repository

import com.example.flexinsight.core.errors.ApiError
import com.example.flexinsight.core.errors.ErrorHandler
import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.data.model.toRoutineFolder
import com.example.flexinsight.core.logger.AppLogger
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
            return Result.error(apiServiceResult.error)
        }

        // Check network
        if (!networkMonitor.hasNetworkConnection()) {
            return Result.error(ApiError.NetworkError.NoConnection)
        }

        val apiService = (apiServiceResult as Result.Success).data

        return try {
            var page = 1
            var hasMore = true
            val allRoutines = mutableListOf<Routine>()

            // Get exercise template mapping (ID -> MuscleGroup)
            // We pass this to toRoutine, although with full details we get the title directly.
            // Keeping it for consistency or fallback.
            val exerciseTemplateMappingResult = exerciseRepository.getExerciseTemplateMapping()
            val exerciseTemplateMapping = if (exerciseTemplateMappingResult is Result.Success) {
                exerciseTemplateMappingResult.data
            } else {
                emptyMap()
            }

            // Fetch all pages of routines
            while (hasMore) {
                val response = apiService.getRoutines(page, 50)

                if (response.isSuccessful) {
                    val paginatedResponse = response.body() ?: return Result.error(
                        ApiError.Unknown("Empty response body")
                    )
                    val routineSummaries = paginatedResponse.routines

                    if (routineSummaries != null && routineSummaries.isNotEmpty()) {
                        // For each routine summary, fetch full details to get exercises
                        routineSummaries.forEach { summary ->
                            try {
                                val detailResponse = apiService.getRoutineById(summary.id)
                                if (detailResponse.isSuccessful) {
                                    val fullRoutineResponse = detailResponse.body()
                                    if (fullRoutineResponse != null) {
                                        val routine = fullRoutineResponse.toRoutine(exerciseTemplateMapping)
                                        allRoutines.add(routine)
                                    }
                                } else {
                                    AppLogger.e("Failed to fetch details for routine ${summary.id}")
                                }
                            } catch (e: Exception) {
                                AppLogger.e("Exception fetching details for routine ${summary.id}: ${e.message}")
                            }
                        }
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

            // Cache routines
            cacheManager.put(CacheKeys.ROUTINES, allRoutines)

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

    /**
     * Get routine folders
     */
    override suspend fun getRoutineFolders(): Result<List<com.example.flexinsight.data.model.RoutineFolder>> {
        // Try cache first
        val cached = cacheManager.get<List<com.example.flexinsight.data.model.RoutineFolder>>(
            CacheKeys.ROUTINE_FOLDERS,
            CacheTTL.ROUTINE_FOLDERS
        )
        if (cached != null) {
            return Result.success(cached)
        }

        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return Result.error(apiServiceResult.error)
        }

        if (!networkMonitor.hasNetworkConnection()) {
            return Result.error(ApiError.NetworkError.NoConnection)
        }

        val apiService = (apiServiceResult as Result.Success).data

        return try {
            var page = 1
            var hasMore = true
            val allFolders = mutableListOf<com.example.flexinsight.data.model.RoutineFolder>()

            while (hasMore) {
                val response = apiService.getRoutineFolders(page, 50)

                if (response.isSuccessful) {
                    val paginatedResponse = response.body() ?: return Result.error(
                        ApiError.Unknown("Empty response body")
                    )

                    val foldersList = paginatedResponse.folders
                    if (foldersList.isNotEmpty()) {
                        allFolders.addAll(foldersList.map { it.toRoutineFolder() })
                    }

                    // Check if there are more pages - though folders response might not strictly follow pageCount logic,
                    // typically it's limited. Assuming standard pagination if pageCount exists or just stop if empty.
                    // The docs sample for folders doesn't explicitly show page_count, but let's assume standard behavior or stop if empty.
                    // If page_count is not in response, we might need adjustments. Let's assume standard paginated response structure for now.
                    // Wait, I didn't see page_count in the sample response in docs for buckets/folders.
                    // Docs: {"page":1, "folders": [...]} - Missing page_count.
                    // So we loop until empty results?

                    if (foldersList.isEmpty()) {
                        hasMore = false
                    } else {
                         page++
                         // Safety break for now if logic is unsure, but typically we'd look for empty list.
                    }
                } else {
                     val error = ErrorHandler.handleHttpException(retrofit2.HttpException(response))
                     if (error is ApiError.AuthError) invalidateApiService()
                     return Result.error(error)
                }
            }

            // Refined Loop logic: Use do-while or check page count if available.
            // Since docs don't show page_count, let's just use the fact that I added PaginatedRoutineFolderResponse which likely should have it or we trust the loop.
            // Actually, checking PaginatedRoutineFolderResponse definition I created...
            // data class PaginatedRoutineFolderResponse(val page: Int, val folders: List<RoutineFolderResponse>)
            // I did NOT add pageCount. So I should iterate until empty list is returned.

            cacheManager.put(CacheKeys.ROUTINE_FOLDERS, allFolders)
            Result.success(allFolders)

        } catch (e: Exception) {
            val error = ErrorHandler.handleError(e)
            Result.error(error)
        }
    }
}
