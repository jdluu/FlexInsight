package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.core.errors.ErrorHandler
import com.example.flexinsight.data.model.*
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class PlannerUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val error: UiError? = null,
    val weeklyGoalProgress: WeeklyGoalProgress? = null,
    val weekCalendarData: List<DayInfo> = emptyList(),
    val selectedDayIndex: Int = 0,
    val selectedDayWorkouts: List<PlannedWorkout> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val volumeBalance: VolumeBalance? = null,
    val muscleGroupProgress: List<MuscleGroupProgress> = emptyList()
) {
    // Backward compatibility helper
    val isLoading: Boolean
        get() = loadingState.isLoading
}

class PlannerViewModel(
    private val repository: FlexRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlannerUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            delay(100) // Small delay to ensure database is initialized
            loadPlannerData()
        }
    }
    
    fun loadPlannerData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(loadingState = LoadingState.Loading, error = null)
                
                // Load weekly goal progress
                var weeklyGoalProgress: WeeklyGoalProgress? = null
                try {
                    weeklyGoalProgress = repository.getWeeklyGoalProgress(target = 5)
                } catch (e: Exception) {
                    // Continue with null if it fails
                }
                
                // Load week calendar data
                var weekCalendarData = emptyList<DayInfo>()
                try {
                    weekCalendarData = repository.getWeekCalendarData()
                } catch (e: Exception) {
                    // Continue with empty list if it fails
                }
                
                // Load routines
                var routines = emptyList<Routine>()
                try {
                    routines = repository.getRoutines().first()
                } catch (e: Exception) {
                    // Continue with empty list if it fails
                }
                
                // Load volume balance
                var volumeBalance: VolumeBalance? = null
                try {
                    volumeBalance = repository.getVolumeBalance(weeks = 4)
                } catch (e: Exception) {
                    // Continue with null if it fails
                }
                
                // Load muscle group progress for recommendations
                var muscleGroupProgress = emptyList<MuscleGroupProgress>()
                try {
                    muscleGroupProgress = repository.getMuscleGroupProgress(weeks = 4)
                } catch (e: Exception) {
                    // Continue with empty list if it fails
                }
                
                // Load workouts for selected day (default to first day)
                var selectedDayWorkouts = emptyList<PlannedWorkout>()
                val selectedDayIndex = _uiState.value.selectedDayIndex
                if (weekCalendarData.isNotEmpty() && selectedDayIndex < weekCalendarData.size) {
                    try {
                        val selectedDay = weekCalendarData[selectedDayIndex]
                        selectedDayWorkouts = repository.getPlannedWorkoutsForDay(selectedDay.timestamp)
                    } catch (e: Exception) {
                        // Continue with empty list if it fails
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    loadingState = LoadingState.Success,
                    weeklyGoalProgress = weeklyGoalProgress,
                    weekCalendarData = weekCalendarData,
                    selectedDayWorkouts = selectedDayWorkouts,
                    routines = routines,
                    volumeBalance = volumeBalance,
                    muscleGroupProgress = muscleGroupProgress,
                    error = null
                )
            } catch (e: Exception) {
                val apiError = ErrorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    loadingState = LoadingState.Error(apiError),
                    error = UiError.fromApiError(apiError)
                )
            }
        }
    }
    
    fun selectDay(dayIndex: Int) {
        viewModelScope.launch {
            try {
                val weekCalendarData = _uiState.value.weekCalendarData
                if (dayIndex >= 0 && dayIndex < weekCalendarData.size) {
                    val selectedDay = weekCalendarData[dayIndex]
                    val workouts = repository.getPlannedWorkoutsForDay(selectedDay.timestamp)
                    
                    _uiState.value = _uiState.value.copy(
                        selectedDayIndex = dayIndex,
                        selectedDayWorkouts = workouts
                    )
                }
            } catch (e: Exception) {
                val apiError = ErrorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    error = UiError.fromApiError(apiError)
                )
            }
        }
    }
    
    fun refresh() {
        loadPlannerData()
    }
}

