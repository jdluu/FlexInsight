package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.data.ai.FlexAIClient
import com.example.flexinsight.core.errors.ErrorHandler
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.model.Exercise
import com.example.flexinsight.data.model.Set
import com.example.flexinsight.data.model.SingleWorkoutStats
import com.example.flexinsight.data.model.Workout
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.data.preferences.UserPreferencesManager
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.common.UiError
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
    val isGeneratingReflection: Boolean = false
) {
    // Backward compatibility helper
    val isLoading: Boolean
        get() = loadingState.isLoading
}

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val repository: FlexRepository,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
    private val userPreferencesManager: UserPreferencesManager,
    private val aiClient: FlexAIClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workoutId: String? = savedStateHandle["workoutId"]

    private val _uiState = MutableStateFlow(WorkoutDetailUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    init {
        // Collect units preference
        viewModelScope.launch {
            userPreferencesManager.unitsFlow.collect { units ->
                _uiState.value = _uiState.value.copy(units = units)
            }
        }

        if (workoutId != null) {
            loadWorkoutData(workoutId)
        } else {
            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Error(com.example.flexinsight.core.errors.ApiError.Unknown("No workout ID provided")),
                error = UiError.Unknown("No workout ID provided")
            )
        }
    }

    private fun loadWorkoutData(workoutId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(loadingState = LoadingState.Loading, error = null)

                // Load workout
                val workout = repository.getWorkoutByIdFlow(workoutId).first()

                if (workout == null) {
                    val apiError = com.example.flexinsight.core.errors.ApiError.Unknown("Workout not found")
                    _uiState.value = _uiState.value.copy(
                        loadingState = LoadingState.Error(apiError),
                        error = UiError.Unknown("Workout not found")
                    )
                    return@launch
                }

                // Calculate workout stats
                val workoutStats = try {
                    repository.calculateWorkoutStats(workout)
                } catch (e: Exception) {
                    null
                }

                // Load exercises
                val exercises = try {
                    exerciseDao.getExercisesByWorkoutIdFlow(workoutId).first()
                } catch (e: Exception) {
                    emptyList()
                }

                // Load sets for each exercise
                val exercisesWithSets = exercises.map { exercise ->
                    val sets = try {
                        setDao.getSetsByExerciseIdFlow(exercise.id).first()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    ExerciseWithSets(exercise, sets)
                }

                _uiState.value = _uiState.value.copy(
                    loadingState = LoadingState.Success,
                    workout = workout,
                    workoutStats = workoutStats,
                    exercisesWithSets = exercisesWithSets,
                    error = null
                )
                
                // Trigger analysis
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

        // Check if we already have analysis to avoid re-generating (optional optimization)
        if (_uiState.value.aiReflection != null) return

        _uiState.value = _uiState.value.copy(isGeneratingReflection = true)

        // Construct prompt
        val durationMinutes = workout.endTime?.let { end ->
            java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(end - workout.startTime)
        } ?: 0L

        val prompt = "Analyze this workout: '${workout.name ?: "Workout"}'. " +
                "Duration: $durationMinutes mins. " +
                "Exercises: ${exercises.joinToString("; ") { "${it.exercise.name} (${it.sets.size} sets)" }}. " +
                "Provide a 3-bullet point 'Coach Reflection' on intensity and volume. be encouraging."

        val result = aiClient.generateResponse(prompt)

        _uiState.value = _uiState.value.copy(isGeneratingReflection = false)

        if (result is com.example.flexinsight.core.errors.Result.Success) {
            _uiState.value = _uiState.value.copy(aiReflection = result.data)
        }
    }

    fun refresh() {
        val workoutId = _uiState.value.workout?.id
        if (workoutId != null) {
            loadWorkoutData(workoutId)
        }
    }
}

