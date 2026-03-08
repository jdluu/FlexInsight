package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.data.ai.FlexAIClient
import com.example.flexinsight.data.model.MuscleGroup
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.common.UiError
import com.example.flexinsight.ui.utils.safeLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class RecoveryUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val error: UiError? = null,
    val moodValue: Float = 7.5f,
    val notesText: String = "",
    val recoveryScore: Int = 0,
    val trainingLoadStatus: TrainingLoadStatus = TrainingLoadStatus.Optimal,
    val sorenessLevel: String = "Unknown",
    val aiInsight: String? = null,
    val isGeneratingInsight: Boolean = false,
    val muscleRecovery: Map<MuscleGroup, Float> = emptyMap()
) {
    val isLoading: Boolean get() = loadingState.isLoading
}

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
            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Loading,
                error = null
            )

            val volumeTrend = runCatching { repository.calculateVolumeTrend(4) }
                .getOrNull()

            val loadStatus = if (volumeTrend != null) {
                val ratio = if (volumeTrend.previousVolume > 0) {
                    volumeTrend.currentVolume / volumeTrend.previousVolume
                } else 1.0
                when {
                    ratio > 1.3 -> TrainingLoadStatus.High
                    ratio < 0.7 -> TrainingLoadStatus.Low
                    else -> TrainingLoadStatus.Optimal
                }
            } else {
                TrainingLoadStatus.Optimal
            }

            val muscleRecovery = runCatching { repository.getMuscleRecoveryStatus() }
                .getOrDefault(emptyMap())

            val sorenessLevel = deriveSorenessLevel(muscleRecovery)
            val recoveryScore = calculateRecoveryScore(
                muscleRecovery = muscleRecovery,
                loadStatus = loadStatus,
                mood = _uiState.value.moodValue
            )

            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Success,
                trainingLoadStatus = loadStatus,
                muscleRecovery = muscleRecovery,
                recoveryScore = recoveryScore,
                sorenessLevel = sorenessLevel,
                error = null
            )

            generateRecoveryInsight(loadStatus, volumeTrend?.currentVolume ?: 0.0)
        }
    }

    /**
     * Derives soreness level from muscle recovery data.
     * Recovery values: 0.0 = fully fatigued, 1.0 = fully recovered.
     * Average recovery below 0.4 = High soreness, below 0.7 = Moderate, else Low.
     */
    private fun deriveSorenessLevel(muscleRecovery: Map<MuscleGroup, Float>): String {
        if (muscleRecovery.isEmpty()) return "Unknown"
        val avgRecovery = muscleRecovery.values.average()
        return when {
            avgRecovery < 0.4 -> "High"
            avgRecovery < 0.7 -> "Moderate"
            else -> "Low"
        }
    }

    /**
     * Calculates a composite recovery score (0-100) from multiple factors:
     * - Muscle recovery average (40% weight)
     * - Training load status (30% weight)
     * - User mood (30% weight)
     */
    private fun calculateRecoveryScore(
        muscleRecovery: Map<MuscleGroup, Float>,
        loadStatus: TrainingLoadStatus,
        mood: Float
    ): Int {
        val muscleComponent = if (muscleRecovery.isNotEmpty()) {
            muscleRecovery.values.average().toFloat()
        } else {
            0.75f
        }

        val loadComponent = when (loadStatus) {
            TrainingLoadStatus.Low -> 0.9f
            TrainingLoadStatus.Optimal -> 0.75f
            TrainingLoadStatus.High -> 0.4f
        }

        val moodComponent = mood / 10f

        val score = (muscleComponent * 0.4f + loadComponent * 0.3f + moodComponent * 0.3f) * 100
        return score.toInt().coerceIn(0, 100)
    }

    private suspend fun generateRecoveryInsight(
        status: TrainingLoadStatus,
        currentVolume: Double
    ) {
        if (!aiClient.isAvailable()) return

        _uiState.value = _uiState.value.copy(isGeneratingInsight = true)

        val prompt = "My training load status is $status with a current volume of $currentVolume kg. " +
                "My reported mood is ${_uiState.value.moodValue}/10. " +
                "My soreness level is ${_uiState.value.sorenessLevel}. " +
                "Recovery score is ${_uiState.value.recoveryScore}/100. " +
                "Provide a short, 2-sentence recovery recommendation."

        val result = aiClient.generateResponse(prompt)

        _uiState.value = _uiState.value.copy(isGeneratingInsight = false)

        if (result is Result.Success) {
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
