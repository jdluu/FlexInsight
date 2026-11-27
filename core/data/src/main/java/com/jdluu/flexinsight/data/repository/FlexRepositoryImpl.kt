package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.dispatchers.DefaultDispatcherProvider
import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.core.network.NetworkMonitor
import com.jdluu.flexinsight.data.cache.CacheKeys
import com.jdluu.flexinsight.data.cache.CacheManager
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.data.sync.HevySyncSource
import com.jdluu.flexinsight.domain.model.DailyDurationData
import com.jdluu.flexinsight.domain.model.DayInfo
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.ExerciseHistory
import com.jdluu.flexinsight.domain.model.MuscleGroup
import com.jdluu.flexinsight.domain.model.MuscleGroupProgress
import com.jdluu.flexinsight.domain.model.PRDetails
import com.jdluu.flexinsight.domain.model.PeriodComparison
import com.jdluu.flexinsight.domain.model.PlannedWorkout
import com.jdluu.flexinsight.domain.model.ProfileInfo
import com.jdluu.flexinsight.domain.model.Routine
import com.jdluu.flexinsight.domain.model.RoutineFolder
import com.jdluu.flexinsight.domain.model.Set
import com.jdluu.flexinsight.domain.model.SingleWorkoutStats
import com.jdluu.flexinsight.domain.model.VolumeBalance
import com.jdluu.flexinsight.domain.model.VolumeTrend
import com.jdluu.flexinsight.domain.model.WeeklyGoalProgress
import com.jdluu.flexinsight.domain.model.WeeklyProgress
import com.jdluu.flexinsight.domain.model.WeeklyVolumeData
import com.jdluu.flexinsight.domain.model.Workout
import com.jdluu.flexinsight.domain.model.WorkoutStats
import com.jdluu.flexinsight.domain.repository.ExerciseRepository
import com.jdluu.flexinsight.domain.repository.FlexRepository
import com.jdluu.flexinsight.domain.repository.RoutineRepository
import com.jdluu.flexinsight.domain.repository.StatsRepository
import com.jdluu.flexinsight.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow

/**
 * Main repository that delegates to specialized repositories.
 */
