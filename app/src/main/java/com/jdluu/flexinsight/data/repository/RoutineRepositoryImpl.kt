package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.ErrorHandler
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.core.logger.AppLogger
import com.jdluu.flexinsight.core.network.NetworkMonitor
import com.jdluu.flexinsight.data.api.FlexApiClient
import com.jdluu.flexinsight.data.api.FlexApiService
import com.jdluu.flexinsight.data.cache.CacheKeys
import com.jdluu.flexinsight.data.cache.CacheManager
import com.jdluu.flexinsight.data.cache.CacheTTL
import com.jdluu.flexinsight.data.mapper.RoutineMapper
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.domain.model.CreateRoutineDraft
import com.jdluu.flexinsight.domain.model.Routine
import com.jdluu.flexinsight.domain.model.RoutineFolder
import com.jdluu.flexinsight.domain.repository.ExerciseRepository
import com.jdluu.flexinsight.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Repository for routine-related operations.
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
        cacheManager.invalidate(CacheKeys.ROUTINES)
    }

    override suspend fun syncRoutines(): Result<Unit> {
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
            val allRoutines = mutableListOf<Routine>()

            val exerciseTemplateMappingResult = exerciseRepository.getExerciseTemplateMapping()
            val exerciseTemplateMapping = if (exerciseTemplateMappingResult is Result.Success) {
                exerciseTemplateMappingResult.data
            } else {
                emptyMap()
            }

            while (hasMore) {
                val response = apiService.getRoutines(page, 50)

                if (response.isSuccessful) {
                    val paginatedResponse = response.body() ?: return Result.error(
                        ApiError.Unknown("Empty response body")
                    )
                    val routineSummaries = paginatedResponse.routines

                    if (!routineSummaries.isNullOrEmpty()) {
                        routineSummaries.forEach { summary ->
                            try {
                                val detailResponse = apiService.getRoutineById(summary.id)
                                if (detailResponse.isSuccessful) {
                                    val fullRoutineResponse = detailResponse.body()
                                    if (fullRoutineResponse != null) {
                                        val routine = RoutineMapper.toRoutine(fullRoutineResponse, exerciseTemplateMapping)
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

            cacheManager.put(CacheKeys.ROUTINES, allRoutines)

            Result.success(Unit)
        } catch (e: Exception) {
            val error = ErrorHandler.handleError(e)
            Result.error(error)
        }
    }

    override fun getRoutines(): Flow<List<Routine>> {
        return flow {
            val cached = cacheManager.get<List<Routine>>(
                CacheKeys.ROUTINES,
                CacheTTL.ROUTINES
            )
            emit(cached ?: emptyList())
        }
    }

    override suspend fun getRoutineById(routineId: String): Result<Routine> {
        val routines = getRoutines().first()
        val routine = routines.firstOrNull { it.id == routineId }

        return if (routine != null) {
            Result.success(routine)
        } else {
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

                    val exerciseTemplateMappingResult = exerciseRepository.getExerciseTemplateMapping()
                    val exerciseTemplateMapping = if (exerciseTemplateMappingResult is Result.Success) {
                        exerciseTemplateMappingResult.data
                    } else {
                        emptyMap()
                    }

                    val mappedRoutine = RoutineMapper.toRoutine(routineResponse, exerciseTemplateMapping)
                    Result.success(mappedRoutine)
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

    override suspend fun getRoutineFolders(): Result<List<RoutineFolder>> {
        val cached = cacheManager.get<List<RoutineFolder>>(
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
            val allFolders = mutableListOf<RoutineFolder>()

            while (hasMore) {
                val response = apiService.getRoutineFolders(page, 50)

                if (response.isSuccessful) {
                    val paginatedResponse = response.body() ?: return Result.error(
                        ApiError.Unknown("Empty response body")
                    )

                    val foldersList = paginatedResponse.folders
                    if (foldersList.isNotEmpty()) {
                        allFolders.addAll(foldersList.map { RoutineMapper.toRoutineFolder(it) })
                    }

                    if (foldersList.isEmpty()) {
                        hasMore = false
                    } else {
                        page++
                    }
                } else {
                    val error = ErrorHandler.handleHttpException(retrofit2.HttpException(response))
                    if (error is ApiError.AuthError) invalidateApiService()
                    return Result.error(error)
                }
            }

            cacheManager.put(CacheKeys.ROUTINE_FOLDERS, allFolders)
            Result.success(allFolders)
        } catch (e: Exception) {
            val error = ErrorHandler.handleError(e)
            Result.error(error)
        }
    }

    override suspend fun createRoutine(draft: CreateRoutineDraft): Result<String> {
        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return Result.error(apiServiceResult.error)
        }

        if (!networkMonitor.hasNetworkConnection()) {
            return Result.error(ApiError.NetworkError.NoConnection)
        }

        val service = (apiServiceResult as Result.Success).data

        return try {
            val request = RoutineMapper.toCreateRoutineRequest(draft)
            val response = service.createRoutine(request)

            if (response.isSuccessful) {
                val body = response.body() ?: return Result.error(
                    ApiError.Unknown("Empty response body")
                )
                AppLogger.d("Created routine: ${body.id} - ${body.title}")
                cacheManager.invalidate(CacheKeys.ROUTINES)
                Result.success(body.id)
            } else {
                val error = ErrorHandler.handleHttpException(
                    retrofit2.HttpException(response)
                )
                if (error is ApiError.AuthError) {
                    invalidateApiService()
                }
                Result.error(error)
            }
        } catch (e: Exception) {
            val error = ErrorHandler.handleError(e)
            Result.error(error)
        }
    }
}
