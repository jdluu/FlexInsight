package com.jdluu.flexinsight.fakes

import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.core.network.NetworkMonitor
import com.jdluu.flexinsight.core.network.NetworkState
import com.jdluu.flexinsight.data.ai.AiFeatureStatus
import com.jdluu.flexinsight.data.ai.FlexAIClient
import com.jdluu.flexinsight.data.ai.HevyAiDataAccessor
import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.health.HealthConnectSnapshot
import com.jdluu.flexinsight.data.model.CreateRoutineRequest
import com.jdluu.flexinsight.data.model.DayInfo
import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.ExerciseHistoryResponse
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
import com.jdluu.flexinsight.data.model.DailyDurationData
import com.jdluu.flexinsight.data.model.WeeklyProgress
import com.jdluu.flexinsight.data.model.WeeklyVolumeData
import com.jdluu.flexinsight.data.model.WeeklyGoalProgress
import com.jdluu.flexinsight.data.model.VolumeBalance
import com.jdluu.flexinsight.data.model.VolumeTrend
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.domain.ai.AiContextProvider
import com.jdluu.flexinsight.data.repository.ExerciseRepository
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.data.repository.RoutineRepository
import com.jdluu.flexinsight.widget.WidgetUpdater
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Hand-written scriptable [FlexRepository] double for view-model tests.
 * Defaults are inert; individual tests only configure the members they assert on.
 */
class ScriptableFlexRepository : FlexRepository {

    var recentWorkouts: List<Workout> = emptyList()
    var recentWorkoutsError: Exception? = null

    var workouts: List<Workout> = emptyList()
    var workoutsError: Exception? = null

    var workoutById: Workout? = null

    var allExercises: List<Exercise> = emptyList()
    var exercisesByWorkoutId: Map<String, List<Exercise>> = emptyMap()
    var exercisesError: Exception? = null
    var setsByExerciseId: Map<String, List<Set>> = emptyMap()

    var profileInfo: ProfileInfo = ProfileInfo(null, null, false, 0, 0)
    var profileInfoError: Exception? = null

    var workoutStatsResult: SingleWorkoutStats = SingleWorkoutStats(0, 0, 0.0)
    var recentPRs: List<Set> = emptyList()
    var recentPrsError: Exception? = null
    var prsWithDetails: List<PRDetails> = emptyList()
    var prsError: Exception? = null
    var muscleGroupProgress: List<MuscleGroupProgress> = emptyList()
    var muscleRecovery: Map<MuscleGroup, Float> = emptyMap()
    var volumeBalance: VolumeBalance = VolumeBalance(0f, 0f, 0f, 0f)
    var consistencyDays: List<DayInfo> = emptyList()
    var periodComparison: PeriodComparison? = null
    var weeklyGoalProgress: WeeklyGoalProgress = WeeklyGoalProgress(0, 5, "Behind")
    var weekCalendar: List<DayInfo> = emptyList()
    var plannedForDay: (Long) -> List<PlannedWorkout> = { emptyList() }
    var routines: List<Routine> = emptyList()
    var routineFolders: List<RoutineFolder> = emptyList()

    var updateStatusResult: Result<Unit> = Result.Success(Unit)

    /** When set, updateWorkoutStatus suspends until completed; the value is then returned. */
    var updateStatusGate: CompletableDeferred<Result<Unit>>? = null

    var rescheduleResult: Result<Unit> = Result.Success(Unit)
    var syncResult: Result<Unit> = Result.Success(Unit)

    /** When set, syncAllData suspends until completed before returning [syncResult]. */
    var syncGate: CompletableDeferred<Unit>? = null

    val statusUpdates = mutableListOf<Pair<String, Boolean>>()
    val reschedules = mutableListOf<Pair<String, Long>>()
    var syncCalls = 0
        private set
    var clearCacheCalls = 0
        private set

    override fun invalidateApiService() {}

    override fun getWorkouts(): Flow<List<Workout>> {
        workoutsError?.let { error -> return flow { throw error } }
        return flowOf(workouts)
    }

    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> {
        recentWorkoutsError?.let { error -> return flow { throw error } }
        return flowOf(recentWorkouts.take(limit))
    }

    override fun getAllExercises(): Flow<List<Exercise>> = flowOf(allExercises)

    override suspend fun getExerciseHistory(templateId: String): Result<ExerciseHistoryResponse> =
        Result.Error(ApiError.Unknown("not faked"))

    override suspend fun getWorkoutById(workoutId: String): Workout? = workoutById

