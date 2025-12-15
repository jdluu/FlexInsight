package com.example.flexinsight.data.repository

import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.data.cache.CacheKeys
import com.example.flexinsight.data.local.FlexDatabase
import com.example.flexinsight.data.model.*
import com.example.flexinsight.data.preferences.ApiKeyManager
import kotlinx.coroutines.flow.Flow

/**
 * Main repository that delegates to specialized repositories.
 * Maintains backward compatibility with existing API while using the new architecture.
 */
class FlexRepositoryImpl(
    database: FlexDatabase,
    private val apiKeyManager: ApiKeyManager,
    networkMonitor: com.example.flexinsight.core.network.NetworkMonitor,
    cacheManager: com.example.flexinsight.data.cache.CacheManager,
    private val dispatcherProvider: com.example.flexinsight.core.dispatchers.DispatcherProvider = com.example.flexinsight.core.dispatchers.DefaultDispatcherProvider(),
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val statsRepository: StatsRepository
) : FlexRepository {
    private val workoutDao = database.workoutDao()
    private val exerciseDao = database.exerciseDao()
    private val setDao = database.setDao()

    private val apiClient = com.example.flexinsight.data.api.FlexApiClient()

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

    override fun getRecentPRs(limit: Int): Flow<List<com.example.flexinsight.data.model.Set>> {
        return statsRepository.getRecentPRs(limit)
    }

    override suspend fun getPRsWithDetails(limit: Int): List<PRDetails> {
        return statsRepository.getPRsWithDetails(limit)
    }

    override suspend fun getMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> {
        return statsRepository.getMuscleGroupProgress(weeks)
    }

    override suspend fun calculateVolumeTrend(weeks: Int): VolumeTrend {
        return statsRepository.calculateVolumeTrend(weeks)
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
    override suspend fun syncAllData() {
        // 1. Sync exercise templates FIRST (needed for accurate muscle group data)
        exerciseRepository.getExerciseTemplateMapping()

        // 2. Sync workouts
        workoutRepository.syncWorkouts()

        // 3. Sync routines
        routineRepository.syncRoutines()

        // 4. Invalidate stats cache after sync to recalculate with new data
        statsRepository.invalidateStatsCache()
    }

    /**
     * Clear all cached data
     */
    override fun clearCache() {
        cacheManager.invalidate(CacheKeys.EXERCISE_TEMPLATES)
        cacheManager.invalidate(CacheKeys.ROUTINES)
        statsRepository.invalidateStatsCache()
    }

    /**
     * Sync with cloud database (structure ready, implementation TBD)
     */
    override suspend fun syncWithCloud() {
        // TODO: Implement cloud sync when database service is chosen
    }
}
