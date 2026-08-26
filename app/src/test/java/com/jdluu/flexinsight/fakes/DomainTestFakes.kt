package com.jdluu.flexinsight.fakes

import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.ai.AiFeatureStatus
import com.jdluu.flexinsight.data.ai.FlexAIClient
import com.jdluu.flexinsight.data.model.DayInfo
import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.MuscleGroup
import com.jdluu.flexinsight.data.model.MuscleGroupProgress
import com.jdluu.flexinsight.data.model.PRDetails
import com.jdluu.flexinsight.data.model.PeriodComparison
import com.jdluu.flexinsight.data.model.PlannedWorkout
import com.jdluu.flexinsight.data.model.ProfileInfo
import com.jdluu.flexinsight.data.model.Routine
import com.jdluu.flexinsight.data.model.RoutineFolder
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.SingleWorkoutStats
import com.jdluu.flexinsight.data.model.Workout
import com.jdluu.flexinsight.data.model.WorkoutResponse
import com.jdluu.flexinsight.data.model.WorkoutStats
import com.jdluu.flexinsight.data.model.DailyDurationData
import com.jdluu.flexinsight.data.model.ExerciseHistoryResponse
import com.jdluu.flexinsight.data.model.WeeklyGoalProgress
import com.jdluu.flexinsight.data.model.WeeklyProgress
import com.jdluu.flexinsight.data.model.WeeklyVolumeData
import com.jdluu.flexinsight.data.model.VolumeBalance
import com.jdluu.flexinsight.data.model.VolumeTrend
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.data.repository.StatsRepository
import com.jdluu.flexinsight.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/** Zero-argument defaults so fakes only need the members a given test cares about. */
object TestDefaults {
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

    fun workout(
        id: String,
        name: String? = "Workout $id",
        startTime: Long = 0L,
        endTime: Long? = null,
        routineId: String? = null,
        isDeleted: Boolean = false
    ) = Workout(
        id = id,
        name = name,
        startTime = startTime,
        endTime = endTime,
        notes = null,
        routineId = routineId,
        isDeleted = isDeleted
    )

    fun exercise(id: String, workoutId: String, name: String = "Exercise $id") = Exercise(
        id = id,
        workoutId = workoutId,
        exerciseTemplateId = null,
        name = name,
        notes = null,
        restDuration = null
    )

    fun set(
        id: String,
        exerciseId: String,
        weight: Double? = null,
        reps: Int? = null,
        rpe: Double? = null,
        number: Int = 1
    ) = Set(
        id = id,
        exerciseId = exerciseId,
        number = number,
        weight = weight,
        reps = reps,
        rpe = rpe,
        distance = null,
        duration = null,
        restDuration = null,
        notes = null
    )
}

class FakeFlexRepository : FlexRepository {
    var statsToReturn: WorkoutStats = TestDefaults.emptyStats
    var statsError: Exception? = null

    var goalToReturn: WeeklyGoalProgress? = WeeklyGoalProgress(0, 5, "Behind")
    var goalError: Exception? = null

    var trendToReturn: VolumeTrend = VolumeTrend(currentVolume = 0.0, previousVolume = 0.0, percentageChange = 0.0)
    var trendError: Exception? = null

    var consistencyToReturn: List<DayInfo> = emptyList()
    var consistencyError: Exception? = null

    var prsToReturn: List<PRDetails> = emptyList()

    var workoutByIdToReturn: Workout? = null

    val requestedConsistencyDays = mutableListOf<Int>()
    val requestedPrLimits = mutableListOf<Int>()

    override suspend fun calculateStats(): WorkoutStats {
        statsError?.let { throw it }
        return statsToReturn
    }

    override suspend fun getWeeklyGoalProgress(target: Int): WeeklyGoalProgress {
        goalError?.let { throw it }
        return goalToReturn ?: throw IllegalStateException("No goal configured")
    }

    override suspend fun calculateVolumeTrend(weeks: Int): VolumeTrend {
        trendError?.let { throw it }
        return trendToReturn
    }