    override fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> = flowOf(workoutById)

    override fun getWorkoutCount(): Flow<Int> = flowOf(workouts.size)

    override suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise> {
        exercisesError?.let { throw it }
        return exercisesByWorkoutId[workoutId].orEmpty()
    }

    override suspend fun getSetsByExerciseId(exerciseId: String): List<Set> =
        setsByExerciseId[exerciseId].orEmpty()

    override suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean): Result<Unit> {
        statusUpdates += workoutId to isCompleted
        updateStatusGate?.let { return it.await() }
        return updateStatusResult
    }

    override suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit> {
        reschedules += workoutId to newStartTime
        return rescheduleResult
    }

    override suspend fun calculateStats() = TestDefaults.emptyStats

    override suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats =
        workoutStatsResult

    override fun getRecentPRs(limit: Int): Flow<List<Set>> {
        recentPrsError?.let { error -> return flow { throw error } }
        return flowOf(recentPRs)
    }

    override suspend fun getPRsWithDetails(limit: Int): List<PRDetails> {
        prsError?.let { throw it }
        return prsWithDetails
    }

    override suspend fun getAllPRsWithDetails(): List<PRDetails> {
        prsError?.let { throw it }
        return prsWithDetails
    }

    override suspend fun getMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> =
        muscleGroupProgress

    override suspend fun calculateVolumeTrend(weeks: Int) = VolumeTrend(0.0, 0.0, 0.0)

    override suspend fun getPeriodComparison(): PeriodComparison? = periodComparison

    override suspend fun getWeeklyVolumeData(weeks: Int) = emptyList<WeeklyVolumeData>()

    override suspend fun getDurationTrend(weeks: Int) = emptyList<DailyDurationData>()

    override suspend fun getWeeklyGoalProgress(target: Int): WeeklyGoalProgress = weeklyGoalProgress

    override suspend fun getWeekCalendarData(): List<DayInfo> = weekCalendar

    override suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> =
        plannedForDay(timestamp)

    override suspend fun getVolumeBalance(weeks: Int): VolumeBalance = volumeBalance

    override suspend fun getWeeklyProgress(weeks: Int) = emptyList<WeeklyProgress>()

    override suspend fun getMemberSinceDate(): Long? = null

    override suspend fun calculateAccountAgeDays(): Int = 0

    override suspend fun getProfileInfo(): ProfileInfo {
        profileInfoError?.let { throw it }
        return profileInfo
    }

    override suspend fun getConsistencyData(days: Int): List<DayInfo> = consistencyDays

    override suspend fun getMuscleRecoveryStatus(): Map<MuscleGroup, Float> = muscleRecovery

    override fun getRoutines(): Flow<List<Routine>> = flowOf(routines)

    override suspend fun getRoutineById(routineId: String): Routine? =
        routines.firstOrNull { it.id == routineId }

    override suspend fun getRoutineFolders(): List<RoutineFolder> = routineFolders

    override suspend fun syncAllData(): Result<Unit> {
        syncCalls++
        syncGate?.await()
        return syncResult
    }

    override fun clearCache() {
        clearCacheCalls++
    }

    override suspend fun syncWithCloud() {}
}

/** Hand-written scriptable [RoutineRepository] double. */
class ScriptableRoutineRepository : RoutineRepository {
    var createResult: Result<String> = Result.Success("routine-1")
    val createdRequests = mutableListOf<CreateRoutineRequest>()

    override fun invalidateApiService() {}
    override suspend fun syncRoutines(): Result<Unit> = Result.Success(Unit)
    override fun getRoutines(): Flow<List<Routine>> = flowOf(emptyList())
    override suspend fun getRoutineById(routineId: String): Result<Routine> =
        Result.Error(ApiError.Unknown("not faked"))
    override suspend fun getRoutineFolders(): Result<List<RoutineFolder>> =
        Result.Success(emptyList())
    override suspend fun createRoutine(request: CreateRoutineRequest): Result<String> {
        createdRequests += request
        return createResult
    }
}

/** Hand-written scriptable [ExerciseRepository] double. */
class ScriptableExerciseRepository : ExerciseRepository {
    var templateNameMapping: Result<Map<String, String>> = Result.Success(emptyMap())

