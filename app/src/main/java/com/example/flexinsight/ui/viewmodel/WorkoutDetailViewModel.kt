package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.core.errors.ErrorHandler
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.model.Exercise
import com.example.flexinsight.data.model.Set
import com.example.flexinsight.data.model.SingleWorkoutStats
import com.example.flexinsight.data.model.Workout
import com.example.flexinsight.data.repository.FlexRepository
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
    val exercisesWithSets: List<ExerciseWithSets> = emptyList()
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workoutId: String? = savedStateHandle["workoutId"]

    private val _uiState = MutableStateFlow(WorkoutDetailUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    init {
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
        val workoutId = _uiState.value.workout?.id
        if (workoutId != null) {
            loadWorkoutData(workoutId)
        }
    }
}

