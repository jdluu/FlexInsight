package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.core.errors.ErrorHandler
import com.example.flexinsight.data.model.PersonalRecord
import com.example.flexinsight.data.model.Set
import com.example.flexinsight.data.model.Workout
import com.example.flexinsight.data.model.WorkoutStats
import com.example.flexinsight.data.model.PRDetails
import com.example.flexinsight.data.model.VolumeTrend
import com.example.flexinsight.data.model.WeeklyVolumeData
import com.example.flexinsight.data.model.DailyDurationData
import com.example.flexinsight.data.model.MuscleGroupProgress
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    val prsWithDetails: List<PRDetails> = emptyList()
) {
    // Backward compatibility helper
    val isLoading: Boolean
        get() = loadingState.isLoading
}

class HistoryViewModel(
    private val repository: FlexRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        // Delay initialization slightly to ensure database is ready
        viewModelScope.launch {
            delay(100) // Small delay to ensure database is initialized
            loadHistoryData()
        }
    }
    
    private fun loadHistoryData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(loadingState = LoadingState.Loading, error = null)
                
                // Load all workouts - use first() to get initial value instead of continuous collection
                val workouts = try {
                    repository.getWorkouts().first()
                } catch (e: Exception) {
                    val apiError = ErrorHandler.handleError(e)
                    _uiState.value = _uiState.value.copy(
                        error = UiError.fromApiError(apiError),
                        loadingState = LoadingState.Error(apiError)
                    )
                    return@launch
                }
                
                try {
                    // Load stats
                    val stats = try {
                        repository.calculateStats()
                    } catch (e: Exception) {
                        WorkoutStats(
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
                    
                    // Load workout count
                    var count = 0
                    try {
                        count = repository.getWorkoutCount().first()
                    } catch (e: Exception) {
                        // Continue with 0 if it fails
                    }
                    
                    // Load recent PRs
                    var prs = emptyList<com.example.flexinsight.data.model.Set>()
                    try {
                        prs = repository.getRecentPRs(limit = 10).first()
                    } catch (e: Exception) {
                        // Continue with empty list if it fails
                    }
                    
                    // Load PRs with details
                    var prsWithDetails = emptyList<PRDetails>()
                    try {
                        prsWithDetails = repository.getPRsWithDetails(limit = 10)
                    } catch (e: Exception) {
                        // Continue with empty list if it fails
                    }
                    
                    // Load volume trend
                    var volumeTrend: VolumeTrend? = null
                    try {
                        volumeTrend = repository.calculateVolumeTrend(weeks = 4)
                    } catch (e: Exception) {
                        // Continue with null if it fails
                    }
                    
                    // Load weekly volume data
                    var weeklyVolumeData = emptyList<WeeklyVolumeData>()
                    try {
                        weeklyVolumeData = repository.getWeeklyVolumeData(weeks = 4)
                    } catch (e: Exception) {
                        // Continue with empty list if it fails
                    }
                    
                    // Load duration trend
                    var durationTrend = emptyList<DailyDurationData>()
                    try {
                        durationTrend = repository.getDurationTrend(weeks = 6)
                    } catch (e: Exception) {
                        // Continue with empty list if it fails
                    }
                    
                    // Load muscle group progress
                    var muscleGroupProgress = emptyList<MuscleGroupProgress>()
                    try {
                        muscleGroupProgress = repository.getMuscleGroupProgress(weeks = 4)
                    } catch (e: Exception) {
                        // Continue with empty list if it fails
                    }
                    
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
                        prsWithDetails = prsWithDetails,
                        error = null
                    )
                } catch (e: Exception) {
                    val apiError = ErrorHandler.handleError(e)
                    _uiState.value = _uiState.value.copy(
                        loadingState = LoadingState.Error(apiError),
                        error = UiError.fromApiError(apiError)
                    )
                }
            } catch (e: Exception) {
                val apiError = ErrorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    loadingState = LoadingState.Error(apiError),
                    error = UiError.fromApiError(apiError)
                )
            }
        }
    }
    
    fun refresh() {
        loadHistoryData()
    }
}

