package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.data.model.CreateRoutineBody
import com.example.flexinsight.data.model.CreateRoutineExercise
import com.example.flexinsight.data.model.CreateRoutineRequest
import com.example.flexinsight.data.model.CreateRoutineSet
import com.example.flexinsight.data.model.DayInfo
import com.example.flexinsight.data.model.MuscleGroupProgress
import com.example.flexinsight.data.model.PlannedWorkout
import com.example.flexinsight.data.model.Routine
import com.example.flexinsight.data.model.RoutineFolder
import com.example.flexinsight.data.model.VolumeBalance
import com.example.flexinsight.data.model.WeeklyGoalProgress
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.data.repository.RoutineRepository
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.flexinsight.ui.utils.safeLaunch
import com.example.flexinsight.data.ai.FlexAIClient
import com.example.flexinsight.core.errors.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Represents the save-to-Hevy operation state.
 */
sealed interface SaveToHevyStatus {
    data object Idle : SaveToHevyStatus
    data object Saving : SaveToHevyStatus
    data class Success(val routineId: String) : SaveToHevyStatus
    data class Error(val message: String) : SaveToHevyStatus
}

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
    val isGeneratingPlan: Boolean = false,
    val saveToHevyStatus: SaveToHevyStatus = SaveToHevyStatus.Idle
) {
    val isLoading: Boolean
        get() = loadingState.isLoading
}


@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val repository: FlexRepository,
    private val routineRepository: RoutineRepository,
    private val aiClient: FlexAIClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(100)
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
            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Loading,
                error = null
            )

            val weeklyGoalProgress = runCatching { repository.getWeeklyGoalProgress() }
                .getOrNull()
            val weekCalendarData = runCatching { repository.getWeekCalendarData() }
                .getOrDefault(emptyList())
            val routinesRequest = runCatching { repository.getRoutines().first() }
                .getOrDefault(emptyList())
            val routineFolders = runCatching { repository.getRoutineFolders() }
                .getOrDefault(emptyList())
            val volumeBalance = runCatching { repository.getVolumeBalance(weeks = 4) }
                .getOrNull()
            val muscleGroupProgress = runCatching { repository.getMuscleGroupProgress(weeks = 4) }
                .getOrDefault(emptyList())

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
                selectedDayWorkouts = runCatching {
                    val selectedDay = weekCalendarData[selectedDayIndex]
                    repository.getPlannedWorkoutsForDay(selectedDay.timestamp)
                }.getOrDefault(emptyList())
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
            if (dayIndex in weekCalendarData.indices) {
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

            if (result is Result.Error) {
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
            if (result is Result.Success) {
                loadPlannerData()
            } else if (result is Result.Error) {
                _uiState.value = _uiState.value.copy(
                    error = UiError.fromApiError(result.error)
                )
            }
        }
    }

    /**
     * Generates an AI workout plan based on the user's current volume balance.
     */
    fun generateAIWorkout() {
        viewModelScope.launch {
            if (!aiClient.isAvailable()) {
                _uiState.value = _uiState.value.copy(
                    aiPlan = "AI features are not available on this device."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isGeneratingPlan = true, aiPlan = null)

            val balance = _uiState.value.volumeBalance
            val focus = if (balance != null) {
                "My volume balance is: " +
                        "Push=${(balance.push * 100).toInt()}%, " +
                        "Pull=${(balance.pull * 100).toInt()}%, " +
                        "Legs=${(balance.legs * 100).toInt()}%. "
            } else ""

            val prompt = "Create a structured gym workout plan for today. " +
                    focus +
                    "I am an intermediate lifter. " +
                    "Format it clearly with Exercise, Sets, and Reps. " +
                    "Keep it under 6 exercises."

            val result = aiClient.generateWorkoutPlan(prompt)

            _uiState.value = _uiState.value.copy(isGeneratingPlan = false)

            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(aiPlan = result.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        aiPlan = "Failed to generate plan. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Pushes the current AI-generated plan to Hevy as a new routine.
     * Creates a placeholder routine with the given title. The Hevy API requires
     * at least one exercise; a placeholder entry is used since structured parsing
     * of free-text AI output is unreliable without a dedicated schema.
     *
     * @param title The user-provided title for the routine.
     */
    fun pushRoutineToHevy(title: String) {
        if (_uiState.value.aiPlan == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                saveToHevyStatus = SaveToHevyStatus.Saving
            )

            val request = CreateRoutineRequest(
                routine = CreateRoutineBody(
                    title = title,
                    exercises = listOf(
                        CreateRoutineExercise(
                            exerciseTemplateId = "0",
                            sets = listOf(
                                CreateRoutineSet(type = "normal", reps = 1)
                            )
                        )
                    )
                )
            )

            val result = routineRepository.createRoutine(request)

            _uiState.value = when (result) {
                is Result.Success -> _uiState.value.copy(
                    saveToHevyStatus = SaveToHevyStatus.Success(result.data)
                )
                is Result.Error -> _uiState.value.copy(
                    saveToHevyStatus = SaveToHevyStatus.Error(
                        result.error.message ?: "Failed to save routine"
                    )
                )
            }
        }
    }

    fun clearAIPlan() {
        _uiState.value = _uiState.value.copy(
            aiPlan = null,
            saveToHevyStatus = SaveToHevyStatus.Idle
        )
    }

    fun clearSaveStatus() {
        _uiState.value = _uiState.value.copy(
            saveToHevyStatus = SaveToHevyStatus.Idle
        )
    }
}
