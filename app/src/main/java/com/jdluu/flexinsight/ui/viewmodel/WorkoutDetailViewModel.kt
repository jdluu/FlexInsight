package com.jdluu.flexinsight.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.flexinsight.data.ai.FlexAIClient
import com.jdluu.flexinsight.domain.usecase.ExplainWorkoutUseCase
import com.jdluu.flexinsight.core.errors.ErrorHandler
import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.SingleWorkoutStats
import com.jdluu.flexinsight.data.model.Workout
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseWithSets(
    val exercise: Exercise,
    val sets: List<Set>,
    val muscleGroup: String? = null
)

data class WorkoutDetailUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val error: UiError? = null,
    val workout: Workout? = null,
    val workoutStats: SingleWorkoutStats? = null,
    val exercisesWithSets: List<ExerciseWithSets> = emptyList(),
    val units: String = "Imperial",
    val aiReflection: String? = null,
    val isGeneratingReflection: Boolean = false,
    val isExplainingWorkout: Boolean = false,
    val workoutExplanation: String? = null
) {
    val isLoading: Boolean
        get() = loadingState.isLoading
}

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val repository: FlexRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val aiClient: FlexAIClient,
    private val explainWorkoutUseCase: ExplainWorkoutUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workoutId: String? = savedStateHandle["workoutId"]

    private val _uiState = MutableStateFlow(WorkoutDetailUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesManager.unitsFlow.collect { units ->
                _uiState.value = _uiState.value.copy(units = units)
            }
        }

        if (workoutId != null) {
            loadWorkoutData(workoutId)
        } else {
            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Error(com.jdluu.flexinsight.core.errors.ApiError.Unknown("No workout ID provided")),
                error = UiError.Unknown("No workout ID provided")
            )
        }
    }

    private fun loadWorkoutData(workoutId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(loadingState = LoadingState.Loading, error = null)

                val workout = repository.getWorkoutByIdFlow(workoutId).first()

                if (workout == null) {
                    _uiState.value = _uiState.value.copy(
                        loadingState = LoadingState.Error(com.jdluu.flexinsight.core.errors.ApiError.Unknown("Workout not found")),
                        error = UiError.Unknown("Workout not found")
                    )
                    return@launch
                }

                val workoutStats = runCatching { repository.calculateWorkoutStats(workout) }.getOrNull()

                val exercises = repository.getExercisesByWorkoutId(workoutId)
                val exercisesWithSets = exercises.map { exercise ->
                    val sets = repository.getSetsByExerciseId(exercise.id)
                    ExerciseWithSets(exercise, sets)
                }

                _uiState.value = _uiState.value.copy(
                    loadingState = LoadingState.Success,
                    workout = workout,
                    workoutStats = workoutStats,
                    exercisesWithSets = exercisesWithSets,
                    error = null
                )

                generateWorkoutAnalysis(workout, exercisesWithSets)
            } catch (e: Exception) {
                val apiError = ErrorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    loadingState = LoadingState.Error(apiError),
                    error = UiError.fromApiError(apiError)
                )
            }
        }
    }

    private suspend fun generateWorkoutAnalysis(workout: Workout, exercises: List<ExerciseWithSets>) {
        if (!aiClient.isAvailable()) return
        if (_uiState.value.aiReflection != null) return

        _uiState.value = _uiState.value.copy(isGeneratingReflection = true)

        val durationMinutes = workout.endTime?.let { end ->
            java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(end - workout.startTime)
        } ?: 0L

        val prompt = "Analyze this workout: '${workout.name ?: "Workout"}'. " +
                "Duration: $durationMinutes mins. " +
                "Exercises: ${exercises.joinToString("; ") { "${it.exercise.name} (${it.sets.size} sets)" }}. " +
                "Provide a 3-bullet point 'Coach Reflection' on intensity and volume. be encouraging."

        val result = aiClient.generateResponse(prompt)

        _uiState.value = _uiState.value.copy(isGeneratingReflection = false)

        if (result is com.jdluu.flexinsight.core.errors.Result.Success) {
            _uiState.value = _uiState.value.copy(aiReflection = result.data)
        }
    }

    fun refresh() {
        val id = _uiState.value.workout?.id
        if (id != null) loadWorkoutData(id)
    }

    fun explainWorkout() {
        val id = _uiState.value.workout?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExplainingWorkout = true, workoutExplanation = null)
            when (val result = explainWorkoutUseCase(id)) {
                is com.jdluu.flexinsight.core.errors.Result.Success ->
                    _uiState.value = _uiState.value.copy(
                        workoutExplanation = result.data,
                        isExplainingWorkout = false
                    )
                is com.jdluu.flexinsight.core.errors.Result.Error ->
                    _uiState.value = _uiState.value.copy(
                        workoutExplanation = result.error.message ?: "Could not explain workout",
                        isExplainingWorkout = false
                    )
            }
        }
    }
}
