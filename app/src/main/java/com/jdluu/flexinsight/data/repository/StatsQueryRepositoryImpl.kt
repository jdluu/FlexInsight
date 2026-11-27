package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import com.jdluu.flexinsight.data.cache.CacheKeys
import com.jdluu.flexinsight.data.cache.CacheStrategy
import com.jdluu.flexinsight.data.cache.CacheTTL
import com.jdluu.flexinsight.data.local.dao.ExerciseDao
import com.jdluu.flexinsight.data.local.dao.SetDao
import com.jdluu.flexinsight.data.local.dao.WorkoutDao
import com.jdluu.flexinsight.data.mapper.EntityMapper
import com.jdluu.flexinsight.domain.calc.DurationCalculator
import com.jdluu.flexinsight.domain.calc.RecoveryScoreCalculator
import com.jdluu.flexinsight.domain.calc.StreakCalculator
import com.jdluu.flexinsight.domain.calc.TrainingLoadCalculator
import com.jdluu.flexinsight.domain.calc.VolumeCalculator
import com.jdluu.flexinsight.domain.model.DailyDurationData
import com.jdluu.flexinsight.domain.model.DayInfo
import com.jdluu.flexinsight.domain.model.MuscleGroup
import com.jdluu.flexinsight.domain.model.MuscleGroupProgress
import com.jdluu.flexinsight.domain.model.PRDetails
import com.jdluu.flexinsight.domain.model.PeriodComparison
import com.jdluu.flexinsight.domain.model.PlannedWorkout
import com.jdluu.flexinsight.domain.model.ProfileInfo
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
import com.jdluu.flexinsight.domain.repository.StatsQueryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Read/query implementation for statistics.
 */
