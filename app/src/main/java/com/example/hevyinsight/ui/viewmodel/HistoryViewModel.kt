package com.example.hevyinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hevyinsight.data.model.PersonalRecord
import com.example.hevyinsight.data.model.Set
import com.example.hevyinsight.data.model.Workout
import com.example.hevyinsight.data.model.WorkoutStats
import com.example.hevyinsight.data.model.PRDetails
import com.example.hevyinsight.data.model.VolumeTrend
import com.example.hevyinsight.data.model.WeeklyVolumeData
import com.example.hevyinsight.data.model.DailyDurationData
import com.example.hevyinsight.data.model.MuscleGroupProgress
import com.example.hevyinsight.data.repository.HevyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class HistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val workouts: List<Workout> = emptyList(),
    val workoutStats: WorkoutStats? = null,
    val recentPRs: List<Set> = emptyList(),
    val workoutCount: Int = 0,
    val volumeTrend: VolumeTrend? = null,
    val weeklyVolumeData: List<WeeklyVolumeData> = emptyList(),
    val durationTrend: List<DailyDurationData> = emptyList(),
    val muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
    val prsWithDetails: List<PRDetails> = emptyList()
)

class HistoryViewModel(
    private val repository: HevyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
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
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Load all workouts
                repository.getWorkouts()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load workouts: ${e.message}",
                            isLoading = false
                        )
                    }
                    .collect { workouts ->
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
                            var prs = emptyList<com.example.hevyinsight.data.model.Set>()
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
                                isLoading = false,
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
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Error processing data: ${e.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load history data: ${e.message}"
                )
            }
        }
    }
    
    fun refresh() {
        loadHistoryData()
    }
}

