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
import com.example.flexinsight.ui.utils.safeLaunch
import com.example.flexinsight.ui.utils.toApiError

data class PlannerUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val error: UiError? = null,
    val weeklyGoalProgress: WeeklyGoalProgress? = null,
    val weekCalendarData: List<DayInfo> = emptyList(),
    val selectedDayIndex: Int = 0,
    val selectedDayWorkouts: List<PlannedWorkout> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val routineFolders: List<RoutineFolder> = emptyList(),
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
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Error(apiError),
                error = UiError.fromApiError(apiError)
            )
        }) {
            _uiState.value = _uiState.value.copy(loadingState = LoadingState.Loading, error = null)

            // Load individual components with separate try-catch blocks to prevent one failure from blocking everything
            // Note: In a real app we might use async/await to load in parallel, but here we keep it simple or follow existing flow

            // Load weekly goal progress
            val weeklyGoalProgress = try {
                repository.getWeeklyGoalProgress(target = 5)
            } catch (e: Exception) {
                null
            }

            // Load week calendar data
            val weekCalendarData = try {
                repository.getWeekCalendarData()
            } catch (e: Exception) {
                emptyList()
            }

            // Load routines
            val routinesRequest = try {
                repository.getRoutines().first()
            } catch (e: Exception) {
                emptyList()
            }

            // Load routine folders
            val routineFolders = try {
                repository.getRoutineFolders()
            } catch (e: Exception) {
                emptyList()
            }

            // Load volume balance
            val volumeBalance = try {
                repository.getVolumeBalance(weeks = 4)
            } catch (e: Exception) {
                null
            }

            // Load muscle group progress for recommendations
            val muscleGroupProgress = try {
                repository.getMuscleGroupProgress(weeks = 4)
            } catch (e: Exception) {
                emptyList()
            }

            // Load workouts for selected day (default to current day)
            var selectedDayWorkouts = emptyList<PlannedWorkout>()
            var selectedDayIndex = _uiState.value.selectedDayIndex

            // If this is the initial load (selectedDayIndex is 0), find today's index
            if (selectedDayIndex == 0 && weekCalendarData.isNotEmpty()) {
                val today = System.currentTimeMillis()
                val todayIndex = weekCalendarData.indexOfFirst { dayInfo ->
                    // Check if the day matches today (same day, ignoring time)
                    val dayStart = dayInfo.timestamp
                    val dayEnd = dayStart + 24 * 60 * 60 * 1000 // Add 24 hours
                    today >= dayStart && today < dayEnd
                }

                // If today is found in the week, use it; otherwise keep 0
                if (todayIndex >= 0) {
                    selectedDayIndex = todayIndex
                }
            }

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
                selectedDayIndex = selectedDayIndex,
                selectedDayWorkouts = selectedDayWorkouts,
                routines = routinesRequest,
                routineFolders = routineFolders,
                volumeBalance = volumeBalance,
                muscleGroupProgress = muscleGroupProgress,
                error = null
            )
        }
    }

    fun selectDay(dayIndex: Int) {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(error = UiError.fromApiError(apiError))
        }) {
            val weekCalendarData = _uiState.value.weekCalendarData
            if (dayIndex >= 0 && dayIndex < weekCalendarData.size) {
                val selectedDay = weekCalendarData[dayIndex]
                val workouts = repository.getPlannedWorkoutsForDay(selectedDay.timestamp)

                _uiState.value = _uiState.value.copy(
                    selectedDayIndex = dayIndex,
                    selectedDayWorkouts = workouts
                )
            }
        }
    }

    fun refresh() {
        loadPlannerData()
    }

    fun markWorkoutAsComplete(workoutId: String, isCompleted: Boolean) {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(error = UiError.fromApiError(apiError))
        }) {
            // Optimistic update
            val updatedWorkouts = _uiState.value.selectedDayWorkouts.map {
                if (it.id == workoutId) it.copy(isCompleted = isCompleted) else it
            }
            _uiState.value = _uiState.value.copy(selectedDayWorkouts = updatedWorkouts)

            val result = repository.updateWorkoutStatus(workoutId, isCompleted)

            if (result is com.example.flexinsight.core.errors.Result.Error) {
                // Revert on failure
                val revertedWorkouts = _uiState.value.selectedDayWorkouts.map {
                    if (it.id == workoutId) it.copy(isCompleted = !isCompleted) else it
                }
                _uiState.value = _uiState.value.copy(
                    selectedDayWorkouts = revertedWorkouts,
                    error = UiError.fromApiError(result.error)
                )
            } else {
                // Refresh data to update stats/calendar
                loadPlannerData()
            }
        }
    }

    fun rescheduleWorkout(workoutId: String, newDate: Long) {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(error = UiError.fromApiError(apiError))
        }) {
            val result = repository.rescheduleWorkout(workoutId, newDate)

            if (result is com.example.flexinsight.core.errors.Result.Success) {
                loadPlannerData()
            } else if (result is com.example.flexinsight.core.errors.Result.Error) {
                _uiState.value = _uiState.value.copy(
                    error = UiError.fromApiError(result.error)
                )
            }
        }
    }

    fun generateAIWorkout() {
        // AI implementation not yet available
        // Could show a toast or message in UI state if needed
    }
}

