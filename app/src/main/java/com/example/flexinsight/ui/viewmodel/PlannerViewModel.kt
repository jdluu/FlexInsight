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
import com.example.flexinsight.data.ai.FlexAIClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

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
    val muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
    val aiPlan: String? = null,
    val isGeneratingPlan: Boolean = false
) {
    // Backward compatibility helper
    val isLoading: Boolean
        get() = loadingState.isLoading
}


@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val repository: FlexRepository,
    private val aiClient: FlexAIClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(100) // Small delay to ensure database is initialized
            loadPlannerData()
        }
    }

    // ... (existing helper methods if any, but we are replacing the whole class body so ensure we keep loadPlannerData etc. or just update specific parts. 
    // Wait, the instruction says "replace the entire ViewModel content" usually, but here I can target the constructor and the generateAIWorkout method + UiState data class.
    // However, since I need to inject the client in the constructor which is at the top, and add the method at the bottom, and update the data class, it's safer to do a few replaces or one big one.
    // Let's do one big replace to ensure consistency and avoid line number shifts.)
    
    // Actually, looking at the file content, I can replace the whole file content to be safe and clean.
    
    fun loadPlannerData() {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Error(apiError),
                error = UiError.fromApiError(apiError)
            )
        }) {
            _uiState.value = _uiState.value.copy(loadingState = LoadingState.Loading, error = null)

            // Load components (keeping existing logic)
            val weeklyGoalProgress = try { repository.getWeeklyGoalProgress(target = 5) } catch (e: Exception) { null }
            val weekCalendarData = try { repository.getWeekCalendarData() } catch (e: Exception) { emptyList() }
            val routinesRequest = try { repository.getRoutines().first() } catch (e: Exception) { emptyList() }
            val routineFolders = try { repository.getRoutineFolders() } catch (e: Exception) { emptyList() }
            val volumeBalance = try { repository.getVolumeBalance(weeks = 4) } catch (e: Exception) { null }
            val muscleGroupProgress = try { repository.getMuscleGroupProgress(weeks = 4) } catch (e: Exception) { emptyList() }

            var selectedDayWorkouts = emptyList<PlannedWorkout>()
            var selectedDayIndex = _uiState.value.selectedDayIndex

            if (selectedDayIndex == 0 && weekCalendarData.isNotEmpty()) {
                val today = System.currentTimeMillis()
                val todayIndex = weekCalendarData.indexOfFirst { dayInfo ->
                    val dayStart = dayInfo.timestamp
                    val dayEnd = dayStart + 24 * 60 * 60 * 1000
                    today in dayStart until dayEnd
                }
                if (todayIndex >= 0) selectedDayIndex = todayIndex
            }

            if (weekCalendarData.isNotEmpty() && selectedDayIndex < weekCalendarData.size) {
                try {
                    val selectedDay = weekCalendarData[selectedDayIndex]
                    selectedDayWorkouts = repository.getPlannedWorkoutsForDay(selectedDay.timestamp)
                } catch (e: Exception) { }
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
            val updatedWorkouts = _uiState.value.selectedDayWorkouts.map {
                if (it.id == workoutId) it.copy(isCompleted = isCompleted) else it
            }
            _uiState.value = _uiState.value.copy(selectedDayWorkouts = updatedWorkouts)

            val result = repository.updateWorkoutStatus(workoutId, isCompleted)

            if (result is com.example.flexinsight.core.errors.Result.Error) {
                val revertedWorkouts = _uiState.value.selectedDayWorkouts.map {
                    if (it.id == workoutId) it.copy(isCompleted = !isCompleted) else it
                }
                _uiState.value = _uiState.value.copy(
                    selectedDayWorkouts = revertedWorkouts,
                    error = UiError.fromApiError(result.error)
                )
            } else {
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
                _uiState.value = _uiState.value.copy(error = UiError.fromApiError(result.error))
            }
        }
    }

    fun generateAIWorkout() {
        viewModelScope.launch {
            if (!aiClient.isAvailable()) {
                _uiState.value = _uiState.value.copy(
                    aiPlan = "Sorry, AI features are not supported on this device."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isGeneratingPlan = true, aiPlan = null)

            // Construct specific prompt
            val balance = _uiState.value.volumeBalance
            val focus = if (balance != null) {
                "My volume balance is: Push=${(balance.push * 100).toInt()}%, Pull=${(balance.pull * 100).toInt()}%, Legs=${(balance.legs * 100).toInt()}%. "
            } else ""

            val prompt = "Create a structured gym workout plan for today. " +
                    focus +
                    "I am an intermediate lifter. " +
                    "Format it clearly with Exercise, Sets, and Reps. " +
                    "Keep it under 6 exercises."

            val result = aiClient.generateWorkoutPlan(prompt)

            _uiState.value = _uiState.value.copy(isGeneratingPlan = false)

            if (result is com.example.flexinsight.core.errors.Result.Success) {
                _uiState.value = _uiState.value.copy(aiPlan = result.data)
            } else {
                _uiState.value = _uiState.value.copy(aiPlan = "Failed to generate plan. Please try again.")
            }
        }
    }
    
    fun clearAIPlan() {
        _uiState.value = _uiState.value.copy(aiPlan = null)
    }
}

