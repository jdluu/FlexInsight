package com.jdluu.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.flexinsight.data.ai.FlexAIClient
import com.jdluu.flexinsight.core.errors.ErrorHandler
import com.jdluu.flexinsight.data.model.PersonalRecord
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.Workout
import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.WorkoutStats
import com.jdluu.flexinsight.data.model.PRDetails
import com.jdluu.flexinsight.data.model.VolumeTrend
import com.jdluu.flexinsight.data.model.WeeklyVolumeData
import com.jdluu.flexinsight.data.model.DailyDurationData
import com.jdluu.flexinsight.data.model.MuscleGroupProgress
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.jdluu.flexinsight.ui.utils.safeLaunch
import com.jdluu.flexinsight.ui.utils.toApiError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class HistoryUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val error: UiError? = null,
    val allWorkouts: List<Workout> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val workoutStats: WorkoutStats? = null,
    val recentPRs: List<Set> = emptyList(),
    val workoutCount: Int = 0,
    val volumeTrend: VolumeTrend? = null,
    val weeklyVolumeData: List<WeeklyVolumeData> = emptyList(),
    val durationTrend: List<DailyDurationData> = emptyList(),
    val muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
    val volumeBalance: com.jdluu.flexinsight.data.model.VolumeBalance? = null,
    val consistencyData: List<com.jdluu.flexinsight.data.model.DayInfo> = emptyList(),
    val prsWithDetails: List<PRDetails> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val dateFilter: String = "All Time",
    val muscleGroupFilter: String? = null,
    val units: String = "Imperial",
    val aiTrendAnalysis: String? = null,
    val isGeneratingTrend: Boolean = false,
    val compareData: ComparisonData? = null,
    val routineComparison: com.jdluu.flexinsight.domain.model.RoutineComparison? = null
) {
    val isLoading: Boolean
        get() = loadingState.isLoading
}

data class ComparisonData(
    val currentPeriodLabel: String,
    val previousPeriodLabel: String,
    val totalVolumeCurrent: Double,
    val totalVolumePrevious: Double,
    val totalWorkoutsCurrent: Int,
    val totalWorkoutsPrevious: Int,
    val avgDurationCurrent: Long,
    val avgDurationPrevious: Long
)


