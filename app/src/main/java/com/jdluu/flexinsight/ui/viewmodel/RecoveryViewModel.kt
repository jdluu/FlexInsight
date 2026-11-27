package com.jdluu.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.flexinsight.core.dispatchers.DefaultDispatcherProvider
import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import com.jdluu.flexinsight.core.errors.ErrorHandler
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.ai.FlexAIClient
import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.health.HealthConnectSnapshot
import com.jdluu.flexinsight.data.model.MuscleGroup
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.domain.calc.RecoveryScoreCalculator
import com.jdluu.flexinsight.domain.model.DeloadAlert
import com.jdluu.flexinsight.domain.model.TrainingLoadScore
import com.jdluu.flexinsight.domain.usecase.CalculateTrainingLoadUseCase
import com.jdluu.flexinsight.domain.usecase.DetectDeloadUseCase
import com.jdluu.flexinsight.domain.usecase.GetMuscleRecoveryUseCase
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecoveryUiState(
    val loadingState: LoadingState = LoadingState.Loading,
    val error: UiError? = null,
    val muscleRecovery: Map<MuscleGroup, Float> = emptyMap(),
    val overallRecoveryScore: Int = 0,
    val selectedMuscle: MuscleGroup? = null,
    val trainingLoad: TrainingLoadScore? = null,
    val deloadAlert: DeloadAlert? = null,
    val healthSnapshot: HealthConnectSnapshot? = null,
    val healthConnectEnabled: Boolean = false,
    val moodRating: Float = 7.5f,
    val sorenessNotes: String = "",
    val aiInsight: String? = null,
    val isGeneratingInsight: Boolean = false
) {
    val isLoading: Boolean
        get() = loadingState.isLoading
}

@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val getMuscleRecoveryUseCase: GetMuscleRecoveryUseCase,
    private val calculateTrainingLoadUseCase: CalculateTrainingLoadUseCase,
    private val detectDeloadUseCase: DetectDeloadUseCase,
    private val healthConnectRepository: HealthConnectRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val aiClient: FlexAIClient,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoveryUiState())
    val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()
    private val prefsWriteChannel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private var moodEditedLocally = false
    private var notesEditedLocally = false

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            for (write in prefsWriteChannel) {
                write()
            }
        }
        viewModelScope.launch {
            userPreferencesManager.recoveryMoodFlow.collect { mood ->
                if (!moodEditedLocally) {
                    _uiState.update { it.copy(moodRating = mood) }
                }
            }
        }
        viewModelScope.launch {
            userPreferencesManager.recoveryNotesFlow.collect { notes ->
                if (!notesEditedLocally) {
                    _uiState.update { it.copy(sorenessNotes = notes) }
                }
            }
        }
        viewModelScope.launch {
            userPreferencesManager.healthConnectEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(healthConnectEnabled = enabled) }
            }
        }
        loadRecoveryData()
    }

    fun loadRecoveryData() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingState = LoadingState.Loading, error = null) }
            try {
                coroutineScope {
                    val recoveryDeferred = async {
                        runCatching { getMuscleRecoveryUseCase() }.getOrDefault(emptyMap())
                    }
                    val loadDeferred = async {
                        runCatching { calculateTrainingLoadUseCase() }.getOrNull()
                    }
                    val deloadDeferred = async {
                        runCatching { detectDeloadUseCase() }.getOrNull()
                    }
                    val healthDeferred = async {
                        runCatching { healthConnectRepository.readSnapshot() }.getOrNull()
                    }

                    val recovery = recoveryDeferred.await()
                    val load = loadDeferred.await()
                    val deload = deloadDeferred.await()
                    val health = healthDeferred.await()
                    val score = RecoveryScoreCalculator.overallRecoveryScore(recovery)

                    _uiState.update {
                        it.copy(
                            loadingState = LoadingState.Success,
                            muscleRecovery = recovery,
                            overallRecoveryScore = score,
                            trainingLoad = load,
                            deloadAlert = deload,
                            healthSnapshot = health,
                            error = null
                        )
                    }
                    generateRecoveryInsight(score, recovery, load, health)
                }
            } catch (e: Exception) {
                val apiError = ErrorHandler.handleError(e)
                _uiState.update {
                    it.copy(
                        loadingState = LoadingState.Error(apiError),
                        error = UiError.fromApiError(apiError)
                    )
                }
            }
        }
    }

    fun selectMuscle(group: MuscleGroup?) {
        _uiState.update { state ->
            state.copy(selectedMuscle = if (state.selectedMuscle == group) null else group)
        }
    }

    fun updateMood(rating: Float) {
        moodEditedLocally = true
        val coerced = rating.coerceIn(0f, 10f)
        _uiState.update { it.copy(moodRating = coerced) }
        prefsWriteChannel.trySend {
            userPreferencesManager.setRecoveryMood(coerced)
        }
    }

    fun updateNotes(notes: String) {
        notesEditedLocally = true
        _uiState.update { it.copy(sorenessNotes = notes) }
        prefsWriteChannel.trySend {
            userPreferencesManager.setRecoveryNotes(notes)
        }
    }

    fun refresh() {
        loadRecoveryData()
    }

    private suspend fun generateRecoveryInsight(
        overallScore: Int,
        recovery: Map<MuscleGroup, Float>,
        load: TrainingLoadScore?,
        health: HealthConnectSnapshot?
    ) {
        if (!aiClient.isAvailable() || _uiState.value.aiInsight != null) return
        _uiState.update { it.copy(isGeneratingInsight = true) }

        val fatigued = recovery.filterValues { it < 0.70f }.keys.joinToString { it.displayName }
        val sleepNote = health?.sleepHoursLastNight?.let { String.format(java.util.Locale.US, "%.1fh sleep", it) }
        val mood = _uiState.value.moodRating
        val prompt = buildString {
            append("Give 1 concise, practical training recovery recommendation (max 2 sentences). ")
            append("Overall readiness: $overallScore%. ")
            if (fatigued.isNotBlank()) append("Fatigued groups: $fatigued. ")
            load?.let { append("Weekly training load: ${it.overall}/100 (${it.label}). ") }
            sleepNote?.let { append("Last night: $it. ") }
            append("Subjective readiness: ${mood.toInt()}/10.")
        }

        val result = aiClient.generateResponse(prompt)
        _uiState.update { it.copy(isGeneratingInsight = false) }
        if (result is Result.Success) {
            _uiState.update { it.copy(aiInsight = result.data) }
        }
    }
}
