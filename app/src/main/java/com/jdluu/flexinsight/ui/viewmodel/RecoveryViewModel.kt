package com.jdluu.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.ai.FlexAIClient
import com.jdluu.flexinsight.data.model.MuscleGroup
import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.domain.usecase.CalculateTrainingLoadUseCase
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import com.jdluu.flexinsight.ui.utils.safeLaunch
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
    val recoveryScore: Int = 0,
    val trainingLoadStatus: TrainingLoadStatus = TrainingLoadStatus.Optimal,
    val sorenessLevel: String = "Unknown",
    val aiInsight: String? = null,
    val isGeneratingInsight: Boolean = false,
    val muscleRecovery: Map<MuscleGroup, Float> = emptyMap(),
    val sleepHours: Double? = null,
    val restingHeartRate: Long? = null,
    val stepsToday: Long? = null
) {
    val isLoading: Boolean get() = loadingState.isLoading
}

enum class TrainingLoadStatus {
    Low, Optimal, High
}


@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val repository: FlexRepository,
    private val aiClient: FlexAIClient,
    private val healthConnectRepository: HealthConnectRepository,
    private val calculateTrainingLoadUseCase: CalculateTrainingLoadUseCase,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoveryUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesManager.recoveryMoodFlow.collect { mood ->
                _uiState.value = _uiState.value.copy(moodValue = mood)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.recoveryNotesFlow.collect { notes ->
                _uiState.value = _uiState.value.copy(notesText = notes)
            }
        }
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
            val health = runCatching { healthConnectRepository.readSnapshot() }.getOrNull()
            val trainingLoad = runCatching { calculateTrainingLoadUseCase() }.getOrNull()
            val adjustedLoadStatus = when {
                health?.sleepHoursLastNight != null && health.sleepHoursLastNight < 5.5 -> TrainingLoadStatus.High
                trainingLoad != null && trainingLoad.overall >= 80 -> TrainingLoadStatus.High
                trainingLoad != null && trainingLoad.overall < 45 -> TrainingLoadStatus.Low
                else -> loadStatus
            }
            val recoveryScore = calculateRecoveryScore(
                muscleRecovery = muscleRecovery,
                loadStatus = adjustedLoadStatus,
                mood = _uiState.value.moodValue,
                sleepHours = health?.sleepHoursLastNight
            )

            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Success,
                trainingLoadStatus = adjustedLoadStatus,
                muscleRecovery = muscleRecovery,
                recoveryScore = recoveryScore,
                sorenessLevel = sorenessLevel,
                sleepHours = health?.sleepHoursLastNight,
                restingHeartRate = health?.restingHeartRateBpm,
                stepsToday = health?.stepsToday,
                error = null
            )

            generateRecoveryInsight(adjustedLoadStatus, volumeTrend?.currentVolume ?: 0.0, health)
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
        mood: Float,
        sleepHours: Double? = null
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
        val sleepComponent = when (sleepHours) {
            null -> 0.75f
            in 7.0..Double.MAX_VALUE -> 1f
            in 6.0..7.0 -> 0.8f
            in 5.0..6.0 -> 0.55f
            else -> 0.35f
        }

        val score = (muscleComponent * 0.35f + loadComponent * 0.25f +
            moodComponent * 0.2f + sleepComponent * 0.2f) * 100
        return score.toInt().coerceIn(0, 100)
    }

    private suspend fun generateRecoveryInsight(
        status: TrainingLoadStatus,
        currentVolume: Double,
        health: com.jdluu.flexinsight.data.health.HealthConnectSnapshot?
    ) {
        if (!aiClient.isAvailable()) return

        _uiState.value = _uiState.value.copy(isGeneratingInsight = true)

        val healthLine = health?.let {
            buildString {
                it.sleepHoursLastNight?.let { h -> append("Sleep: ${"%.1f".format(h)}h. ") }
                it.restingHeartRateBpm?.let { hr -> append("Resting HR: $hr bpm. ") }
                it.stepsToday?.let { s -> append("Steps today: $s. ") }
            }
        } ?: ""

        val prompt = "My training load status is $status with a current volume of $currentVolume kg. " +
                healthLine +
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
        viewModelScope.launch { userPreferencesManager.setRecoveryMood(value) }
    }

    fun updateNotes(text: String) {
        _uiState.value = _uiState.value.copy(notesText = text)
        viewModelScope.launch { userPreferencesManager.setRecoveryNotes(text) }
    }
}