class FlexRepositoryImpl(
    private val apiKeyManager: ApiKeyManager,
    @Suppress("UNUSED_PARAMETER") networkMonitor: NetworkMonitor,
    private val cacheManager: CacheManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val statsRepository: StatsRepository
) : FlexRepository, HevySyncSource {

    override fun invalidateApiService() {
        workoutRepository.invalidateApiService()
        exerciseRepository.invalidateApiService()
        routineRepository.invalidateApiService()
    }

    override fun getWorkouts(): Flow<List<Workout>> = workoutRepository.getWorkouts()

    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> = workoutRepository.getRecentWorkouts(limit)

    override fun getAllExercises(): Flow<List<Exercise>> = exerciseRepository.getAllExercises()

    override suspend fun getExerciseHistory(templateId: String): Result<ExerciseHistory> =
        exerciseRepository.getExerciseHistory(templateId)

    override suspend fun getWorkoutById(workoutId: String): Workout? =
        when (val result = workoutRepository.getWorkoutById(workoutId)) {
            is Result.Success -> result.data
            is Result.Error -> null
        }

    override fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> =
        workoutRepository.getWorkoutByIdFlow(workoutId)

    override fun getWorkoutCount(): Flow<Int> = workoutRepository.getWorkoutCount()

    override suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise> =
        workoutRepository.getExercisesByWorkoutId(workoutId)

    override suspend fun getSetsByExerciseId(exerciseId: String): List<Set> =
        workoutRepository.getSetsByExerciseId(exerciseId)

    override suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean): Result<Unit> =
        workoutRepository.updateWorkoutStatus(workoutId, isCompleted, if (isCompleted) System.currentTimeMillis() else null)

    override suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit> =
        workoutRepository.rescheduleWorkout(workoutId, newStartTime)

    override suspend fun calculateStats(): WorkoutStats = statsRepository.calculateStats()

    override suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats =
        statsRepository.calculateWorkoutStats(workout)

    override fun getRecentPRs(limit: Int): Flow<List<Set>> = statsRepository.getRecentPRs(limit)

    override suspend fun getPRsWithDetails(limit: Int): List<PRDetails> = statsRepository.getPRsWithDetails(limit)

    override suspend fun getAllPRsWithDetails(): List<PRDetails> = statsRepository.getAllPRsWithDetails()

    override suspend fun getMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> =
        statsRepository.getMuscleGroupProgress(weeks)

    override suspend fun calculateVolumeTrend(weeks: Int): VolumeTrend =
        statsRepository.calculateVolumeTrend(weeks)

    override suspend fun getPeriodComparison(): PeriodComparison? = statsRepository.getPeriodComparison()

    override suspend fun getWeeklyVolumeData(weeks: Int): List<WeeklyVolumeData> =
        statsRepository.getWeeklyVolumeData(weeks)

    override suspend fun getDurationTrend(weeks: Int): List<DailyDurationData> =
        statsRepository.getDurationTrend(weeks)

    override suspend fun getWeeklyGoalProgress(target: Int): WeeklyGoalProgress =
        statsRepository.getWeeklyGoalProgress(target)

    override suspend fun getWeekCalendarData(): List<DayInfo> = statsRepository.getWeekCalendarData()

    override suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> =
        statsRepository.getPlannedWorkoutsForDay(timestamp)

    override suspend fun getVolumeBalance(weeks: Int): VolumeBalance = statsRepository.getVolumeBalance(weeks)

    override suspend fun getWeeklyProgress(weeks: Int): List<WeeklyProgress> =
        statsRepository.getWeeklyProgress(weeks)

    override suspend fun getMemberSinceDate(): Long? = statsRepository.getMemberSinceDate()

    override suspend fun calculateAccountAgeDays(): Int = statsRepository.calculateAccountAgeDays()

    override suspend fun getProfileInfo(): ProfileInfo {
        val hasApiKey = apiKeyManager.hasApiKey()

        val remoteCount = if (hasApiKey) {
            val result = workoutRepository.getRemoteWorkoutCount()
            if (result is Result.Success) result.data else null
        } else null

        return statsRepository.getProfileInfo(hasApiKey, remoteCount)
    }

    override suspend fun getConsistencyData(days: Int): List<DayInfo> = statsRepository.getConsistencyData(days)

    override suspend fun getMuscleRecoveryStatus(): Map<MuscleGroup, Float> =
        statsRepository.getMuscleRecoveryStatus()

    override fun getRoutines(): Flow<List<Routine>> = routineRepository.getRoutines()

    override suspend fun getRoutineById(routineId: String): Routine? =
        when (val result = routineRepository.getRoutineById(routineId)) {
            is Result.Success -> result.data
            is Result.Error -> null
        }

    override suspend fun getRoutineFolders(): List<RoutineFolder> =
        when (val result = routineRepository.getRoutineFolders()) {
            is Result.Success -> result.data
            is Result.Error -> emptyList()
        }

    override suspend fun syncAll(): Result<Unit> = syncAllData()

    override suspend fun syncAllData(): Result<Unit> {
        val errors = mutableListOf<ApiError>()

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
        internal fun mergeSyncErrors(errors: List<ApiError>): Result<Unit>? {
            if (errors.isEmpty()) return null
            val message = errors.joinToString("; ") { it.message }
            return Result.Error(
                ApiError.Unknown(
                    if (errors.size == 1) errors.first().message else "Sync failed: $message"
                )
            )
        }
    }

    override fun clearCache() {
        cacheManager.invalidate(CacheKeys.EXERCISE_TEMPLATES)
        cacheManager.invalidate(CacheKeys.EXERCISE_TEMPLATE_NAMES)
        cacheManager.invalidate(CacheKeys.ROUTINES)
        statsRepository.invalidateStatsCache()
    }

    override suspend fun syncWithCloud() {
        // No-op until a cloud backend is added.
    }
}
