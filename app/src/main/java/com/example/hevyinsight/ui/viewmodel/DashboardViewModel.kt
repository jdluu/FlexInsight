package com.example.hevyinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hevyinsight.data.model.Workout
import com.example.hevyinsight.data.model.WorkoutStats
import com.example.hevyinsight.data.model.WeeklyProgress
import com.example.hevyinsight.data.model.SingleWorkoutStats
import com.example.hevyinsight.data.repository.HevyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val latestWorkout: Workout? = null,
    val latestWorkoutStats: SingleWorkoutStats? = null,
    val workoutStats: WorkoutStats? = null,
    val weeklyProgress: List<WeeklyProgress> = emptyList(),
    val currentStreak: Int = 0,
    val muscleGroupProgress: List<com.example.hevyinsight.data.model.MuscleGroupProgress> = emptyList()
)

class DashboardViewModel(
    private val repository: HevyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        // Delay initialization slightly to ensure database is ready
        viewModelScope.launch {
            delay(100) // Small delay to ensure database is initialized
            loadDashboardData()
        }
    }
    
    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Load latest workout
                repository.getRecentWorkouts(limit = 1)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load workouts: ${e.message}",
                            isLoading = false
                        )
                    }
                    .collect { workouts ->
                        try {
                            val latestWorkout = workouts.firstOrNull()
                            
                            // Calculate latest workout stats
                            val latestWorkoutStats = latestWorkout?.let {
                                try {
                                    repository.calculateWorkoutStats(it)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
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
                            
                            // Load weekly progress
                            var weeklyProgress = emptyList<com.example.hevyinsight.data.model.WeeklyProgress>()
                            try {
                                weeklyProgress = repository.getWeeklyProgress(weeks = 4)
                            } catch (e: Exception) {
                                // Continue with empty progress if it fails
                            }
                            
                            // Load muscle group progress
                            var muscleGroupProgress = emptyList<com.example.hevyinsight.data.model.MuscleGroupProgress>()
                            try {
                                muscleGroupProgress = repository.getMuscleGroupProgress(weeks = 4)
                            } catch (e: Exception) {
                                // Continue with empty progress if it fails
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                latestWorkout = latestWorkout,
                                latestWorkoutStats = latestWorkoutStats,
                                workoutStats = stats,
                                weeklyProgress = weeklyProgress,
                                currentStreak = stats.currentStreak,
                                muscleGroupProgress = muscleGroupProgress,
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
                    error = "Failed to load dashboard data: ${e.message}"
                )
            }
        }
    }
    
    fun refresh() {
        loadDashboardData()
    }
}

