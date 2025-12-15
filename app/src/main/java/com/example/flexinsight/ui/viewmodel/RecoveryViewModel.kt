package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.data.ai.FlexAIClient
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.common.UiError
import com.example.flexinsight.ui.utils.safeLaunch
import com.example.flexinsight.ui.utils.toApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class RecoveryUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val error: UiError? = null,
    val moodValue: Float = 7.5f,
    val notesText: String = "",
    val recoveryScore: Int = 85, // Default/Placeholder
    val trainingLoadStatus: TrainingLoadStatus = TrainingLoadStatus.Optimal,
    val sleepHours: Double = 7.5,
    val sorenessLevel: String = "Low",
    val aiInsight: String? = null,
    val isGeneratingInsight: Boolean = false
)

enum class TrainingLoadStatus {
    Low, Optimal, High
}


@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val repository: FlexRepository,
    private val aiClient: FlexAIClient
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

            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Success,
                trainingLoadStatus = loadStatus,
                error = null
            )
            
            // Trigger AI Insight if available
            generateRecoveryInsight(loadStatus, volumeTrend?.currentVolume ?: 0.0)
        }
    }

    private suspend fun generateRecoveryInsight(status: TrainingLoadStatus, currentVolume: Double) {
        if (!aiClient.isAvailable()) return

        _uiState.value = _uiState.value.copy(isGeneratingInsight = true)

        val prompt = "My training load status is $status with a current volume of $currentVolume kg. " +
                "My reported mood is ${_uiState.value.moodValue}/10. " +
                "Provide a short, 2-sentence recovery recommendation."

        val result = aiClient.generateResponse(prompt)

        _uiState.value = _uiState.value.copy(isGeneratingInsight = false)
        
        if (result is com.example.flexinsight.core.errors.Result.Success) {
            _uiState.value = _uiState.value.copy(aiInsight = result.data)
        }
    }

    fun updateMood(value: Float) {
        _uiState.value = _uiState.value.copy(moodValue = value)
    }

    fun updateNotes(text: String) {
        _uiState.value = _uiState.value.copy(notesText = text)
    }
}
