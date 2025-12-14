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
import java.util.Calendar

/**
 * Repository for statistics calculations.
 * Optimized to avoid N+1 query problems by using batch operations.
 */
class StatsRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
    private val exerciseRepository: ExerciseRepository,
    private val cacheManager: CacheManager
) {
    /**
     * Calculate workout statistics with caching
     */
    suspend fun calculateStats(): WorkoutStats {
        // Check cache first
        val cached = cacheManager.get<WorkoutStats>(
            CacheKeys.WORKOUT_STATS,
            CacheTTL.STATS
        )
        if (cached != null) {
            return cached
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
            return emptyStats
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
        
        // Calculate stats
        val totalWorkouts = workouts.size
        val totalVolume = calculateTotalVolumeOptimized(workouts, allExercises, allSets)
        val averageVolume = if (totalWorkouts > 0) totalVolume / totalWorkouts else 0.0
        val totalSets = allSets.size
        
        // Calculate duration
        val totalDuration = workouts
            .filter { it.endTime != null }
            .sumOf { workout -> 
                val endTime = workout.endTime ?: return@sumOf 0L
                (endTime - workout.startTime) / (1000 * 60)
            }
        val averageDuration = if (totalWorkouts > 0) totalDuration / totalWorkouts else 0L
        
        val currentStreak = calculateStreak(workouts)
        val longestStreak = calculateLongestStreak(workouts)
        
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
        return stats
    }
    
    /**
     * Calculate statistics for a single workout
     */
    suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats {
        val exercises = exerciseDao.getExercisesByWorkoutId(workout.id)
        val exerciseIds = exercises.map { it.id }
        val allSets = exerciseIds.flatMap { exerciseId ->
            setDao.getSetsByExerciseId(exerciseId)
        }
        
        val totalSets = allSets.size
        val totalVolume = allSets.sumOf { set ->
            (set.weight ?: 0.0) * (set.reps ?: 0)
        }
        
        val durationMinutes = if (workout.endTime != null) {
            (workout.endTime - workout.startTime) / (1000 * 60)
        } else {
            0L
        }
        
        return SingleWorkoutStats(
            durationMinutes = durationMinutes,
            totalSets = totalSets,
            totalVolume = totalVolume
        )
    }
    
    /**
     * Get recent PRs
     */
    fun getRecentPRs(limit: Int = 10): Flow<List<com.example.flexinsight.data.model.Set>> {
        return setDao.getRecentPRsFlow(limit)
    }
    
    /**
     * Get PRs with exercise and workout details (optimized)
     */
    suspend fun getPRsWithDetails(limit: Int = 10): List<PRDetails> {
        // Check cache
        val cacheKey = "${CacheKeys.PRS_WITH_DETAILS}_$limit"
        val cached = cacheManager.get<List<PRDetails>>(cacheKey, CacheTTL.PRS)
        if (cached != null) {
            return cached
        }
        
        val prSets = setDao.getRecentPRsFlow(limit).first()
        if (prSets.isEmpty()) {
            return emptyList()
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
            
            if (set.weight == null) return@mapNotNull null
            
            val muscleGroup = exerciseRepository.getMuscleGroupForExercise(exercise) ?: "Unknown"
            
            PRDetails(
                exerciseName = exercise.name,
                date = workout.startTime,
                muscleGroup = muscleGroup,
                weight = set.weight,
                workoutId = workout.id,
                setId = set.id
            )
        }
        
        // Cache the result
        cacheManager.put(cacheKey, prDetails)
        return prDetails
    }
    
    /**
     * Get muscle group progress for the last N weeks (optimized)
     */
    suspend fun getMuscleGroupProgress(weeks: Int = 4): List<MuscleGroupProgress> {
        val cacheKey = "${CacheKeys.MUSCLE_GROUP_PROGRESS}$weeks"
        val cached = cacheManager.get<List<MuscleGroupProgress>>(cacheKey, CacheTTL.PROGRESS)
        if (cached != null) {
            return cached
        }
        
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val startDate = calendar.timeInMillis
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()
        
        if (workouts.isEmpty()) {
            return emptyList()
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
            val intensity = when {
                volume >= averageVolume * 1.5 -> "HI"
                volume >= averageVolume * 0.7 -> "MD"
                else -> "LO"
            }
            MuscleGroupProgress(
                muscleGroup = muscleGroup,
                volume = volume,
                sets = sets,
                intensity = intensity
            )
        }.sortedByDescending { it.volume }.take(3)
        
        // Cache the result
        cacheManager.put(cacheKey, progress)
        return progress
    }
    
    /**
     * Calculate volume trend comparing current period to previous period
     */
    suspend fun calculateVolumeTrend(weeks: Int = 4): VolumeTrend {
        val cacheKey = "${CacheKeys.VOLUME_TREND}_$weeks"
        val cached = cacheManager.get<VolumeTrend>(cacheKey, CacheTTL.PROGRESS)
        if (cached != null) {
            return cached
        }
        
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Current period: last N weeks
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val currentPeriodStart = calendar.timeInMillis
        
        // Previous period: N weeks before that
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val previousPeriodStart = calendar.timeInMillis
        
        val currentWorkouts = workoutDao.getWorkoutsByDateRangeFlow(currentPeriodStart, now).first()
        val previousWorkouts = workoutDao.getWorkoutsByDateRangeFlow(previousPeriodStart, currentPeriodStart).first()
        
        val currentVolume = calculateTotalVolumeForWorkouts(currentWorkouts)
        val previousVolume = calculateTotalVolumeForWorkouts(previousWorkouts)
        
        val percentageChange = if (previousVolume > 0) {
            ((currentVolume - previousVolume) / previousVolume) * 100.0
        } else {
            if (currentVolume > 0) 100.0 else 0.0
        }
        
        val trend = VolumeTrend(
            currentVolume = currentVolume,
            previousVolume = previousVolume,
            percentageChange = percentageChange
        )
        
        cacheManager.put(cacheKey, trend)
        return trend
    }
    
    /**
     * Get weekly volume data for chart display
     */
    suspend fun getWeeklyVolumeData(weeks: Int = 4): List<WeeklyVolumeData> {
        val weeklyProgress = getWeeklyProgress(weeks)
        return weeklyProgress.mapIndexed { index, progress ->
            WeeklyVolumeData(
                weekLabel = "W${index + 1}",
                volume = progress.totalVolume
            )
        }
    }
    
    /**
     * Get duration trend grouped by day of week
     */
    suspend fun getDurationTrend(weeks: Int = 6): List<DailyDurationData> {
        val cacheKey = "${CacheKeys.DURATION_TREND}$weeks"
        val cached = cacheManager.get<List<DailyDurationData>>(cacheKey, CacheTTL.PROGRESS)
        if (cached != null) {
            return cached
        }
        
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val startDate = calendar.timeInMillis
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()
        
        // Group by day of week
        val dayGroups = mutableMapOf<Int, MutableList<Long>>()
        
        workouts.forEach { workout ->
            if (workout.endTime != null) {
                val workoutCal = Calendar.getInstance().apply {
                    timeInMillis = workout.startTime
                }
                val dayOfWeek = workoutCal.get(Calendar.DAY_OF_WEEK)
                val durationMinutes = (workout.endTime - workout.startTime) / (1000 * 60)
                
                val dayIndex = when (dayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> -1
                }
                
                if (dayIndex >= 0) {
                    dayGroups.getOrPut(dayIndex) { mutableListOf() }.add(durationMinutes)
                }
            }
        }
        
        val dayLabels = listOf("M", "T", "W", "T", "F", "S")
        val result = dayLabels.mapIndexed { index, label ->
            val durations = dayGroups[index] ?: emptyList()
            val avgDuration = if (durations.isNotEmpty()) {
                durations.average().toLong()
            } else {
                0L
            }
            DailyDurationData(
                dayOfWeek = label,
                averageDuration = avgDuration
            )
        }
        
        cacheManager.put(cacheKey, result)
        return result
    }
    
    /**
     * Get weekly goal progress
     */
    suspend fun getWeeklyGoalProgress(target: Int = 5): WeeklyGoalProgress {
        val calendar = Calendar.getInstance()
        
        // Get current week (Monday to Sunday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val weekEnd = calendar.timeInMillis
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(weekStart, weekEnd).first()
        val completed = workouts.size
        
        val status = when {
            completed >= target -> "On Track"
            completed >= target * 0.7 -> "On Track"
            completed >= target * 0.5 -> "Behind"
            else -> "Behind"
        }
        
        return WeeklyGoalProgress(
            completed = completed,
            target = target,
            status = status
        )
    }
    
    /**
     * Get week calendar data (Monday to Sunday)
     */
    suspend fun getWeekCalendarData(): List<DayInfo> {
        val calendar = Calendar.getInstance()
        
        // Get current week starting Monday
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (dayOfWeek) {
            Calendar.SUNDAY -> 6
            else -> dayOfWeek - Calendar.MONDAY
        }
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val days = mutableListOf<DayInfo>()
        
        for (i in 0..6) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayStart = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val dayEnd = calendar.timeInMillis
            
            val workouts = workoutDao.getWorkoutsByDateRangeFlow(dayStart, dayEnd).first()
            val hasWorkout = workouts.isNotEmpty()
            val isCompleted = workouts.any { it.endTime != null }
            val workoutCount = workouts.size
            
            days.add(
                DayInfo(
                    name = dayNames[i],
                    date = calendar.get(Calendar.DAY_OF_MONTH),
                    timestamp = dayStart,
                    hasWorkout = hasWorkout,
                    isCompleted = isCompleted,
                    workoutCount = workoutCount
                )
            )
            
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return days
    }
    
    /**
     * Get planned workouts for a specific day
     */
    suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dayStart = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val dayEnd = calendar.timeInMillis
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(dayStart, dayEnd).first()
        
        return workouts.map { workout ->
            val exercises = exerciseDao.getExercisesByWorkoutId(workout.id)
            val duration = if (workout.endTime != null) {
                (workout.endTime - workout.startTime) / (1000 * 60)
            } else {
                null
            }
            
            // Determine intensity based on volume
            val allSets = exercises.flatMap { exercise ->
                setDao.getSetsByExerciseId(exercise.id)
            }
            val totalVolume = allSets.sumOf { set ->
                (set.weight ?: 0.0) * (set.reps ?: 0)
            }
            val intensity = when {
                totalVolume > 5000 -> "High Intensity"
                totalVolume > 2000 -> "Medium Intensity"
                else -> "Aerobic"
            }
            
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
    suspend fun getVolumeBalance(weeks: Int = 4): VolumeBalance {
        val muscleGroupProgress = getMuscleGroupProgress(weeks)
        
        // Map muscle groups to categories
        val pushGroups = listOf("Chest", "Shoulders", "Triceps")
        val pullGroups = listOf("Back", "Biceps")
        val legsGroups = listOf("Legs", "Quads", "Hamstrings", "Glutes", "Calves")
        val cardioGroups = listOf("Cardio")
        
        var pushVolume = 0.0
        var pullVolume = 0.0
        var legsVolume = 0.0
        var cardioVolume = 0.0
        
        muscleGroupProgress.forEach { progress ->
            val group = progress.muscleGroup
            when {
                pushGroups.any { group.contains(it, ignoreCase = true) } -> pushVolume += progress.volume
                pullGroups.any { group.contains(it, ignoreCase = true) } -> pullVolume += progress.volume
                legsGroups.any { group.contains(it, ignoreCase = true) } -> legsVolume += progress.volume
                cardioGroups.any { group.contains(it, ignoreCase = true) } -> cardioVolume += progress.volume
            }
        }
        
        val totalVolume = pushVolume + pullVolume + legsVolume + cardioVolume
        
        return if (totalVolume > 0) {
            VolumeBalance(
                push = (pushVolume / totalVolume).toFloat(),
                pull = (pullVolume / totalVolume).toFloat(),
                legs = (legsVolume / totalVolume).toFloat(),
                cardio = (cardioVolume / totalVolume).toFloat()
            )
        } else {
            VolumeBalance(
                push = 0.25f,
                pull = 0.25f,
                legs = 0.25f,
                cardio = 0.25f
            )
        }
    }
    
    /**
     * Get weekly progress
     */
    suspend fun getWeeklyProgress(weeks: Int = 4): List<WeeklyProgress> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val startDate = calendar.timeInMillis
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()
        
        // Group by week and calculate progress
        return workouts.groupBy { workout ->
            val workoutCal = Calendar.getInstance().apply {
                timeInMillis = workout.startTime
            }
            workoutCal.get(Calendar.WEEK_OF_YEAR)
        }.map { (_, weekWorkouts) ->
            val weekStart = weekWorkouts.minOfOrNull { it.startTime } ?: 0L
            val totalVolume = calculateTotalVolumeForWorkouts(weekWorkouts)
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
    suspend fun getMemberSinceDate(): Long? {
        val workouts = workoutDao.getAllWorkoutsFlow().first()
        return workouts.minOfOrNull { it.startTime }
    }
    
    /**
     * Calculate account age in days
     */
    suspend fun calculateAccountAgeDays(): Int {
        val memberSince = getMemberSinceDate() ?: return 0
        val now = System.currentTimeMillis()
        val daysDiff = (now - memberSince) / (1000 * 60 * 60 * 24)
        return daysDiff.toInt().coerceAtLeast(0)
    }
    
    /**
     * Get profile information
     */
    suspend fun getProfileInfo(hasApiKey: Boolean): ProfileInfo {
        val workouts = workoutDao.getAllWorkoutsFlow().first()
        val totalWorkouts = workouts.size
        val memberSince = getMemberSinceDate()
        val accountAgeDays = calculateAccountAgeDays()
        
        return ProfileInfo(
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
    fun invalidateStatsCache() {
        cacheManager.invalidatePrefix(CacheKeys.WORKOUT_STATS)
        cacheManager.invalidatePrefix(CacheKeys.PRS_WITH_DETAILS)
        cacheManager.invalidatePrefix(CacheKeys.MUSCLE_GROUP_PROGRESS)
        cacheManager.invalidatePrefix(CacheKeys.WEEKLY_PROGRESS)
        cacheManager.invalidatePrefix(CacheKeys.VOLUME_TREND)
        cacheManager.invalidatePrefix(CacheKeys.DURATION_TREND)
    }
    
    // Helper functions
    
    private suspend fun calculateTotalVolumeOptimized(
        workouts: List<Workout>,
        allExercises: List<Exercise>,
        allSets: List<com.example.flexinsight.data.model.Set>
    ): Double {
        // Create maps for efficient lookup
        val exercisesByWorkout = allExercises.groupBy { it.workoutId }
        val setsByExercise = allSets.groupBy { it.exerciseId }
        
        return workouts.sumOf { workout ->
            val exercises = exercisesByWorkout[workout.id] ?: emptyList()
            exercises.sumOf { exercise ->
                val sets = setsByExercise[exercise.id] ?: emptyList()
                sets.sumOf { set: com.example.flexinsight.data.model.Set ->
                    (set.weight ?: 0.0) * (set.reps ?: 0)
                }
            }
        }
    }
    
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
        
        return allSets.sumOf { set ->
            (set.weight ?: 0.0) * (set.reps ?: 0)
        }
    }
    
    private fun calculateStreak(workouts: List<Workout>): Int {
        if (workouts.isEmpty()) return 0
        
        val sortedWorkouts = workouts.sortedByDescending { it.startTime }
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        var streak = 0
        var currentDate = calendar.timeInMillis
        
        for (workout in sortedWorkouts) {
            val workoutCal = Calendar.getInstance().apply {
                timeInMillis = workout.startTime
            }
            workoutCal.set(Calendar.HOUR_OF_DAY, 0)
            workoutCal.set(Calendar.MINUTE, 0)
            workoutCal.set(Calendar.SECOND, 0)
            workoutCal.set(Calendar.MILLISECOND, 0)
            val workoutDateStart = workoutCal.timeInMillis
            
            if (workoutDateStart == currentDate) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                currentDate = calendar.timeInMillis
            } else if (workoutDateStart < currentDate) {
                break
            }
        }
        
        return streak
    }
    
    private fun calculateLongestStreak(workouts: List<Workout>): Int {
        if (workouts.isEmpty()) return 0
        
        val sortedWorkouts = workouts.sortedBy { it.startTime }
        var longestStreak = 0
        var currentStreak = 0
        var lastDate: Long? = null
        
        for (workout in sortedWorkouts) {
            val workoutCal = Calendar.getInstance().apply {
                timeInMillis = workout.startTime
            }
            workoutCal.set(Calendar.HOUR_OF_DAY, 0)
            workoutCal.set(Calendar.MINUTE, 0)
            workoutCal.set(Calendar.SECOND, 0)
            workoutCal.set(Calendar.MILLISECOND, 0)
            val workoutDate = workoutCal.timeInMillis
            
            if (lastDate == null) {
                currentStreak = 1
                lastDate = workoutDate
            } else {
                val daysDiff = (workoutDate - lastDate) / (1000 * 60 * 60 * 24)
                if (daysDiff == 1L) {
                    currentStreak++
                } else {
                    longestStreak = maxOf(longestStreak, currentStreak)
                    currentStreak = 1
                }
                lastDate = workoutDate
            }
        }
        
        return maxOf(longestStreak, currentStreak)
    }
}