    override suspend fun getConsistencyData(days: Int): List<DayInfo> {
        requestedConsistencyDays += days
        consistencyError?.let { throw it }
        return consistencyToReturn
    }

    override suspend fun getPRsWithDetails(limit: Int): List<PRDetails> {
        requestedPrLimits += limit
        return prsToReturn
    }

    override suspend fun getWorkoutById(workoutId: String): Workout? = workoutByIdToReturn

    // Unused by the use cases under test; benign defaults keep the fake honest.
    override fun invalidateApiService() {}
    override fun getWorkouts(): Flow<List<Workout>> = flowOf(emptyList())
    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> = flowOf(emptyList())
    override fun getAllExercises(): Flow<List<Exercise>> = flowOf(emptyList())
    override suspend fun getExerciseHistory(templateId: String): Result<ExerciseHistoryResponse> =
        Result.Error(ApiError.Unknown("not faked"))
    override fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> = flowOf(null)
    override fun getWorkoutCount(): Flow<Int> = flowOf(0)
    override suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise> = emptyList()
    override suspend fun getSetsByExerciseId(exerciseId: String): List<Set> = emptyList()
    override suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean): Result<Unit> =
        Result.Success(Unit)
    override suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit> =
        Result.Success(Unit)
    override suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats =
        SingleWorkoutStats(0, 0, 0.0)
    override fun getRecentPRs(limit: Int): Flow<List<Set>> = flowOf(emptyList())
    override suspend fun getAllPRsWithDetails(): List<PRDetails> = emptyList()
    override suspend fun getMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> = emptyList()
    override suspend fun getPeriodComparison(): PeriodComparison? = null
    override suspend fun getWeeklyVolumeData(weeks: Int): List<WeeklyVolumeData> = emptyList()
    override suspend fun getDurationTrend(weeks: Int): List<DailyDurationData> = emptyList()
    override suspend fun getWeekCalendarData(): List<DayInfo> = emptyList()
    override suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> = emptyList()
    override suspend fun getVolumeBalance(weeks: Int): VolumeBalance = VolumeBalance(0f, 0f, 0f, 0f)
    override suspend fun getWeeklyProgress(weeks: Int): List<WeeklyProgress> = emptyList()
    override suspend fun getMemberSinceDate(): Long? = null
    override suspend fun calculateAccountAgeDays(): Int = 0
    override suspend fun getProfileInfo(): ProfileInfo =
        ProfileInfo(null, null, false, 0, 0)
    override suspend fun getMuscleRecoveryStatus(): Map<MuscleGroup, Float> = emptyMap()
    override fun getRoutines(): Flow<List<Routine>> = flowOf(emptyList())
    override suspend fun getRoutineById(routineId: String): Routine? = null
    override suspend fun getRoutineFolders(): List<RoutineFolder> = emptyList()
    override suspend fun syncAllData(): Result<Unit> = Result.Success(Unit)
    override fun clearCache() {}
    override suspend fun syncWithCloud() {}
}

class FakeWorkoutRepository : WorkoutRepository {
    /** Backed by StateFlow so tests can emit values and observe them with Turbine. */
    val workoutsFlow = MutableStateFlow<List<Workout>>(emptyList())

    val exercisesByWorkout = mutableMapOf<String, List<Exercise>>()
    val setsByExercise = mutableMapOf<String, List<Set>>()

    override fun getWorkouts(): Flow<List<Workout>> = workoutsFlow

    override suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise> =
        exercisesByWorkout[workoutId] ?: emptyList()

    override suspend fun getSetsByExerciseId(exerciseId: String): List<Set> =
        setsByExercise[exerciseId] ?: emptyList()

