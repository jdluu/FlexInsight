package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.ErrorHandler
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.core.network.NetworkMonitor
import com.jdluu.flexinsight.data.api.FlexApiClient
import com.jdluu.flexinsight.data.api.FlexApiService
import com.jdluu.flexinsight.data.cache.CacheKeys
import com.jdluu.flexinsight.data.cache.CacheManager
import com.jdluu.flexinsight.data.cache.CacheTTL
import com.jdluu.flexinsight.data.local.dao.ExerciseDao
import com.jdluu.flexinsight.data.mapper.EntityMapper
import com.jdluu.flexinsight.data.mapper.ExerciseMapper
import com.jdluu.flexinsight.data.model.ExerciseTemplateResponse
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.ExerciseHistory
import com.jdluu.flexinsight.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for exercise-related operations.
 * Handles exercise templates and muscle group mapping.
 */
class ExerciseRepositoryImpl(
    private val exerciseDao: ExerciseDao,
    private val apiKeyManager: ApiKeyManager,
    private val networkMonitor: NetworkMonitor,
    private val apiClient: FlexApiClient,
    private val cacheManager: CacheManager
) : ExerciseRepository {
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
        cacheManager.invalidate(CacheKeys.EXERCISE_TEMPLATES)
        cacheManager.invalidate(CacheKeys.EXERCISE_TEMPLATE_NAMES)
    }

    override suspend fun getExerciseTemplateNameMapping(): Result<Map<String, String>> {
        val fromEvents = cacheManager.get<Map<String, String>>(
            CacheKeys.EXERCISE_TEMPLATES_FROM_EVENTS,
            CacheTTL.EXERCISE_TEMPLATES_FROM_EVENTS
        ) ?: emptyMap()

        val cached = cacheManager.get<Map<String, String>>(
            CacheKeys.EXERCISE_TEMPLATE_NAMES,
            CacheTTL.EXERCISE_TEMPLATE_NAMES
        )
        if (cached != null) {
            return Result.success(fromEvents + cached)
        }

        if (!networkMonitor.hasNetworkConnection()) {
            return if (fromEvents.isNotEmpty()) {
                Result.success(fromEvents)
            } else {
                Result.error(ApiError.NetworkError.NoConnection)
            }
        }

        return when (val templatesResult = fetchAllTemplatesFromApi()) {
            is Result.Error -> {
                if (fromEvents.isNotEmpty()) Result.success(fromEvents) else templatesResult
            }
            is Result.Success -> {
                val fromApi = templatesResult.data.associate { it.id to it.title }
                cacheManager.put(CacheKeys.EXERCISE_TEMPLATE_NAMES, fromApi)
                Result.success(fromEvents + fromApi)
            }
        }
    }

    private suspend fun fetchAllTemplatesFromApi(): Result<List<ExerciseTemplateResponse>> {
        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return apiServiceResult
        }

        val apiService = (apiServiceResult as Result.Success).data

        return try {
            var page = 1
            var hasMore = true
            val allTemplates = mutableListOf<ExerciseTemplateResponse>()

            while (hasMore) {
                val response = apiService.getExerciseTemplates(page, 50)
                if (!response.isSuccessful) {
                    val error = ErrorHandler.handleHttpException(retrofit2.HttpException(response))
                    return Result.error(error)
                }

                val paginatedResponse = response.body()
                    ?: return Result.error(ApiError.Unknown("Empty response body"))
                paginatedResponse.exerciseTemplates?.let { allTemplates.addAll(it) }
                hasMore = page < paginatedResponse.pageCount
                page++
            }

            Result.success(allTemplates)
        } catch (e: Exception) {
            Result.error(ErrorHandler.handleError(e))
        }
    }

    override suspend fun getExerciseTemplateMapping(): Result<Map<String, String>> {
        val eventsCached = cacheManager.get<Map<String, String>>(
            CacheKeys.EXERCISE_TEMPLATES_FROM_EVENTS,
            CacheTTL.EXERCISE_TEMPLATES_FROM_EVENTS
        )

        val cached = cacheManager.get<Map<String, String>>(
            CacheKeys.EXERCISE_TEMPLATES,
            CacheTTL.EXERCISE_TEMPLATES
        )
        if (cached != null) {
            return Result.success(cached)
        }

        if (!networkMonitor.hasNetworkConnection()) {
            return if (eventsCached != null) {
                Result.success(emptyMap())
            } else {
                Result.error(ApiError.NetworkError.NoConnection)
            }
        }

        return when (val templatesResult = fetchAllTemplatesFromApi()) {
            is Result.Error -> {
                if (eventsCached != null) Result.success(emptyMap()) else templatesResult
            }
            is Result.Success -> {
                val mapping = templatesResult.data.mapNotNull { templateResponse ->
                    val template = ExerciseMapper.toExerciseTemplate(templateResponse)
                    template.muscleGroup?.let { template.id to it }
                }.toMap()
                cacheManager.put(CacheKeys.EXERCISE_TEMPLATES, mapping)
                val nameMapping = templatesResult.data.associate { it.id to it.title }
                cacheManager.put(CacheKeys.EXERCISE_TEMPLATE_NAMES, nameMapping)
                Result.success(mapping)
            }
        }
    }

    override suspend fun getMuscleGroupForExercise(exercise: Exercise): String? {
        if (exercise.exerciseTemplateId != null) {
            val templateMappingResult = getExerciseTemplateMapping()
            if (templateMappingResult is Result.Success) {
                templateMappingResult.data[exercise.exerciseTemplateId]?.let { return it }
            }
        }
        return getMuscleGroupFromExerciseName(exercise.name)
    }

    private fun getMuscleGroupFromExerciseName(exerciseName: String): String? {
        val name = exerciseName.lowercase()

        if (name.contains("bench") || name.contains("chest") || name.contains("pec") ||
            name.contains("fly") || (name.contains("press") && (name.contains("incline") || name.contains("decline")))) {
            return "Chest"
        }

        if (name.contains("row") || name.contains("pull") || name.contains("lat") ||
            name.contains("deadlift") || name.contains("shrug") || name.contains("rear delt")) {
            return "Back"
        }

        if (name.contains("squat") || name.contains("leg") || name.contains("quad") ||
            name.contains("hamstring") || name.contains("calf") || name.contains("lunge") ||
            name.contains("glute") || name.contains("hip")) {
            return "Legs"
        }

        if (name.contains("shoulder") || name.contains("delt") ||
            (name.contains("press") && (name.contains("overhead") || name.contains("military"))) ||
            name.contains("lateral raise") || name.contains("front raise")) {
            return "Shoulders"
        }

        if (name.contains("bicep") || name.contains("tricep") || name.contains("curl") ||
            name.contains("pushdown") || name.contains("dip") || name.contains("preacher")) {
            return "Arms"
        }

        if (name.contains("ab") || name.contains("core") || name.contains("crunch") ||
            name.contains("plank") || name.contains("sit-up") || name.contains("oblique")) {
            return "Core"
        }

        if (name.contains("run") || name.contains("bike") || name.contains("cardio") ||
            name.contains("walk") || name.contains("elliptical") || name.contains("stair")) {
            return "Cardio"
        }

        return null
    }

    override fun getExercisesByWorkoutId(workoutId: String): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByWorkoutIdFlow(workoutId).map { list ->
            list.map(EntityMapper::toDomain)
        }
    }

    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises().map { list ->
            list.map(EntityMapper::toDomain)
        }
    }

    override suspend fun getExercisesByWorkoutIdSuspend(workoutId: String): List<Exercise> {
        return exerciseDao.getExercisesByWorkoutId(workoutId).map(EntityMapper::toDomain)
    }

    override suspend fun getExerciseById(exerciseId: String): Exercise? {
        return exerciseDao.getExerciseById(exerciseId)?.let(EntityMapper::toDomain)
    }

    override suspend fun getExerciseHistory(templateId: String): Result<ExerciseHistory> {
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
                Result.success(ExerciseMapper.toExerciseHistory(historyResponse))
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
