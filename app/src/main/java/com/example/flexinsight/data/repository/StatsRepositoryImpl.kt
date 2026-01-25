package com.example.flexinsight.data.repository

import com.example.flexinsight.data.cache.CacheKeys
import com.example.flexinsight.data.cache.CacheManager
import com.example.flexinsight.data.cache.CacheStrategy
import com.example.flexinsight.data.cache.CacheTTL
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.local.dao.WorkoutDao
import com.example.flexinsight.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import java.time.LocalTime
import com.example.flexinsight.domain.util.StatsCalculator
import com.example.flexinsight.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository for statistics calculations.
 * Optimized to avoid N+1 query problems by using batch operations.
 * Refactored to use java.time, StatsCalculator, and CacheStrategy.
 */
class StatsRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
    private val exerciseRepository: ExerciseRepository,
    private val cacheManager: CacheManager,
    private val dispatcherProvider: DispatcherProvider,
    private val cacheStrategy: CacheStrategy,
    // Use Cases (Lazy to avoid heavy initialization if not needed, though mostly okay as singletons)
    private val getWorkoutStatsUseCase: com.example.flexinsight.domain.usecase.GetWorkoutStatsUseCase,
    private val getPRDetailsUseCase: com.example.flexinsight.domain.usecase.GetPRDetailsUseCase,
    private val getMuscleGroupProgressUseCase: com.example.flexinsight.domain.usecase.GetMuscleGroupProgressUseCase,
    private val getWeeklyProgressUseCase: com.example.flexinsight.domain.usecase.GetWeeklyProgressUseCase,
    private val getMuscleRecoveryUseCase: com.example.flexinsight.domain.usecase.GetMuscleRecoveryUseCase
) : StatsRepository {
    /**
     * Calculate workout statistics with caching
     */
    override suspend fun calculateStats(): WorkoutStats {
        return cacheStrategy.getOrFetch(CacheKeys.WORKOUT_STATS, CacheTTL.STATS) {
            getWorkoutStatsUseCase()
        }
    }

    /**
     * Calculate statistics for a single workout
     */
    override suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats = withContext(dispatcherProvider.default) {
        val exercises = exerciseDao.getExercisesByWorkoutId(workout.id)
        val exerciseIds = exercises.map { it.id }
        val allSets = exerciseIds.flatMap { exerciseId ->
            setDao.getSetsByExerciseId(exerciseId)
        }

        val totalSets = allSets.size
        val totalVolume = allSets.sumOf { set ->
            (set.weight ?: 0.0) * (set.reps ?: 0)
        }

        val durationMinutes = workout.endTime?.let { endTime ->
            (endTime - workout.startTime) / (1000 * 60)
        } ?: 0L

        SingleWorkoutStats(
            durationMinutes = durationMinutes,
            totalSets = totalSets,
            totalVolume = totalVolume
        )
    }

    /**
     * Get recent PRs
     */
    override fun getRecentPRs(limit: Int): Flow<List<com.example.flexinsight.data.model.Set>> {
        return setDao.getRecentPRsFlow(limit)
    }

    /**
     * Get PRs with exercise and workout details (optimized)
     */
    override suspend fun getPRsWithDetails(limit: Int): List<PRDetails> {
        return cacheStrategy.getOrFetch("${CacheKeys.PRS_WITH_DETAILS}_$limit", CacheTTL.PRS) {
            getPRDetailsUseCase(limit)
        }
    }

    /**
     * Get all PRs with details (limit 100)
     */
    override suspend fun getAllPRsWithDetails(): List<PRDetails> {
        return getPRsWithDetails(limit = 100)
    }

    /**
     * Get muscle group progress for the last N weeks (optimized)
     */
    override suspend fun getMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> {
        return cacheStrategy.getOrFetch("${CacheKeys.MUSCLE_GROUP_PROGRESS}$weeks", CacheTTL.PROGRESS) {
            getMuscleGroupProgressUseCase(weeks)
        }
    }

    /**
     * Calculate volume trend comparing current period to previous period
     */
    override suspend fun calculateVolumeTrend(weeks: Int): VolumeTrend {
        return cacheStrategy.getOrFetch("${CacheKeys.VOLUME_TREND}_$weeks", CacheTTL.PROGRESS) {
            val now = Instant.now()
            val currentPeriodEnd = now.toEpochMilli()
            val currentPeriodStart = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()
            val previousPeriodStart = now.minus(weeks.toLong() * 14, ChronoUnit.DAYS).toEpochMilli()

            val currentWorkouts = workoutDao.getWorkoutsByDateRangeFlow(currentPeriodStart, currentPeriodEnd).first()
            val previousWorkouts = workoutDao.getWorkoutsByDateRangeFlow(previousPeriodStart, currentPeriodStart).first()

            val currentVolume = calculateTotalVolumeForWorkouts(currentWorkouts)
            val previousVolume = calculateTotalVolumeForWorkouts(previousWorkouts)

            val percentageChange = StatsCalculator.calculateVolumeChange(currentVolume, previousVolume)

            VolumeTrend(
                currentVolume = currentVolume,
                previousVolume = previousVolume,
                percentageChange = percentageChange
            )
        }
    }

    /**
     * Get weekly volume data for chart display
     */
    override suspend fun getWeeklyVolumeData(weeks: Int): List<WeeklyVolumeData> = withContext(dispatcherProvider.default) {
        val weeklyProgress = getWeeklyProgress(weeks)
        weeklyProgress.mapIndexed { index, progress ->
            WeeklyVolumeData(
                weekLabel = "W${index + 1}",
                volume = progress.totalVolume
            )
        }
    }

    /**
     * Get duration trend grouped by day of week
     */
    override suspend fun getDurationTrend(weeks: Int): List<DailyDurationData> {
        return cacheStrategy.getOrFetch("${CacheKeys.DURATION_TREND}$weeks", CacheTTL.PROGRESS) {
            val now = Instant.now()
            val endDate = now.toEpochMilli()
            val startDate = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()

            val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()

            StatsCalculator.calculateDurationTrend(workouts, startDate, endDate)
        }
    }

    /**
     * Get weekly goal progress
     */
    override suspend fun getWeeklyGoalProgress(target: Int): WeeklyGoalProgress = withContext(dispatcherProvider.default) {
        val now = LocalDate.now()
        val weekStart = now.with(java.time.DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weekEnd = now.with(java.time.DayOfWeek.SUNDAY).atTime(java.time.LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val workouts = workoutDao.getWorkoutsByDateRangeFlow(weekStart, weekEnd).first()
        val completed = workouts.size

        val status = StatsCalculator.calculateGoalStatus(completed, target)

        WeeklyGoalProgress(
            completed = completed,
            target = target,
            status = status
        )
    }

    /**
     * Get week calendar data (Monday to Sunday)
     */
    override suspend fun getWeekCalendarData(): List<DayInfo> = withContext(dispatcherProvider.default) {
        val now = LocalDate.now()
        val weekStart = now.with(java.time.DayOfWeek.MONDAY)
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val days = mutableListOf<DayInfo>()

        val weekStartTimestamp = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weekEndTimestamp = weekStart.plusDays(6).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weekWorkouts = workoutDao.getWorkoutsByDateRangeFlow(weekStartTimestamp, weekEndTimestamp).first()

        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayEnd = date.atTime(java.time.LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val workouts = weekWorkouts.filter { it.startTime in dayStart..dayEnd }
            days.add(
                DayInfo(
                    name = dayNames[i],
                    date = date.dayOfMonth,
                    timestamp = dayStart,
                    hasWorkout = workouts.isNotEmpty(),
                    isCompleted = workouts.any { it.endTime != null },
                    workoutCount = workouts.size
                )
            )
        }
        days
    }

    /**
     * Get planned workouts for a specific day
     */
    override suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> = withContext(dispatcherProvider.default) {
        val dayStart = StatsCalculator.getStartOfDay(timestamp)
        val dayEnd = StatsCalculator.getEndOfDay(timestamp)

        val workouts = workoutDao.getWorkoutsByDateRangeFlow(dayStart, dayEnd).first()
        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { workoutId ->
            exerciseDao.getExercisesByWorkoutId(workoutId)
        }
        val exerciseIds = allExercises.map { it.id }
        val allSets = exerciseIds.flatMap { exerciseId ->
            setDao.getSetsByExerciseId(exerciseId)
        }

        val exercisesByWorkout = allExercises.groupBy { it.workoutId }
        val setsByExercise = allSets.groupBy { it.exerciseId }

        workouts.map { workout ->
            val exercises = exercisesByWorkout[workout.id] ?: emptyList()
            val totalVolume = exercises.sumOf { exercise ->
                val sets = setsByExercise[exercise.id] ?: emptyList()
                sets.sumOf { set -> (set.weight ?: 0.0) * (set.reps ?: 0) }
            }
            PlannedWorkout(
                id = workout.id,
                name = workout.name ?: "Workout",
                duration = workout.endTime?.let { (it - workout.startTime) / (1000 * 60) },
                intensity = StatsCalculator.calculateAbsoluteIntensity(totalVolume),
                isCompleted = workout.endTime != null,
                routineId = workout.routineId,
                exerciseCount = exercises.size
            )
        }
    }

    /**
     * Get volume balance across Push, Pull, Legs, Cardio
     */
    override suspend fun getVolumeBalance(weeks: Int): VolumeBalance = withContext(dispatcherProvider.default) {
        val muscleGroupProgress = getMuscleGroupProgress(weeks)
        StatsCalculator.calculateVolumeBalance(muscleGroupProgress)
    }

    /**
     * Get weekly progress
     */
    override suspend fun getWeeklyProgress(weeks: Int): List<WeeklyProgress> {
        return cacheStrategy.getOrFetch("${CacheKeys.WEEKLY_PROGRESS}$weeks", CacheTTL.PROGRESS) {
            getWeeklyProgressUseCase(weeks)
        }
    }

    /**
     * Get member since date (timestamp of first workout)
     */
    override suspend fun getMemberSinceDate(): Long? {
        val workouts = workoutDao.getAllWorkoutsFlow().first()
        return workouts.minOfOrNull { it.startTime }
    }

    /**
     * Calculate account age in days
     */
    override suspend fun calculateAccountAgeDays(): Int {
        val memberSince = getMemberSinceDate() ?: return 0
        val now = System.currentTimeMillis()
        val daysDiff = (now - memberSince) / (1000 * 60 * 60 * 24)
        return daysDiff.toInt().coerceAtLeast(0)
    }

    /**
     * Get profile information
     */
    override suspend fun getProfileInfo(hasApiKey: Boolean, remoteWorkoutCount: Int?): ProfileInfo = withContext(dispatcherProvider.default) {
        val workouts = workoutDao.getAllWorkoutsFlow().first()
        val localCount = workouts.size
        
        val totalWorkouts = if (remoteWorkoutCount != null) {
            java.lang.Math.max(remoteWorkoutCount, localCount)
        } else {
            localCount
        }
        val memberSince = getMemberSinceDate()
        val accountAgeDays = calculateAccountAgeDays()

        ProfileInfo(
            displayName = null,
            memberSince = memberSince,
            isProMember = hasApiKey,
            totalWorkouts = totalWorkouts,
            accountAgeDays = accountAgeDays
        )
    }

    /**
     * Invalidate stats cache
     */
    override fun invalidateStatsCache() {
        cacheManager.invalidatePrefix(CacheKeys.WORKOUT_STATS)
        cacheManager.invalidatePrefix(CacheKeys.PRS_WITH_DETAILS)
        cacheManager.invalidatePrefix(CacheKeys.MUSCLE_GROUP_PROGRESS)
        cacheManager.invalidatePrefix(CacheKeys.WEEKLY_PROGRESS)
        cacheManager.invalidatePrefix(CacheKeys.VOLUME_TREND)
        cacheManager.invalidatePrefix(CacheKeys.DURATION_TREND)
    }

    // Helper functions

    private suspend fun calculateTotalVolumeForWorkouts(workouts: List<Workout>): Double {
        if (workouts.isEmpty()) return 0.0

        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { workoutId ->
            exerciseDao.getExercisesByWorkoutId(workoutId)
        }
        val exerciseIds = allExercises.map { it.id }
        val allSets = exerciseIds.flatMap { exerciseId ->
            setDao.getSetsByExerciseId(exerciseId)
        }

        return StatsCalculator.calculateTotalVolume(workouts, allExercises, allSets)
    }

    /**
     * Get persistence data for the last N days (Consistency Heatmap)
     */
    override suspend fun getConsistencyData(days: Int): List<DayInfo> = withContext(dispatcherProvider.default) {
        val now = LocalDate.now()
        val startDate = now.minusDays((days - 1).toLong())
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val resultDays = mutableListOf<DayInfo>()

        val allWorkouts = workoutDao.getAllWorkoutsFlow().first()

        for (i in 0 until days) {
            val date = startDate.plusDays(i.toLong())
            val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayEnd = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val workouts = allWorkouts.filter { it.startTime in dayStart..dayEnd }
            val dayOfWeek = date.dayOfWeek.value
            resultDays.add(
                DayInfo(
                    name = dayNames[dayOfWeek - 1],
                    date = date.dayOfMonth,
                    timestamp = dayStart,
                    hasWorkout = workouts.isNotEmpty(),
                    isCompleted = workouts.any { it.endTime != null },
                    workoutCount = workouts.size
                )
            )
        }
        resultDays
    }
    
    override suspend fun getMuscleRecoveryStatus(): Map<MuscleGroup, Float> {
        return getMuscleRecoveryUseCase()
    }
}