@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: FlexRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val aiClient: FlexAIClient,
    private val getWorkoutStatsUseCase: com.jdluu.flexinsight.domain.usecase.GetWorkoutStatsUseCase,
    private val getPRDetailsUseCase: com.jdluu.flexinsight.domain.usecase.GetPRDetailsUseCase,
    private val getWeeklyProgressUseCase: com.jdluu.flexinsight.domain.usecase.GetWeeklyProgressUseCase,
    private val compareRoutineSessionsUseCase: com.jdluu.flexinsight.domain.usecase.CompareRoutineSessionsUseCase
) : ViewModel() {

    fun loadRoutineComparison(routineId: String?, routineName: String) {
        viewModelScope.launch {
            val comparison = compareRoutineSessionsUseCase(routineId, routineName)
            _uiState.value = _uiState.value.copy(routineComparison = comparison)
        }
    }

    private val _uiState = MutableStateFlow(HistoryUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        // Collect units preference
        viewModelScope.launch {
            userPreferencesManager.unitsFlow.collect { units ->
                _uiState.value = _uiState.value.copy(units = units)
            }
        }

        // Delay initialization slightly to ensure database is ready
        loadHistoryData()
    }

    private fun loadHistoryData() {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Error(apiError),
                error = UiError.fromApiError(apiError)
            )
        }) {
            _uiState.value = _uiState.value.copy(loadingState = LoadingState.Loading, error = null)

            // Critical data: Workouts
            // If this fails, safeLaunch handles it as a critical error
            val workouts = repository.getWorkouts().first()

            // Optional data: Load with fail-safe defaults using runCatching
            val stats = runCatching { getWorkoutStatsUseCase() }
                .getOrDefault(WorkoutStats(
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
                ))

            val count = runCatching { repository.getProfileInfo().totalWorkouts }.getOrDefault(0)

            val prs = runCatching { repository.getRecentPRs(limit = 10).first() }.getOrDefault(emptyList())

            val prsWithDetails = runCatching { getPRDetailsUseCase(limit = 10) }.getOrDefault(emptyList())

            val volumeTrend = runCatching { repository.calculateVolumeTrend(weeks = 4) }.getOrNull()

            val weeklyVolumeData = runCatching { repository.getWeeklyVolumeData(weeks = 4) }.getOrDefault(emptyList())

            val durationTrend = runCatching { repository.getDurationTrend(weeks = 6) }.getOrDefault(emptyList())

            val muscleGroupProgress = runCatching { repository.getMuscleGroupProgress(weeks = 4) }.getOrDefault(emptyList())

            val volumeBalance = runCatching { repository.getVolumeBalance(weeks = 4) }.getOrNull()

            val consistencyData = runCatching { repository.getConsistencyData(days = 90) }.getOrDefault(emptyList())
            val exercises = runCatching { repository.getAllExercises().first() }.getOrDefault(emptyList())

            val periodComparison = runCatching { repository.getPeriodComparison() }.getOrNull()
            val comparisonData = periodComparison?.let {
                ComparisonData(
                    currentPeriodLabel = it.currentPeriodLabel,
                    previousPeriodLabel = it.previousPeriodLabel,
                    totalVolumeCurrent = it.totalVolumeCurrent,
                    totalVolumePrevious = it.totalVolumePrevious,
                    totalWorkoutsCurrent = it.totalWorkoutsCurrent,
                    totalWorkoutsPrevious = it.totalWorkoutsPrevious,
                    avgDurationCurrent = it.avgDurationCurrent,
                    avgDurationPrevious = it.avgDurationPrevious
                )
            }

            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Success,
                allWorkouts = workouts,
                workouts = workouts,
                workoutStats = stats,
                recentPRs = prs,
                workoutCount = count,
                volumeTrend = volumeTrend,
                weeklyVolumeData = weeklyVolumeData,
                durationTrend = durationTrend,
                muscleGroupProgress = muscleGroupProgress,
                volumeBalance = volumeBalance,
                consistencyData = consistencyData,
                prsWithDetails = prsWithDetails,
                exercises = exercises,
                compareData = comparisonData,
                error = null
            )

            val latestRoutine = workouts.firstOrNull { it.routineId != null && !it.isDeleted }
            val routineComparison = latestRoutine?.let {
                compareRoutineSessionsUseCase(it.routineId, it.name ?: "Routine")
            }

            _uiState.value = _uiState.value.copy(routineComparison = routineComparison)

            generateTrendAnalysis(stats, count)
        }
    }

    private suspend fun generateTrendAnalysis(stats: WorkoutStats, count: Int) {
        if (!aiClient.isAvailable()) return
        if (_uiState.value.aiTrendAnalysis != null) return

        _uiState.value = _uiState.value.copy(isGeneratingTrend = true)

        val prompt = "Analyze my gym progress. " +
                "Total workouts: $count. " +
                "Total Volume: ${(stats.totalVolume / 1000).toInt()}k kg. " +
                "Streak: ${stats.currentStreak} days. " +
                "Write a 1-sentence summary of my consistency and a short motivational quote."

        val result = aiClient.generateResponse(prompt)

        _uiState.value = _uiState.value.copy(isGeneratingTrend = false)

        if (result is com.jdluu.flexinsight.core.errors.Result.Success) {
            _uiState.value = _uiState.value.copy(aiTrendAnalysis = result.data)
        }
    }
    
    fun refresh() {
        loadHistoryData()
    }

    /**
     * Filters the workout list by the selected date range.
     * Supported values: "All Time", "This Week", "This Month", "Last 3 Months".
     */
    fun setDateFilter(filter: String) {
        val now = System.currentTimeMillis()
        val cutoff = when (filter) {
            "This Week" -> now - 7L * 24 * 60 * 60 * 1000
            "This Month" -> now - 30L * 24 * 60 * 60 * 1000
            "Last 3 Months" -> now - 90L * 24 * 60 * 60 * 1000
            else -> 0L
        }

        val filtered = if (cutoff > 0L) {
            _uiState.value.allWorkouts.filter { it.startTime >= cutoff }
        } else {
            _uiState.value.allWorkouts
        }

        _uiState.value = _uiState.value.copy(
            dateFilter = filter,
            workouts = applyMuscleGroupFilter(filtered, _uiState.value.muscleGroupFilter)
        )
    }

    /**
     * Filters the workout list by muscle group.
     * Null clears the filter.
     */
    fun setMuscleGroupFilter(muscleGroup: String?) {
        val dateFiltered = applyDateFilter(
            _uiState.value.allWorkouts,
            _uiState.value.dateFilter
        )

        _uiState.value = _uiState.value.copy(
            muscleGroupFilter = muscleGroup,
            workouts = applyMuscleGroupFilter(dateFiltered, muscleGroup)
        )
    }

    private fun applyDateFilter(workouts: List<Workout>, filter: String): List<Workout> {
        val now = System.currentTimeMillis()
        val cutoff = when (filter) {
            "This Week" -> now - 7L * 24 * 60 * 60 * 1000
            "This Month" -> now - 30L * 24 * 60 * 60 * 1000
            "Last 3 Months" -> now - 90L * 24 * 60 * 60 * 1000
            else -> 0L
        }
        return if (cutoff > 0L) workouts.filter { it.startTime >= cutoff } else workouts
    }

    private fun applyMuscleGroupFilter(
        workouts: List<Workout>,
        muscleGroup: String?
    ): List<Workout> {
        if (muscleGroup == null) return workouts
        return workouts.filter { workout ->
            workout.name?.contains(muscleGroup, ignoreCase = true) == true
        }
    }
}