class StatsQueryRepositoryImpl(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
    private val exerciseRepository: ExerciseRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val cacheStrategy: CacheStrategy
) : StatsQueryRepository {

    override suspend fun calculateStats(): WorkoutStats {
        return cacheStrategy.getOrFetch(CacheKeys.WORKOUT_STATS, CacheTTL.STATS) {
            computeWorkoutStats()
        }
    }

    override suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats = withContext(dispatcherProvider.default) {
        val exercises = exerciseDao.getExercisesByWorkoutId(workout.id).map(EntityMapper::toDomain)
        val exerciseIds = exercises.map { it.id }
        val allSets = exerciseIds.flatMap { exerciseId ->
            setDao.getSetsByExerciseId(exerciseId).map(EntityMapper::toDomain)
        }

        val totalSets = allSets.size
        val totalVolume = VolumeCalculator.totalSetVolume(allSets)
        val durationMinutes = DurationCalculator.workoutDurationMinutes(workout)

        SingleWorkoutStats(
            durationMinutes = durationMinutes,
            totalSets = totalSets,
            totalVolume = totalVolume
        )
    }

    override fun getRecentPRs(limit: Int): Flow<List<Set>> {
        return setDao.getRecentPRsFlow(limit).map { list -> list.map(EntityMapper::toDomain) }
    }

    override suspend fun getPRsWithDetails(limit: Int): List<PRDetails> {
        return cacheStrategy.getOrFetch("${CacheKeys.PRS_WITH_DETAILS}_$limit", CacheTTL.PRS) {
            computePRDetails(limit)
        }
    }

    override suspend fun getAllPRsWithDetails(): List<PRDetails> = getPRsWithDetails(limit = 100)

    override suspend fun getMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> {
        return cacheStrategy.getOrFetch("${CacheKeys.MUSCLE_GROUP_PROGRESS}$weeks", CacheTTL.PROGRESS) {
            computeMuscleGroupProgress(weeks)
        }
    }

    override suspend fun getPeriodComparison(): PeriodComparison? = withContext(dispatcherProvider.default) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val currentStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val currentEnd = today.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val previousStart = today.minusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val currentWorkouts = workoutDao.getWorkoutsByDateRangeFlow(currentStart, currentEnd).first()
            .map(EntityMapper::toDomain)
            .filter { !it.isDeleted }
        val previousWorkouts = workoutDao.getWorkoutsByDateRangeFlow(previousStart, currentStart).first()
            .map(EntityMapper::toDomain)
            .filter { !it.isDeleted }

        if (currentWorkouts.isEmpty() && previousWorkouts.isEmpty()) return@withContext null

        val currentVolume = calculateTotalVolumeForWorkouts(currentWorkouts)
        val previousVolume = calculateTotalVolumeForWorkouts(previousWorkouts)
        val currentMonthName = today.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val prevMonthName = today.minusMonths(1).month.name.lowercase().replaceFirstChar { it.uppercase() }

        PeriodComparison(
            currentPeriodLabel = currentMonthName,
            previousPeriodLabel = prevMonthName,
            totalVolumeCurrent = currentVolume,
            totalVolumePrevious = previousVolume,
            totalWorkoutsCurrent = currentWorkouts.size,
            totalWorkoutsPrevious = previousWorkouts.size,
            avgDurationCurrent = DurationCalculator.averageDurationMinutes(currentWorkouts),
            avgDurationPrevious = DurationCalculator.averageDurationMinutes(previousWorkouts)
        )
    }

    override suspend fun calculateVolumeTrend(weeks: Int): VolumeTrend {
        return cacheStrategy.getOrFetch("${CacheKeys.VOLUME_TREND}_$weeks", CacheTTL.PROGRESS) {
            val now = Instant.now()
            val currentPeriodEnd = now.toEpochMilli()
            val currentPeriodStart = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()
            val previousPeriodStart = now.minus(weeks.toLong() * 14, ChronoUnit.DAYS).toEpochMilli()

            val currentWorkouts = workoutDao.getWorkoutsByDateRangeFlow(currentPeriodStart, currentPeriodEnd).first()
                .map(EntityMapper::toDomain)
            val previousWorkouts = workoutDao.getWorkoutsByDateRangeFlow(previousPeriodStart, currentPeriodStart).first()
                .map(EntityMapper::toDomain)

            val currentVolume = calculateTotalVolumeForWorkouts(currentWorkouts)
            val previousVolume = calculateTotalVolumeForWorkouts(previousWorkouts)
            val percentageChange = VolumeCalculator.changePercent(currentVolume, previousVolume)

            VolumeTrend(
                currentVolume = currentVolume,
                previousVolume = previousVolume,
                percentageChange = percentageChange
            )
        }
    }

    override suspend fun getWeeklyVolumeData(weeks: Int): List<WeeklyVolumeData> = withContext(dispatcherProvider.default) {
        val weeklyProgress = getWeeklyProgress(weeks)
        weeklyProgress.mapIndexed { index, progress ->
            WeeklyVolumeData(
                weekLabel = "W${index + 1}",
                volume = progress.totalVolume
            )
        }
    }

    override suspend fun getDurationTrend(weeks: Int): List<DailyDurationData> {
        return cacheStrategy.getOrFetch("${CacheKeys.DURATION_TREND}$weeks", CacheTTL.PROGRESS) {
            val now = Instant.now()
            val endDate = now.toEpochMilli()
            val startDate = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()

            val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()
                .map(EntityMapper::toDomain)

            DurationCalculator.durationTrend(workouts, startDate, endDate)
        }
    }

    override suspend fun getWeeklyGoalProgress(target: Int): WeeklyGoalProgress = withContext(dispatcherProvider.default) {
        val now = LocalDate.now()
        val weekStart = now.with(java.time.DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weekEnd = now.with(java.time.DayOfWeek.SUNDAY).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val workouts = workoutDao.getWorkoutsByDateRangeFlow(weekStart, weekEnd).first()
        val completed = workouts.size
        val status = TrainingLoadCalculator.goalStatus(completed, target)

        WeeklyGoalProgress(
            completed = completed,
            target = target,
            status = status
        )
    }

    override suspend fun getWeekCalendarData(): List<DayInfo> = withContext(dispatcherProvider.default) {
        val now = LocalDate.now()
        val weekStart = now.with(java.time.DayOfWeek.MONDAY)
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val days = mutableListOf<DayInfo>()

        val weekStartTimestamp = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weekEndTimestamp = weekStart.plusDays(6).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weekWorkouts = workoutDao.getWorkoutsByDateRangeFlow(weekStartTimestamp, weekEndTimestamp).first()
            .map(EntityMapper::toDomain)

        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayEnd = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

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

    override suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> = withContext(dispatcherProvider.default) {
        val dayStart = DurationCalculator.startOfDay(timestamp)
        val dayEnd = DurationCalculator.endOfDay(timestamp)

        val workouts = workoutDao.getWorkoutsByDateRangeFlow(dayStart, dayEnd).first()
            .map(EntityMapper::toDomain)
        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { workoutId ->
            exerciseDao.getExercisesByWorkoutId(workoutId).map(EntityMapper::toDomain)
        }
        val exerciseIds = allExercises.map { it.id }
        val allSets = exerciseIds.flatMap { exerciseId ->
            setDao.getSetsByExerciseId(exerciseId).map(EntityMapper::toDomain)
        }

        val exercisesByWorkout = allExercises.groupBy { it.workoutId }
        val setsByExercise = allSets.groupBy { it.exerciseId }

        workouts.map { workout ->
            val exercises = exercisesByWorkout[workout.id] ?: emptyList()
            val totalVolume = exercises.sumOf { exercise ->
                VolumeCalculator.totalSetVolume(setsByExercise[exercise.id] ?: emptyList())
            }
            PlannedWorkout(
                id = workout.id,
                name = workout.name ?: "Workout",
                duration = workout.endTime?.let { DurationCalculator.workoutDurationMinutes(workout) },
                intensity = VolumeCalculator.absoluteIntensity(totalVolume),
                isCompleted = workout.endTime != null,
                routineId = workout.routineId,
                exerciseCount = exercises.size
            )
        }
    }

    override suspend fun getVolumeBalance(weeks: Int): VolumeBalance = withContext(dispatcherProvider.default) {
        val muscleGroupProgress = getMuscleGroupProgress(weeks)
        VolumeCalculator.volumeBalance(muscleGroupProgress)
    }

    override suspend fun getWeeklyProgress(weeks: Int): List<WeeklyProgress> {
        return cacheStrategy.getOrFetch("${CacheKeys.WEEKLY_PROGRESS}$weeks", CacheTTL.PROGRESS) {
            computeWeeklyProgress(weeks)
        }
    }

    override suspend fun getMemberSinceDate(): Long? {
        val workouts = workoutDao.getAllWorkoutsFlow().first()
        return workouts.minOfOrNull { it.startTime }
    }

    override suspend fun calculateAccountAgeDays(): Int {
        val memberSince = getMemberSinceDate() ?: return 0
        return DurationCalculator.accountAgeDays(memberSince, System.currentTimeMillis())
    }

    override suspend fun getProfileInfo(hasApiKey: Boolean, remoteWorkoutCount: Int?): ProfileInfo = withContext(dispatcherProvider.default) {
        val workouts = workoutDao.getAllWorkoutsFlow().first()
        val localCount = workouts.size

        val totalWorkouts = if (remoteWorkoutCount != null) {
            kotlin.math.max(remoteWorkoutCount, localCount)
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

    private suspend fun calculateTotalVolumeForWorkouts(workouts: List<Workout>): Double {
        if (workouts.isEmpty()) return 0.0

        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { workoutId ->
            exerciseDao.getExercisesByWorkoutId(workoutId).map(EntityMapper::toDomain)
        }
        val exerciseIds = allExercises.map { it.id }
        val allSets = exerciseIds.flatMap { exerciseId ->
            setDao.getSetsByExerciseId(exerciseId).map(EntityMapper::toDomain)
        }

        return VolumeCalculator.totalVolume(workouts, allExercises, allSets)
    }

    override suspend fun getConsistencyData(days: Int): List<DayInfo> = withContext(dispatcherProvider.default) {
        val now = LocalDate.now()
        val startDate = now.minusDays((days - 1).toLong())
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val resultDays = ArrayList<DayInfo>(days)

        val allWorkouts = workoutDao.getAllWorkoutsFlow().first().map(EntityMapper::toDomain)
        val workoutsByDate = allWorkouts.groupBy {
            Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        for (i in 0 until days) {
            val date = startDate.plusDays(i.toLong())
            val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val workouts = workoutsByDate[date] ?: emptyList()
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

    override suspend fun getMuscleRecoveryStatus(): Map<MuscleGroup, Float> = computeMuscleRecovery()

    private suspend fun computeWorkoutStats(): WorkoutStats {
        val workoutsWithDetails = workoutDao.getAllWorkoutsWithDetailsFlow().first()
        if (workoutsWithDetails.isEmpty()) {
            return WorkoutStats(
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
        }
        val workouts = workoutsWithDetails.map { EntityMapper.toDomain(it.workout) }.filter { !it.isDeleted }
        val allExercises = workoutsWithDetails.flatMap { wd -> wd.exercises.map { EntityMapper.toDomain(it.exercise) } }
        val allSets = workoutsWithDetails.flatMap { wd -> wd.exercises.flatMap { ex -> ex.sets.map(EntityMapper::toDomain) } }
        val totalWorkouts = workouts.size
        val totalVolume = VolumeCalculator.totalVolume(workouts, allExercises, allSets)
        val averageVolume = if (totalWorkouts > 0) totalVolume / totalWorkouts else 0.0
        val totalSets = allSets.size
        val totalDuration = DurationCalculator.totalDuration(workouts)
        val averageDuration = if (totalWorkouts > 0) totalDuration / totalWorkouts else 0L
        val currentStreak = StreakCalculator.currentStreak(workouts)
        val longestStreak = StreakCalculator.longestStreak(workouts)
        val weeklyProgress = computeWeeklyProgress(4)
        val bestWeek = weeklyProgress.maxByOrNull { it.totalVolume }
        return WorkoutStats(
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
    }

    private suspend fun computeWeeklyProgress(weeks: Int): List<WeeklyProgress> {
        val now = Instant.now()
        val endDate = now.toEpochMilli()
        val startDate = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()
            .map(EntityMapper::toDomain)
            .filter { !it.isDeleted }
        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { exerciseDao.getExercisesByWorkoutId(it).map(EntityMapper::toDomain) }
        val allSets = allExercises.map { it.id }.flatMap { setDao.getSetsByExerciseId(it).map(EntityMapper::toDomain) }
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
        return workouts.groupBy { workout ->
            Instant.ofEpochMilli(workout.startTime)
                .atZone(ZoneId.systemDefault())
                .get(weekFields.weekOfWeekBasedYear())
        }.map { (_, weekWorkouts) ->
            val weekStart = weekWorkouts.minOfOrNull { it.startTime } ?: 0L
            val totalVolume = VolumeCalculator.totalVolume(weekWorkouts, allExercises, allSets)
            WeeklyProgress(
                weekStartDate = weekStart,
                totalVolume = totalVolume,
                workoutCount = weekWorkouts.size,
                averageVolume = if (weekWorkouts.isNotEmpty()) totalVolume / weekWorkouts.size else 0.0
            )
        }
    }

    private suspend fun computePRDetails(limit: Int): List<PRDetails> {
        val prSets = setDao.getRecentPRsFlow(limit).first().map(EntityMapper::toDomain)
        if (prSets.isEmpty()) return emptyList()
        val exerciseIds = prSets.map { it.exerciseId }.distinct()
        val exercises = exerciseIds.mapNotNull { exerciseDao.getExerciseById(it)?.let(EntityMapper::toDomain) }
        val workouts = exercises.map { it.workoutId }.distinct().associateWith {
            workoutDao.getWorkoutById(it)?.let(EntityMapper::toDomain)
        }
        return prSets.mapNotNull { set ->
            val exercise = exercises.find { it.id == set.exerciseId } ?: return@mapNotNull null
            val workout = workouts[exercise.workoutId] ?: return@mapNotNull null
            val weight = set.weight ?: return@mapNotNull null
            PRDetails(
                exerciseName = exercise.name,
                date = workout.startTime,
                muscleGroup = exerciseRepository.getMuscleGroupForExercise(exercise) ?: "Unknown",
                weight = weight,
                workoutId = workout.id,
                setId = set.id
            )
        }
    }

    private suspend fun computeMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> {
        val now = Instant.now()
        val startDate = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, now.toEpochMilli()).first()
            .map(EntityMapper::toDomain)
            .filter { !it.isDeleted }
        if (workouts.isEmpty()) return emptyList()
        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { exerciseDao.getExercisesByWorkoutId(it).map(EntityMapper::toDomain) }
        val allSets = allExercises.associate { it.id to setDao.getSetsByExerciseId(it.id).map(EntityMapper::toDomain) }
        val muscleGroupData = mutableMapOf<String, Pair<Double, Int>>()
        allExercises.forEach { exercise ->
            val muscleGroup = exerciseRepository.getMuscleGroupForExercise(exercise) ?: return@forEach
            val sets = allSets[exercise.id] ?: emptyList()
            val exerciseVolume = VolumeCalculator.totalSetVolume(sets)
            val current = muscleGroupData[muscleGroup] ?: (0.0 to 0)
            muscleGroupData[muscleGroup] = (current.first + exerciseVolume) to (current.second + sets.size)
        }
        val totalVolume = muscleGroupData.values.sumOf { it.first }
        val averageVolume = if (muscleGroupData.isNotEmpty()) totalVolume / muscleGroupData.size else 0.0
        return muscleGroupData.map { (muscleGroup, data) ->
            val (volume, sets) = data
            MuscleGroupProgress(
                muscleGroup = muscleGroup,
                volume = volume,
                sets = sets,
                intensity = VolumeCalculator.relativeIntensity(volume, averageVolume)
            )
        }.sortedByDescending { it.volume }
    }

    private suspend fun computeMuscleRecovery(): Map<MuscleGroup, Float> = withContext(dispatcherProvider.io) {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
        val workouts = workoutDao.getWorkoutsSinceFlow(sevenDaysAgo).first()
            .map(EntityMapper::toDomain)
            .filter { !it.isDeleted }
        val lastTrainedMap = mutableMapOf<MuscleGroup, Long>()
        workouts.sortedByDescending { it.startTime }.forEach { workout ->
            exerciseDao.getExercisesByWorkoutId(workout.id)
                .map(EntityMapper::toDomain)
                .mapNotNull { MuscleGroup.fromString(exerciseRepository.getMuscleGroupForExercise(it)) }
                .distinct()
                .forEach { group ->
                    if (!lastTrainedMap.containsKey(group)) {
                        lastTrainedMap[group] = workout.startTime
                    }
                }
        }
        RecoveryScoreCalculator.muscleRecoveryStatus(now, lastTrainedMap)
    }
}