    override fun invalidateApiService() {}
    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> = flowOf(emptyList())
    override suspend fun getWorkoutById(workoutId: String): Result<Workout> =
        Result.Error(ApiError.Unknown("not faked"))
    override fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> = flowOf(null)
    override fun getWorkoutCount(): Flow<Int> = flowOf(0)
    override suspend fun getRemoteWorkoutCount(): Result<Int> = Result.Error(ApiError.Unknown("not faked"))
    override fun getWorkoutsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<Workout>> =
        flowOf(emptyList())
    override suspend fun syncWorkouts(): Result<Unit> = Result.Success(Unit)
    override suspend fun saveWorkoutWithExercisesAndSets(workoutResponse: WorkoutResponse) {}
    override suspend fun getMostRecentSyncedTimestamp(): Long? = null
    override suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean, endTime: Long?): Result<Unit> =
        Result.Success(Unit)
    override suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit> =
        Result.Success(Unit)
}

class FakeStatsRepository : StatsRepository {
    var statsToReturn: WorkoutStats = TestDefaults.emptyStats
    var muscleGroupProgressToReturn: List<MuscleGroupProgress> = emptyList()
    var weeklyProgressToReturn: List<WeeklyProgress> = emptyList()
    var prsToReturn: List<PRDetails> = emptyList()
    var recoveryToReturn: Map<MuscleGroup, Float> = emptyMap()

    val requestedMuscleGroupWeeks = mutableListOf<Int>()
    val requestedWeeklyProgressWeeks = mutableListOf<Int>()
    val requestedPrLimits = mutableListOf<Int>()

    override suspend fun calculateStats(): WorkoutStats = statsToReturn

    override suspend fun getMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> {
        requestedMuscleGroupWeeks += weeks
        return muscleGroupProgressToReturn
    }

    override suspend fun getWeeklyProgress(weeks: Int): List<WeeklyProgress> {
        requestedWeeklyProgressWeeks += weeks
        return weeklyProgressToReturn
    }

    override suspend fun getPRsWithDetails(limit: Int): List<PRDetails> {
        requestedPrLimits += limit
        return prsToReturn
    }

    override suspend fun getMuscleRecoveryStatus(): Map<MuscleGroup, Float> = recoveryToReturn

    override fun invalidateStatsCache() {}
    override suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats =
        SingleWorkoutStats(0, 0, 0.0)
    override fun getRecentPRs(limit: Int): Flow<List<Set>> = flowOf(emptyList())
    override suspend fun getAllPRsWithDetails(): List<PRDetails> = emptyList()
    override suspend fun calculateVolumeTrend(weeks: Int): VolumeTrend =
        VolumeTrend(0.0, 0.0, 0.0)
    override suspend fun getPeriodComparison(): PeriodComparison? = null
    override suspend fun getWeeklyVolumeData(weeks: Int): List<WeeklyVolumeData> = emptyList()
    override suspend fun getDurationTrend(weeks: Int): List<DailyDurationData> = emptyList()
    override suspend fun getWeeklyGoalProgress(target: Int): WeeklyGoalProgress =
        WeeklyGoalProgress(0, target, "Behind")
    override suspend fun getWeekCalendarData(): List<DayInfo> = emptyList()
    override suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> = emptyList()
    override suspend fun getVolumeBalance(weeks: Int): VolumeBalance = VolumeBalance(0f, 0f, 0f, 0f)
    override suspend fun getMemberSinceDate(): Long? = null
    override suspend fun calculateAccountAgeDays(): Int = 0
    override suspend fun getProfileInfo(hasApiKey: Boolean, remoteWorkoutCount: Int?): ProfileInfo =
        ProfileInfo(null, null, hasApiKey, 0, 0)
    override suspend fun getConsistencyData(days: Int): List<DayInfo> = emptyList()
}

class FakeFlexAIClient : FlexAIClient {
    var available: Boolean = true
    var response: Result<String> = Result.Success("AI says hi")

    val prompts = mutableListOf<String>()

    override suspend fun isAvailable(): Boolean = available

    override suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>>
    ): Result<String> {
        prompts += prompt
        return response
    }

    override suspend fun getFeatureStatus(): AiFeatureStatus = AiFeatureStatus.Ready
    override suspend fun prepareModel(): Result<Unit> = Result.Success(Unit)
    override suspend fun generateWorkoutPlan(prompt: String): Result<String> = response
    override fun generateResponseStream(prompt: String, history: List<Pair<String, String>>) = emptyFlow<String>()
}
