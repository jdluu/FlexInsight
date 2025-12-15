package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.common.UiError
import com.example.flexinsight.ui.utils.safeLaunch
import com.example.flexinsight.ui.utils.toApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecoveryUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val error: UiError? = null,
    val moodValue: Float = 7.5f,
    val notesText: String = "",
    val recoveryScore: Int = 85, // Default/Placeholder
    val trainingLoadStatus: TrainingLoadStatus = TrainingLoadStatus.Optimal,
    val sleepHours: Double = 7.5,
    val sorenessLevel: String = "Low"
)

enum class TrainingLoadStatus {
    Low, Optimal, High
}

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val repository: FlexRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoveryUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

    init {
        loadRecoveryData()
    }

    private fun loadRecoveryData() {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Error(apiError),
                error = UiError.fromApiError(apiError)
            )
        }) {
            _uiState.value = _uiState.value.copy(loadingState = LoadingState.Loading, error = null)

            // Calculate Training Load based on volume trend
            // If current volume > 1.2 * previous, it's High
            // If current volume < 0.8 * previous, it's Low
            // Else Optimal
            val volumeTrend = try {
                 repository.calculateVolumeTrend(4)
            } catch (e: Exception) {
                 null
            }

            val loadStatus = if (volumeTrend != null) {
                val ratio = if (volumeTrend.previousVolume > 0) volumeTrend.currentVolume / volumeTrend.previousVolume else 1.0
                when {
                    ratio > 1.3 -> TrainingLoadStatus.High
                    ratio < 0.7 -> TrainingLoadStatus.Low
                    else -> TrainingLoadStatus.Optimal
                }
            } else {
                TrainingLoadStatus.Optimal
            }

            // For demo purposes, we can keep some hardcoded values like sleep/soreness
            // or fetch them if we had a sleep repo. We'll stick to calculating the Load.

            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Success,
                trainingLoadStatus = loadStatus,
                error = null
            )
        }
    }

    fun updateMood(value: Float) {
        _uiState.value = _uiState.value.copy(moodValue = value)
    }

    fun updateNotes(text: String) {
        _uiState.value = _uiState.value.copy(notesText = text)
    }
}