    override fun invalidateApiService() {}
    override suspend fun getExerciseTemplateMapping(): Result<Map<String, String>> =
        templateNameMapping
    override suspend fun getExerciseTemplateNameMapping(): Result<Map<String, String>> =
        templateNameMapping
    override suspend fun getMuscleGroupForExercise(exercise: Exercise): String? = null
    override fun getExercisesByWorkoutId(workoutId: String): Flow<List<Exercise>> =
        flowOf(emptyList())
    override fun getAllExercises(): Flow<List<Exercise>> = flowOf(emptyList())
    override suspend fun getExercisesByWorkoutIdSuspend(workoutId: String): List<Exercise> =
        emptyList()
    override suspend fun getExerciseById(exerciseId: String): Exercise? = null
    override suspend fun getExerciseHistory(templateId: String): Result<ExerciseHistoryResponse> =
        Result.Error(ApiError.Unknown("not faked"))
}

/** Static [AiContextProvider] for use cases that need a context snapshot text. */
class StaticAiContextProvider(
    private val text: String = "System Context - Hevy Training Data"
) : AiContextProvider {
    override suspend fun buildContext(userQuery: String?): HevyAiDataAccessor.ContextSnapshot =
        HevyAiDataAccessor.ContextSnapshot(
            text = text,
            hasWorkoutData = true,
            hasApiKey = true,
            workoutCount = 1,
            usesLiveExerciseHistory = false
        )
}

/**
 * NetworkMonitor is a final Android-bound class with no interface; tests stub its
 * state flow via MockK and drive transitions through the returned StateFlow.
 */
fun networkMonitorStub(
    initial: NetworkState = NetworkState.Unknown
): Pair<NetworkMonitor, MutableStateFlow<NetworkState>> {
    val flow = MutableStateFlow(initial)
    val monitor = mockk<NetworkMonitor> {
        every { networkState } returns flow
    }
    return monitor to flow
}

/** WidgetUpdater touches Glance app widgets; tests only observe that it was invoked. */
fun widgetUpdaterStub(): WidgetUpdater = mockk(relaxUnitFun = true)

/**
 * HealthConnectRepository is final and bound to the Health Connect SDK; tests stub
 * it at the repository boundary.
 */
fun healthConnectRepositoryStub(
    sdkAvailable: Boolean = false,
    snapshot: HealthConnectSnapshot = HealthConnectSnapshot()
): HealthConnectRepository = mockk {
    every { requiredPermissions } returns emptySet()
    every { isSdkAvailable() } returns sdkAvailable
    coEvery { readSnapshot() } returns snapshot
    coEvery { writeWorkoutsToHealthConnect(any()) } returns 0
}

/**
 * ApiKeyManager is final and backed by EncryptedSharedPreferences (no keystore under
 * Robolectric); tests stub it while mirroring the real format-validation rule.
 */
class ScriptableApiKeyManager {
    val keyFlow = MutableStateFlow<String?>(null)
    val savedKeys = mutableListOf<String>()

    val manager: ApiKeyManager = mockk {
        every { apiKeyFlow } returns this@ScriptableApiKeyManager.keyFlow
        every { isValidApiKeyFormat(any()) } answers {
            val key: String = firstArg()
            key.isNotBlank() && key.length >= 10
        }
        coEvery { saveApiKey(any()) } coAnswers {
            val key: String = firstArg()
            savedKeys += key
            keyFlow.value = key
        }
    }
}

/**
 * Resets every preference key view-model tests mutate. The preferences DataStore is a
 * process-wide singleton, so without this, state leaks between Robolectric tests.
 */
suspend fun UserPreferencesManager.resetForTests() {
    setWeeklyGoal(5)
    setTheme("System")
    setUnits("Imperial")
    setViewOnlyMode(true)
    setForceAiEnable(false)
    setNotificationsEnabled(true)
    setHealthConnectEnabled(false)
    setHealthConnectWriteEnabled(false)
}

/** Minimal FlexAIClient double for view models that only generate one-shot responses. */
class OneShotFakeAiClient(
    var available: Boolean = false,
    var response: Result<String> = Result.Success("AI says hi")
) : FlexAIClient {
    val prompts = mutableListOf<String>()

    override suspend fun isAvailable(): Boolean = available
    override suspend fun getFeatureStatus(): AiFeatureStatus = AiFeatureStatus.Ready
    override suspend fun prepareModel(): Result<Unit> = Result.Success(Unit)
    override suspend fun generateResponse(prompt: String, history: List<Pair<String, String>>): Result<String> {
        prompts += prompt
        return response
    }
    override suspend fun generateWorkoutPlan(prompt: String): Result<String> {
        prompts += prompt
        return response
    }
    override fun generateResponseStream(prompt: String, history: List<Pair<String, String>>) = flow<String> { }
}
