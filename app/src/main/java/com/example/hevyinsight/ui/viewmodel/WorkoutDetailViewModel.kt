package com.example.hevyinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hevyinsight.core.errors.ErrorHandler
import com.example.hevyinsight.data.local.HevyDatabase
import com.example.hevyinsight.data.model.Exercise
import com.example.hevyinsight.data.model.Set
import com.example.hevyinsight.data.model.SingleWorkoutStats
import com.example.hevyinsight.data.model.Workout
import com.example.hevyinsight.data.repository.HevyRepository
import com.example.hevyinsight.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ExerciseWithSets(
    val exercise: Exercise,
    val sets: List<Set>
)

data class WorkoutDetailUiState(
    val isLoading: Boolean = false,
    val error: UiError? = null,
    val workout: Workout? = null,
    val workoutStats: SingleWorkoutStats? = null,
    val exercisesWithSets: List<ExerciseWithSets> = emptyList()
)

class WorkoutDetailViewModel(
    private val repository: HevyRepository,
    private val database: HevyDatabase,
    workoutId: String?
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkoutDetailUiState(isLoading = true))
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()
    
    private val exerciseDao = database.exerciseDao()
    private val setDao = database.setDao()
    
    init {
        if (workoutId != null) {
            loadWorkoutData(workoutId)
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "No workout ID provided"
            )
        }
    }
    
    private fun loadWorkoutData(workoutId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Load workout
                val workout = repository.getWorkoutByIdFlow(workoutId).first()
                
                if (workout == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
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
                    isLoading = false,
                    workout = workout,
                    workoutStats = workoutStats,
                    exercisesWithSets = exercisesWithSets,
                    error = null
                )
            } catch (e: Exception) {
                val apiError = ErrorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
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

