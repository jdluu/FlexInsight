package com.example.flexinsight.data.repository

import com.example.flexinsight.data.cache.CacheKeys
import com.example.flexinsight.data.cache.CacheManager
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
import java.time.DayOfWeek
import com.example.flexinsight.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.withContext

/**
 * Repository for statistics calculations.
 * Optimized to avoid N+1 query problems by using batch operations.
 * Refactored to use java.time and StatsCalculator.
 */
class StatsRepositoryImpl(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
    private val exerciseRepository: ExerciseRepository,
    private val cacheManager: CacheManager,
    private val dispatcherProvider: DispatcherProvider
) : StatsRepository {
    /**
     * Calculate workout statistics with caching
     */
    override suspend fun calculateStats(): WorkoutStats {
        // Check cache first
        val cached = cacheManager.get<WorkoutStats>(
            CacheKeys.WORKOUT_STATS,
            CacheTTL.STATS
        )
        if (cached != null) {
            return cached
        }
        
        return withContext(dispatcherProvider.default) {
            val cachedInContext = cacheManager.get<WorkoutStats>(
                CacheKeys.WORKOUT_STATS,
                CacheTTL.STATS
            )
            if (cachedInContext != null) {
                return@withContext cachedInContext
            }
            val workouts = workoutDao.getAllWorkoutsFlow().first()
        
            if (workouts.isEmpty()) {
                val emptyStats = WorkoutStats(
                    totalWorkouts = 0,
                    totalVolume = 0.0,
                    averageVolume = 0.0,
                    totalSets = 0,
                    totalDuration = 0L,
                    averageDuration = 0L,
                    currentStreak = 0,
                    longestStreak = 0,
                    bestWeekVolume = 0.0,
                    bestWeekDate = null
                )
                cacheManager.put(CacheKeys.WORKOUT_STATS, emptyStats)
                return@withContext emptyStats
            }
            
            // Optimize: Get all exercises and sets in batch
            val workoutIds = workouts.map { it.id }
            val allExercises = workoutIds.flatMap { workoutId ->
                exerciseDao.getExercisesByWorkoutId(workoutId)
            }
            val exerciseIds = allExercises.map { it.id }
            val allSets = exerciseIds.flatMap { exerciseId ->
                setDao.getSetsByExerciseId(exerciseId)
            }
            
            // Calculate stats using StatsCalculator
            val totalWorkouts = workouts.size
            val totalVolume = StatsCalculator.calculateTotalVolume(workouts, allExercises, allSets)
            val averageVolume = if (totalWorkouts > 0) totalVolume / totalWorkouts else 0.0
            val totalSets = allSets.size
            
            val totalDuration = StatsCalculator.calculateTotalDuration(workouts)
            val averageDuration = if (totalWorkouts > 0) totalDuration / totalWorkouts else 0L
            
            val currentStreak = StatsCalculator.calculateStreak(workouts)
            val longestStreak = StatsCalculator.calculateLongestStreak(workouts)
            
            // Calculate best week
            val weeklyProgress = getWeeklyProgress(4)
            val bestWeek = weeklyProgress.maxByOrNull { it.totalVolume }
            
            val stats = WorkoutStats(
                totalWorkouts = totalWorkouts,
                totalVolume = totalVolume,
                averageVolume = averageVolume,
                totalSets = totalSets,
                totalDuration = totalDuration,
                averageDuration = averageDuration,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                bestWeekVolume = bestWeek?.totalVolume ?: 0.0,
                bestWeekDate = bestWeek?.weekStartDate
            )
            
            // Cache the result
            cacheManager.put(CacheKeys.WORKOUT_STATS, stats)
            stats
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
    override suspend fun getPRsWithDetails(limit: Int): List<PRDetails> = withContext(dispatcherProvider.default) {
        // Check cache
        val cacheKey = "${CacheKeys.PRS_WITH_DETAILS}_$limit"
        val cached = cacheManager.get<List<PRDetails>>(cacheKey, CacheTTL.PRS)
        if (cached != null) {
            return@withContext cached
        }
        
        val prSets = setDao.getRecentPRsFlow(limit).first()
        if (prSets.isEmpty()) {
            return@withContext emptyList()
        }
        
        // Batch fetch exercises and workouts
        val exerciseIds = prSets.map { it.exerciseId }.distinct()
        val exercises = exerciseIds.mapNotNull { exerciseId ->
            exerciseDao.getExerciseById(exerciseId)
        }
        
        val workoutIds = exercises.map { it.workoutId }.distinct()
        val workouts = workoutIds.associateWith { workoutId ->
            workoutDao.getWorkoutById(workoutId)
        }
        
        val prDetails = prSets.mapNotNull { set ->
            val exercise = exercises.find { it.id == set.exerciseId } ?: return@mapNotNull null
            val workout = workouts[exercise.workoutId] ?: return@mapNotNull null
            
            val weight = set.weight ?: return@mapNotNull null
            
            val muscleGroup = exerciseRepository.getMuscleGroupForExercise(exercise) ?: "Unknown"
            
            PRDetails(
                exerciseName = exercise.name,
                date = workout.startTime,
                muscleGroup = muscleGroup,
                weight = weight,
                workoutId = workout.id,
                setId = set.id
            )
        }
        
        // Cache the result
        cacheManager.put(cacheKey, prDetails)
        prDetails
    }
    
    /**
     * Get muscle group progress for the last N weeks (optimized)
     */
    override suspend fun getMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> = withContext(dispatcherProvider.default) {
        val cacheKey = "${CacheKeys.MUSCLE_GROUP_PROGRESS}$weeks"
        val cached = cacheManager.get<List<MuscleGroupProgress>>(cacheKey, CacheTTL.PROGRESS)
        if (cached != null) {
            return@withContext cached
        }
        
        val now = Instant.now()
        val endDate = now.toEpochMilli()
        val startDate = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()
        
        if (workouts.isEmpty()) {
            return@withContext emptyList()
        }
        
        // Batch fetch all exercises and sets
        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { workoutId ->
            exerciseDao.getExercisesByWorkoutId(workoutId)
        }
        val exerciseIds = allExercises.map { it.id }
        val allSets = exerciseIds.associateBy(
            { it },
            { exerciseId -> setDao.getSetsByExerciseId(exerciseId) }
        )
        
        // Map to track volume and sets per muscle group
        val muscleGroupData = mutableMapOf<String, Pair<Double, Int>>()
        
        allExercises.forEach { exercise ->
            val muscleGroup = exerciseRepository.getMuscleGroupForExercise(exercise) ?: return@forEach
            val sets = allSets[exercise.id] ?: emptyList()
            
            val exerciseVolume = sets.sumOf { set ->
                (set.weight ?: 0.0) * (set.reps ?: 0)
            }
            
            val current = muscleGroupData[muscleGroup] ?: (0.0 to 0)
            muscleGroupData[muscleGroup] = (current.first + exerciseVolume) to (current.second + sets.size)
        }
        
        // Calculate average volume for intensity determination
        val totalVolume = muscleGroupData.values.sumOf { it.first }
        val averageVolume = if (muscleGroupData.isNotEmpty()) totalVolume / muscleGroupData.size else 0.0
        
        // Convert to MuscleGroupProgress and determine intensity
        val progress = muscleGroupData.map { (muscleGroup, data) ->
            val (volume, sets) = data
            val intensity = StatsCalculator.calculateRelativeIntensity(volume, averageVolume)
            MuscleGroupProgress(
                muscleGroup = muscleGroup,
                volume = volume,
                sets = sets,
                intensity = intensity
            )
        }.sortedByDescending { it.volume }.take(3)
        
        // Cache the result
        cacheManager.put(cacheKey, progress)
        progress
    }
    
    /**
     * Calculate volume trend comparing current period to previous period
     */
    override suspend fun calculateVolumeTrend(weeks: Int): VolumeTrend = withContext(dispatcherProvider.default) {
        val cacheKey = "${CacheKeys.VOLUME_TREND}_$weeks"
        val cached = cacheManager.get<VolumeTrend>(cacheKey, CacheTTL.PROGRESS)
        if (cached != null) {
            return@withContext cached
        }
        
        val now = Instant.now()
        val currentPeriodEnd = now.toEpochMilli()
        val currentPeriodStart = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()
        val previousPeriodStart = now.minus(weeks.toLong() * 14, ChronoUnit.DAYS).toEpochMilli()
        
        val currentWorkouts = workoutDao.getWorkoutsByDateRangeFlow(currentPeriodStart, currentPeriodEnd).first()
        val previousWorkouts = workoutDao.getWorkoutsByDateRangeFlow(previousPeriodStart, currentPeriodStart).first()
        
        val currentVolume = calculateTotalVolumeForWorkouts(currentWorkouts)
        val previousVolume = calculateTotalVolumeForWorkouts(previousWorkouts)
        
        val percentageChange = StatsCalculator.calculateVolumeChange(currentVolume, previousVolume)
        
        val trend = VolumeTrend(
            currentVolume = currentVolume,
            previousVolume = previousVolume,
            percentageChange = percentageChange
        )
        
        cacheManager.put(cacheKey, trend)
        trend
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
    override suspend fun getDurationTrend(weeks: Int): List<DailyDurationData> = withContext(dispatcherProvider.default) {
        val cacheKey = "${CacheKeys.DURATION_TREND}$weeks"
        val cached = cacheManager.get<List<DailyDurationData>>(cacheKey, CacheTTL.PROGRESS)
        if (cached != null) {
            return@withContext cached
        }
        
        val now = Instant.now()
        val endDate = now.toEpochMilli()
        val startDate = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()
        
        val result = StatsCalculator.calculateDurationTrend(workouts, startDate, endDate)
        
        cacheManager.put(cacheKey, result)
        result
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
        // Get start of current week (Monday)
        val weekStart = now.with(java.time.DayOfWeek.MONDAY)
        
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val days = mutableListOf<DayInfo>()
        
        // Efficiently fetch all workouts for the week in one query
        val weekStartTimestamp = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weekEndTimestamp = weekStart.plusDays(6).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weekWorkouts = workoutDao.getWorkoutsByDateRangeFlow(weekStartTimestamp, weekEndTimestamp).first()
        
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayEnd = date.atTime(java.time.LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            // Filter in memory instead of querying DB for each day
            val workouts = weekWorkouts.filter { it.startTime in dayStart..dayEnd }
            val hasWorkout = workouts.isNotEmpty()
            val isCompleted = workouts.any { it.endTime != null }
            val workoutCount = workouts.size
            
            days.add(
                DayInfo(
                    name = dayNames[i],
                    date = date.dayOfMonth,
                    timestamp = dayStart,
                    hasWorkout = hasWorkout,
                    isCompleted = isCompleted,
                    workoutCount = workoutCount
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
        
        // Batch fetch all exercises and sets for the day's workouts
        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { workoutId ->
            exerciseDao.getExercisesByWorkoutId(workoutId)
        }
        val exerciseIds = allExercises.map { it.id }
        val allSets = exerciseIds.flatMap { exerciseId ->
            setDao.getSetsByExerciseId(exerciseId)
        }
        
        // Optimize lookup with maps
        val exercisesByWorkout = allExercises.groupBy { it.workoutId }
        val setsByExercise = allSets.groupBy { it.exerciseId }
        
        workouts.map { workout ->
            val exercises = exercisesByWorkout[workout.id] ?: emptyList()
            val duration = workout.endTime?.let { endTime ->
                (endTime - workout.startTime) / (1000 * 60)
            }
            
            // Determine intensity based on volume
            val totalVolume = exercises.sumOf { exercise ->
                val sets = setsByExercise[exercise.id] ?: emptyList()
                sets.sumOf { set -> (set.weight ?: 0.0) * (set.reps ?: 0) }
            }
            val intensity = StatsCalculator.calculateAbsoluteIntensity(totalVolume)
            
            PlannedWorkout(
                id = workout.id,
                name = workout.name ?: "Workout",
                duration = duration,
                intensity = intensity,
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
    override suspend fun getWeeklyProgress(weeks: Int): List<WeeklyProgress> = withContext(dispatcherProvider.default) {
        val now = Instant.now()
        val endDate = now.toEpochMilli()
        val startDate = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()
        
        // Batch fetch all data upfront to avoid N+1 queries
        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { workoutId ->
            exerciseDao.getExercisesByWorkoutId(workoutId)
        }
        val exerciseIds = allExercises.map { it.id }
        val allSets = exerciseIds.flatMap { exerciseId ->
            setDao.getSetsByExerciseId(exerciseId)
        }
        
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
        
        // Group by week and calculate progress using in-memory data
        workouts.groupBy { workout ->
             Instant.ofEpochMilli(workout.startTime)
                .atZone(ZoneId.systemDefault())
                .get(weekFields.weekOfWeekBasedYear())
        }.map { (_, weekWorkouts) ->
            val weekStart = weekWorkouts.minOfOrNull { it.startTime } ?: 0L
            
            // We can pass the full lists of exercises and sets; 
            // StatsCalculator will efficiently look up only what's needed for the passed workouts.
            val totalVolume = StatsCalculator.calculateTotalVolume(weekWorkouts, allExercises, allSets)
            
            WeeklyProgress(
                weekStartDate = weekStart,
                totalVolume = totalVolume,
                workoutCount = weekWorkouts.size,
                averageVolume = if (weekWorkouts.isNotEmpty()) totalVolume / weekWorkouts.size else 0.0
            )
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
    override suspend fun getProfileInfo(hasApiKey: Boolean): ProfileInfo = withContext(dispatcherProvider.default) {
        val workouts = workoutDao.getAllWorkoutsFlow().first()
        val totalWorkouts = workouts.size
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
}
