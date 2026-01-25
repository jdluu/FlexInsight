package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.data.ai.FlexAIClient
import com.example.flexinsight.core.errors.ErrorHandler
import com.example.flexinsight.data.model.PersonalRecord
import com.example.flexinsight.data.model.Set
import com.example.flexinsight.data.model.Workout
import com.example.flexinsight.data.model.Exercise
import com.example.flexinsight.data.model.WorkoutStats
import com.example.flexinsight.data.model.PRDetails
import com.example.flexinsight.data.model.VolumeTrend
import com.example.flexinsight.data.model.WeeklyVolumeData
import com.example.flexinsight.data.model.DailyDurationData
import com.example.flexinsight.data.model.MuscleGroupProgress
import com.example.flexinsight.data.preferences.UserPreferencesManager
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.flexinsight.ui.utils.safeLaunch
import com.example.flexinsight.ui.utils.toApiError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class HistoryUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val error: UiError? = null,
    val workouts: List<Workout> = emptyList(),
    val workoutStats: WorkoutStats? = null,
    val recentPRs: List<Set> = emptyList(),
    val workoutCount: Int = 0,
    val volumeTrend: VolumeTrend? = null,
    val weeklyVolumeData: List<WeeklyVolumeData> = emptyList(),
    val durationTrend: List<DailyDurationData> = emptyList(),
    val muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
    val volumeBalance: com.example.flexinsight.data.model.VolumeBalance? = null,
    val consistencyData: List<com.example.flexinsight.data.model.DayInfo> = emptyList(),
    val prsWithDetails: List<PRDetails> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val dateFilter: String = "All Time",
    val units: String = "Imperial",
    val aiTrendAnalysis: String? = null,
    val isGeneratingTrend: Boolean = false,
    val compareData: ComparisonData? = null
) {
    // Backward compatibility helper
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
    private val getWorkoutStatsUseCase: com.example.flexinsight.domain.usecase.GetWorkoutStatsUseCase,
    private val getPRDetailsUseCase: com.example.flexinsight.domain.usecase.GetPRDetailsUseCase,
    private val getWeeklyProgressUseCase: com.example.flexinsight.domain.usecase.GetWeeklyProgressUseCase
) : ViewModel() {

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
        viewModelScope.launch {
            delay(100) // Small delay to ensure database is initialized
            loadHistoryData()
        }
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
            // val consistencyData = emptyList<com.example.flexinsight.data.model.DayInfo>()

            val exercises = runCatching { repository.getAllExercises().first() }.getOrDefault(emptyList())

            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Success,
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
                error = null
            )
            
            // Generate AI Insight
            generateTrendAnalysis(stats, count)
            
            /*
             ### AI Intelligence (Deep Context)
            - **Problem Fixed**: The AI was previously limited to seeing only the last 3 workout names/dates, making it impossible to analyze actual performance trends.
            - **Deep Context Injection**: 
                - Increased memory to the **last 7 workouts**.
                - Expanded data to include **every exercise and every set** (Weight, Reps, RPE).
                - Structured the data for the AI to "see" performance trends (e.g., comparing squats across weeks).
            - **Backend Scaling**: Updated the `Repository` and `DAO` layers to support deep-fetching of exercises and sets for the AI session.

            ### Streaming AI Responses.
            */
            
            // Calculate Comparison Data (Simple Mock for "This Month" vs "Last Month")
            // In a real app, we'd query the DB for specific ranges.
            // Here we just project realistic variance from total stats.
            
            val comparisonData = ComparisonData(
                currentPeriodLabel = "This Month",
                previousPeriodLabel = "Last Month",
                totalVolumeCurrent = stats.totalVolume * 0.2, // Mock 20% of total
                totalVolumePrevious = stats.totalVolume * 0.18, // Mock slightly less
                totalWorkoutsCurrent = (count * 0.2).toInt().coerceAtLeast(1),
                totalWorkoutsPrevious = (count * 0.18).toInt().coerceAtLeast(1),
                avgDurationCurrent = stats.averageDuration,
                avgDurationPrevious = (stats.averageDuration * 0.95).toLong()
            )
            
            _uiState.value = _uiState.value.copy(compareData = comparisonData)
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

        if (result is com.example.flexinsight.core.errors.Result.Success) {
            _uiState.value = _uiState.value.copy(aiTrendAnalysis = result.data)
        }
    }
    
    // ... existing helper methods ...

    fun refresh() {
        loadHistoryData()
    }

    fun setDateFilter(filter: String) {
        _uiState.value = _uiState.value.copy(dateFilter = filter)
        // In a real app, this would re-query the repository with a date range
        // For now, we update the UI state so the UI can filter or show the selected range
    }
}

