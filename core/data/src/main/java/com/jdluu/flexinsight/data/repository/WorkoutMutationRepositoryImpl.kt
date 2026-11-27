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
import com.jdluu.flexinsight.data.local.dao.ExerciseDao
import com.jdluu.flexinsight.data.local.dao.SetDao
import com.jdluu.flexinsight.data.local.dao.WorkoutDao
import com.jdluu.flexinsight.data.mapper.EntityMapper
import com.jdluu.flexinsight.data.mapper.WorkoutMapper
import com.jdluu.flexinsight.data.model.WorkoutResponse
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.data.preferences.SyncPreferencesManager
import com.jdluu.flexinsight.data.sync.SyncManager
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.Set
import com.jdluu.flexinsight.domain.model.Workout
import com.jdluu.flexinsight.domain.repository.WorkoutMutationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Mutation/sync implementation for workouts.
 */
class WorkoutMutationRepositoryImpl(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
    private val apiKeyManager: ApiKeyManager,
    private val networkMonitor: NetworkMonitor,
    private val apiClient: FlexApiClient,
    private val cacheManager: CacheManager,
    private val syncManager: SyncManager,
    private val syncPreferencesManager: SyncPreferencesManager
) : WorkoutMutationRepository {
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
    }

    override suspend fun saveWorkout(workout: Workout, exercises: List<Exercise>, sets: List<Set>) {
        workoutDao.insertWorkoutWithDetails(
            workout = EntityMapper.toEntity(workout),
            exercises = exercises.map(EntityMapper::toEntity),
            sets = sets.map(EntityMapper::toEntity),
            exerciseDao = exerciseDao,
            setDao = setDao
        )
    }

    private suspend fun saveWorkoutWithExercisesAndSets(workoutResponse: WorkoutResponse) {
        val workout = WorkoutMapper.toWorkout(workoutResponse)
        val exercises = mutableListOf<Exercise>()
        val sets = mutableListOf<Set>()

        workoutResponse.exercises?.forEach { exerciseResponse ->
            val exercise = WorkoutMapper.toExercise(exerciseResponse, workoutResponse.id)
            exercises.add(exercise)

            exerciseResponse.sets?.forEach { setResponse ->
                sets.add(WorkoutMapper.toSet(setResponse, exercise.id))
            }
        }

        saveWorkout(workout, exercises, sets)
    }

    override suspend fun syncWorkouts(): Result<Unit> {
        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return Result.error(apiServiceResult.error)
        }

        if (!networkMonitor.hasNetworkConnection()) {
            return Result.error(ApiError.NetworkError.NoConnection)
        }

        val mostRecentSynced = workoutDao.getMostRecentSyncedTimestamp()
        val localCount = workoutDao.getWorkoutCount()

        var shouldForceFullSync = false
        try {
            val remoteCountResult = fetchRemoteWorkoutCount()
            if (remoteCountResult is Result.Success) {
                val remoteCount = remoteCountResult.data
                if (localCount < remoteCount) {
                    shouldForceFullSync = true
                    AppLogger.d("Force full sync: Local $localCount < Remote $remoteCount")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to check remote count during sync: ${e.message}")
        }

        val isIncrementalSync = mostRecentSynced != null && !shouldForceFullSync

        if (isIncrementalSync) {
            val eventsResult = syncWorkoutsFromEvents()
            if (eventsResult is Result.Success) {
                return eventsResult
            }
        }

        val apiService = (apiServiceResult as Result.Success).data

        return try {
            var page = 1
            var hasMore = true
            var allWorkoutsExist = false

            while (hasMore && !allWorkoutsExist) {
                val response = apiService.getWorkouts(page, 10)

                if (response.isSuccessful) {
                    val paginatedResponse = response.body() ?: return Result.error(
                        ApiError.Unknown("Empty response body")
                    )
                    val workoutsList = paginatedResponse.workouts

                    if (workoutsList == null || workoutsList.isEmpty()) {
                        hasMore = false
                        continue
                    }

                    val workoutIds = workoutsList.map { it.id }
                    val existingIds = if (workoutIds.isEmpty()) {
                        emptySet()
                    } else {
                        workoutDao.getExistingWorkoutIds(workoutIds).toSet()
                    }

                    if (isIncrementalSync && workoutIds.all { it in existingIds }) {
                        allWorkoutsExist = true
                        break
                    }

                    val idsToFetch = workoutIds.filter { it !in existingIds }
                    fetchAndSaveWorkoutDetails(apiService, idsToFetch)

                    hasMore = page < paginatedResponse.pageCount
                    page++
                } else {
                    val error = if (response.code() == 401 || response.code() == 403) {
                        ApiError.AuthError.InvalidApiKey
                    } else {
                        ErrorHandler.handleHttpException(retrofit2.HttpException(response))
                    }

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

    private fun timestampToIso8601(timestampMillis: Long): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return dateFormat.format(java.util.Date(timestampMillis))
    }

    private suspend fun syncWorkoutsFromEvents(): Result<Unit> {
        val apiServiceResult = getApiService()
        if (apiServiceResult is Result.Error) {
            return Result.error(apiServiceResult.error)
        }

        if (!networkMonitor.hasNetworkConnection()) {
            return Result.error(ApiError.NetworkError.NoConnection)
        }

        val apiService = (apiServiceResult as Result.Success).data

        return try {
            val mostRecentSynced = workoutDao.getMostRecentSyncedTimestamp()
            val sinceParam = mostRecentSynced?.let { timestampToIso8601(it) }
                ?: return Result.error(ApiError.Unknown("No previous sync timestamp, use regular sync"))

            var page = 1
            var hasMore = true

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

                    val idsToRefresh = mutableListOf<String>()
                    events.forEach { event ->
                        val workoutId = event.workoutId ?: return@forEach
                        when (event.type) {
                            "created", "updated" -> idsToRefresh.add(workoutId)
                            "deleted" -> {
                                workoutDao.softDeleteWorkoutById(workoutId)
                                syncPreferencesManager.recordDeletedWorkout()
                            }
                        }
                    }
                    fetchAndSaveWorkoutDetails(apiService, idsToRefresh.distinct())

                    hasMore = page < eventsResponse.pageCount
                    page++
                } else {
                    val error = if (response.code() == 401 || response.code() == 403) {
                        ApiError.AuthError.InvalidApiKey
                    } else {
                        ErrorHandler.handleHttpException(retrofit2.HttpException(response))
                    }

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

    override suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean, endTime: Long?): Result<Unit> {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return Result.error(ApiError.Unknown("Workout not found"))

        val updatedWorkout = workout.copy(
            endTime = if (isCompleted) (endTime ?: System.currentTimeMillis()) else null,
            needsSync = true
        )

        workoutDao.updateWorkout(updatedWorkout)
        syncManager.syncNow()

        return Result.success(Unit)
    }

    override suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit> {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return Result.error(ApiError.Unknown("Workout not found"))

        val updatedWorkout = workout.copy(
            startTime = newStartTime,
            needsSync = true
        )

        workoutDao.updateWorkout(updatedWorkout)
        syncManager.syncNow()

        return Result.success(Unit)
    }

    private suspend fun fetchAndSaveWorkoutDetails(
        apiService: FlexApiService,
        workoutIds: List<String>
    ) {
        if (workoutIds.isEmpty()) return

        coroutineScope {
            val semaphore = Semaphore(SYNC_DETAIL_CONCURRENCY)
            workoutIds.map { workoutId ->
                async {
                    semaphore.withPermit {
                        try {
                            val detailResponse = apiService.getWorkoutById(workoutId)
                            if (detailResponse.isSuccessful) {
                                val fullWorkout = detailResponse.body()
                                if (fullWorkout != null) {
                                    saveWorkoutWithExercisesAndSets(fullWorkout)
                                    cacheExerciseTemplatesFromWorkout(fullWorkout)
                                }
                            } else {
                                AppLogger.e("Failed to fetch details for workout $workoutId")
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Exception fetching details for workout $workoutId: ${e.message}")
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun fetchRemoteWorkoutCount(): Result<Int> {
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

    private fun cacheExerciseTemplatesFromWorkout(fullWorkout: WorkoutResponse) {
        fullWorkout.exercises?.forEach { exercise ->
            exercise.exerciseTemplateId?.let { templateId ->
                val cacheKey = CacheKeys.EXERCISE_TEMPLATES_FROM_EVENTS
                val currentCache = cacheManager.get<Map<String, String>>(
                    cacheKey,
                    CacheTTL.EXERCISE_TEMPLATES_FROM_EVENTS
                ) ?: emptyMap()
                if (!currentCache.containsKey(templateId)) {
                    cacheManager.put(cacheKey, currentCache + (templateId to exercise.title))
                }
            }
        }
    }

    companion object {
        private const val SYNC_DETAIL_CONCURRENCY = 3
    }
}
