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
class FlexRepository(
    database: FlexDatabase,
    private val apiKeyManager: ApiKeyManager,
    networkMonitor: com.example.flexinsight.core.network.NetworkMonitor,
    cacheManager: com.example.flexinsight.data.cache.CacheManager
) {
    private val workoutDao = database.workoutDao()
    private val exerciseDao = database.exerciseDao()
    private val setDao = database.setDao()
    
    private val apiClient = com.example.flexinsight.data.api.FlexApiClient()
    
    // Specialized repositories
    private val exerciseRepository = ExerciseRepository(
        exerciseDao = exerciseDao,
        apiKeyManager = apiKeyManager,
        networkMonitor = networkMonitor,
        apiClient = apiClient,
        cacheManager = cacheManager
    )
    
    private val workoutRepository = WorkoutRepository(
        workoutDao = workoutDao,
        exerciseDao = exerciseDao,
        setDao = setDao,
        apiKeyManager = apiKeyManager,
        networkMonitor = networkMonitor,
        apiClient = apiClient,
        cacheManager = cacheManager
    )
    
    private val routineRepository = RoutineRepository(
        apiKeyManager = apiKeyManager,
        networkMonitor = networkMonitor,
        apiClient = apiClient,
        cacheManager = cacheManager,
        exerciseRepository = exerciseRepository
    )
    
    private val statsRepository = StatsRepository(
        workoutDao = workoutDao,
        exerciseDao = exerciseDao,
        setDao = setDao,
        exerciseRepository = exerciseRepository,
        cacheManager = cacheManager
    )
    
    private val cacheManager = cacheManager
    
    /**
     * Invalidate the API service (useful when API key is updated)
     */
    fun invalidateApiService() {
        workoutRepository.invalidateApiService()
        exerciseRepository.invalidateApiService()
        routineRepository.invalidateApiService()
    }
    
    // Workout operations
    
    fun getWorkouts(): Flow<List<Workout>> {
        return workoutRepository.getWorkouts()
    }
    
    fun getRecentWorkouts(limit: Int = 10): Flow<List<Workout>> {
        return workoutRepository.getRecentWorkouts(limit)
    }

    fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseRepository.getAllExercises()
    }
    
    suspend fun getWorkoutById(workoutId: String): Workout? {
        return when (val result = workoutRepository.getWorkoutById(workoutId)) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
    }
    
    fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> {
        return workoutRepository.getWorkoutByIdFlow(workoutId)
    }
    
    fun getWorkoutCount(): Flow<Int> {
        return workoutRepository.getWorkoutCount()
    }

    suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean): Result<Unit> {
        return workoutRepository.updateWorkoutStatus(workoutId, isCompleted, if (isCompleted) System.currentTimeMillis() else null)
    }

    suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit> {
        return workoutRepository.rescheduleWorkout(workoutId, newStartTime)
    }
    
    // Statistics operations
    
    suspend fun calculateStats(): WorkoutStats {
        return statsRepository.calculateStats()
    }
    
    suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats {
        return statsRepository.calculateWorkoutStats(workout)
    }
    
    fun getRecentPRs(limit: Int = 10): Flow<List<com.example.flexinsight.data.model.Set>> {
        return statsRepository.getRecentPRs(limit)
    }
    
    suspend fun getPRsWithDetails(limit: Int = 10): List<PRDetails> {
        return statsRepository.getPRsWithDetails(limit)
    }
    
    suspend fun getMuscleGroupProgress(weeks: Int = 4): List<MuscleGroupProgress> {
        return statsRepository.getMuscleGroupProgress(weeks)
    }
    
    suspend fun calculateVolumeTrend(weeks: Int = 4): VolumeTrend {
        return statsRepository.calculateVolumeTrend(weeks)
    }
    
    suspend fun getWeeklyVolumeData(weeks: Int = 4): List<WeeklyVolumeData> {
        return statsRepository.getWeeklyVolumeData(weeks)
    }
    
    suspend fun getDurationTrend(weeks: Int = 6): List<DailyDurationData> {
        return statsRepository.getDurationTrend(weeks)
    }
    
    suspend fun getWeeklyGoalProgress(target: Int = 5): WeeklyGoalProgress {
        return statsRepository.getWeeklyGoalProgress(target)
    }
    
    suspend fun getWeekCalendarData(): List<DayInfo> {
        return statsRepository.getWeekCalendarData()
    }
    
    suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> {
        return statsRepository.getPlannedWorkoutsForDay(timestamp)
    }
    
    suspend fun getVolumeBalance(weeks: Int = 4): VolumeBalance {
        return statsRepository.getVolumeBalance(weeks)
    }
    
    suspend fun getWeeklyProgress(weeks: Int = 4): List<WeeklyProgress> {
        return statsRepository.getWeeklyProgress(weeks)
    }
    
    suspend fun getMemberSinceDate(): Long? {
        return statsRepository.getMemberSinceDate()
    }
    
    suspend fun calculateAccountAgeDays(): Int {
        return statsRepository.calculateAccountAgeDays()
    }
    
    suspend fun getProfileInfo(): ProfileInfo {
        val hasApiKey = apiKeyManager.hasApiKey()
        return statsRepository.getProfileInfo(hasApiKey)
    }
    
    // Routine operations
    
    fun getRoutines(): Flow<List<Routine>> {
        return routineRepository.getRoutines()
    }
    
    suspend fun getRoutineById(routineId: String): Routine? {
        return when (val result = routineRepository.getRoutineById(routineId)) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
    }
    
    // Exercise operations (for muscle group lookup)
    
    private suspend fun getMuscleGroupForExercise(exercise: Exercise): String? {
        return exerciseRepository.getMuscleGroupForExercise(exercise)
    }
    
    // Sync operations
    
    /**
     * Sync all data from API (workouts, routines, exercise templates)
     */
    suspend fun syncAllData() {
        // Sync workouts
        workoutRepository.syncWorkouts()
        
        // Sync routines
        routineRepository.syncRoutines()
        
        // Sync exercise templates (happens automatically when needed)
        exerciseRepository.getExerciseTemplateMapping()
        
        // Invalidate stats cache after sync
        statsRepository.invalidateStatsCache()
    }
    
    /**
     * Clear all cached data
     */
    fun clearCache() {
        cacheManager.invalidate(CacheKeys.EXERCISE_TEMPLATES)
        cacheManager.invalidate(CacheKeys.ROUTINES)
        statsRepository.invalidateStatsCache()
    }
    
    /**
     * Sync with cloud database (structure ready, implementation TBD)
     */
    suspend fun syncWithCloud() {
        // TODO: Implement cloud sync when database service is chosen
    }
}
