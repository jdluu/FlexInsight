package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.cache.CacheKeys
import com.jdluu.flexinsight.data.model.*
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.data.sync.HevySyncSource
import kotlinx.coroutines.flow.Flow

/**
 * Main repository that delegates to specialized repositories.
 * Maintains backward compatibility with existing API while using the new architecture.
 */
class FlexRepositoryImpl(
    private val apiKeyManager: ApiKeyManager,
    networkMonitor: com.jdluu.flexinsight.core.network.NetworkMonitor,
    cacheManager: com.jdluu.flexinsight.data.cache.CacheManager,
    private val dispatcherProvider: com.jdluu.flexinsight.core.dispatchers.DispatcherProvider = com.jdluu.flexinsight.core.dispatchers.DefaultDispatcherProvider(),
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val statsRepository: StatsRepository
) : FlexRepository, HevySyncSource {
    private val cacheManager = cacheManager

    /**
     * Invalidate the API service (useful when API key is updated)
     */
    override fun invalidateApiService() {
        workoutRepository.invalidateApiService()
        exerciseRepository.invalidateApiService()
        routineRepository.invalidateApiService()
    }

    // Workout operations

    override fun getWorkouts(): Flow<List<Workout>> {
        return workoutRepository.getWorkouts()
    }

    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> {
        return workoutRepository.getRecentWorkouts(limit)
    }

    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseRepository.getAllExercises()
    }

    override suspend fun getExerciseHistory(templateId: String): Result<ExerciseHistoryResponse> {
        return exerciseRepository.getExerciseHistory(templateId)
    }

    override suspend fun getWorkoutById(workoutId: String): Workout? {
        return when (val result = workoutRepository.getWorkoutById(workoutId)) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
    }

    override fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> {
        return workoutRepository.getWorkoutByIdFlow(workoutId)
    }

    override fun getWorkoutCount(): Flow<Int> {
        return workoutRepository.getWorkoutCount()
    }

    override suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise> {
        return workoutRepository.getExercisesByWorkoutId(workoutId)
    }

    override suspend fun getSetsByExerciseId(exerciseId: String): List<com.jdluu.flexinsight.data.model.Set> {
        return workoutRepository.getSetsByExerciseId(exerciseId)
    }

    override suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean): Result<Unit> {
        return workoutRepository.updateWorkoutStatus(workoutId, isCompleted, if (isCompleted) System.currentTimeMillis() else null)
    }

    override suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit> {
        return workoutRepository.rescheduleWorkout(workoutId, newStartTime)
    }

    // Statistics operations

    override suspend fun calculateStats(): WorkoutStats {
        return statsRepository.calculateStats()
    }

    override suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats {
        return statsRepository.calculateWorkoutStats(workout)
    }

    override fun getRecentPRs(limit: Int): Flow<List<com.jdluu.flexinsight.data.model.Set>> {
        return statsRepository.getRecentPRs(limit)
    }

    override suspend fun getPRsWithDetails(limit: Int): List<PRDetails> {
        return statsRepository.getPRsWithDetails(limit)
    }

    override suspend fun getAllPRsWithDetails(): List<PRDetails> {
        return statsRepository.getAllPRsWithDetails()
    }

    override suspend fun getMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> {
        return statsRepository.getMuscleGroupProgress(weeks)
    }

    override suspend fun calculateVolumeTrend(weeks: Int): VolumeTrend {
        return statsRepository.calculateVolumeTrend(weeks)
    }

    override suspend fun getPeriodComparison(): PeriodComparison? {
        return statsRepository.getPeriodComparison()
    }

    override suspend fun getWeeklyVolumeData(weeks: Int): List<WeeklyVolumeData> {
        return statsRepository.getWeeklyVolumeData(weeks)
    }

    override suspend fun getDurationTrend(weeks: Int): List<DailyDurationData> {
        return statsRepository.getDurationTrend(weeks)
    }

    override suspend fun getWeeklyGoalProgress(target: Int): WeeklyGoalProgress {
        return statsRepository.getWeeklyGoalProgress(target)
    }

    override suspend fun getWeekCalendarData(): List<DayInfo> {
        return statsRepository.getWeekCalendarData()
    }

    override suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> {
        return statsRepository.getPlannedWorkoutsForDay(timestamp)
    }

    override suspend fun getVolumeBalance(weeks: Int): VolumeBalance {
        return statsRepository.getVolumeBalance(weeks)
    }

    override suspend fun getWeeklyProgress(weeks: Int): List<WeeklyProgress> {
        return statsRepository.getWeeklyProgress(weeks)
    }

    override suspend fun getMemberSinceDate(): Long? {
        return statsRepository.getMemberSinceDate()
    }

    override suspend fun calculateAccountAgeDays(): Int {
        return statsRepository.calculateAccountAgeDays()
    }

    override suspend fun getProfileInfo(): ProfileInfo {
        val hasApiKey = apiKeyManager.hasApiKey()

        // Try getting remote count if authorized and network available
        val remoteCount = if (hasApiKey) {
             val result = workoutRepository.getRemoteWorkoutCount()
             if (result is Result.Success) result.data else null
        } else null

        return statsRepository.getProfileInfo(hasApiKey, remoteCount)
    }

    override suspend fun getConsistencyData(days: Int): List<DayInfo> {
        return statsRepository.getConsistencyData(days)
    }

    override suspend fun getMuscleRecoveryStatus(): Map<com.jdluu.flexinsight.data.model.MuscleGroup, Float> {
        return statsRepository.getMuscleRecoveryStatus()
    }

    // Routine operations

    override fun getRoutines(): Flow<List<Routine>> {
        return routineRepository.getRoutines()
    }

    override suspend fun getRoutineById(routineId: String): Routine? {
        return when (val result = routineRepository.getRoutineById(routineId)) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
    }

    override suspend fun getRoutineFolders(): List<RoutineFolder> {
        return when (val result = routineRepository.getRoutineFolders()) {
            is Result.Success -> result.data
            is Result.Error -> emptyList()
        }
    }

    // Exercise operations (for muscle group lookup)

    private suspend fun getMuscleGroupForExercise(exercise: Exercise): String? {
        return exerciseRepository.getMuscleGroupForExercise(exercise)
    }

    // Sync operations

    /**
     * Sync all data from API (workouts, routines, exercise templates)
     * Exercise templates are synced FIRST to ensure muscle group data is available
     */
    override suspend fun syncAll(): Result<Unit> = syncAllData()

    override suspend fun syncAllData(): Result<Unit> {
        val errors = mutableListOf<com.jdluu.flexinsight.core.errors.ApiError>()

        when (val templates = exerciseRepository.getExerciseTemplateMapping()) {
            is Result.Error -> errors.add(templates.error)
            is Result.Success -> Unit
        }
        when (val workouts = workoutRepository.syncWorkouts()) {
            is Result.Error -> errors.add(workouts.error)
            is Result.Success -> Unit
        }
        when (val routines = routineRepository.syncRoutines()) {
            is Result.Error -> errors.add(routines.error)
            is Result.Success -> Unit
        }

        return mergeSyncErrors(errors).let { merged ->
            if (merged != null) merged
            else {
                statsRepository.invalidateStatsCache()
                Result.Success(Unit)
            }
        }
    }

    companion object {
        internal fun mergeSyncErrors(errors: List<com.jdluu.flexinsight.core.errors.ApiError>): Result<Unit>? {
            if (errors.isEmpty()) return null
            val message = errors.joinToString("; ") { it.message }
            return Result.Error(
                com.jdluu.flexinsight.core.errors.ApiError.Unknown(
                    if (errors.size == 1) errors.first().message else "Sync failed: $message"
                )
            )
        }
    }

    /**
     * Clear all cached data
     */
    override fun clearCache() {
        cacheManager.invalidate(CacheKeys.EXERCISE_TEMPLATES)
        cacheManager.invalidate(CacheKeys.EXERCISE_TEMPLATE_NAMES)
        cacheManager.invalidate(CacheKeys.ROUTINES)
        statsRepository.invalidateStatsCache()
    }

    /**
     * Cloud backup is not implemented; local + Hevy API sync covers all current features.
     */
    override suspend fun syncWithCloud() {
        // No-op until a cloud backend is added.
    }
}
