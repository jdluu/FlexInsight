package com.jdluu.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.ai.AiFeatureStatus
import com.jdluu.flexinsight.data.ai.FlexAIClient
import com.jdluu.flexinsight.data.ai.HevyAiDataAccessor
import com.jdluu.flexinsight.data.preferences.ApiKeyStatusSource
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.data.sync.ManualSyncScheduler
import com.jdluu.flexinsight.data.sync.SyncCompleteListener
import com.jdluu.flexinsight.domain.usecase.BuildAiContextUseCase
import com.jdluu.flexinsight.ui.screens.aitrainer.parts.ChatMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class AITrainerUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val isAiAvailable: Boolean = true,
    val aiStatusMessage: String? = null,
    val isPreparingModel: Boolean = false,
    val isSyncingHevyData: Boolean = false,
    val hasHevyData: Boolean = false,
    val hevyWorkoutCount: Int = 0,
    val error: String? = null,
    val usesLiveExerciseHistory: Boolean = false,
    val isOfflineAi: Boolean = true
)

@HiltViewModel
class AITrainerViewModel @Inject constructor(
    private val aiClient: FlexAIClient,
    private val buildAiContextUseCase: BuildAiContextUseCase,
    private val flexRepository: FlexRepository,
    private val apiKeyManager: ApiKeyStatusSource,
    private val syncManager: ManualSyncScheduler,
    private val syncCoordinator: SyncCompleteListener
) : ViewModel() {

    private val _uiState = MutableStateFlow(AITrainerUiState())
    val uiState: StateFlow<AITrainerUiState> = _uiState.asStateFlow()

    private var systemContext: String? = null

    init {
        checkAiAvailability()
    }

    private fun checkAiAvailability() {
        viewModelScope.launch {
            val status = aiClient.getFeatureStatus()
            val statusMessage = status.toUserMessage()

            when (status) {
                AiFeatureStatus.Ready, AiFeatureStatus.Downloadable -> {
                    _uiState.value = _uiState.value.copy(
                        isPreparingModel = true,
                        aiStatusMessage = statusMessage
                    )
                    when (val prepared = aiClient.prepareModel()) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isAiAvailable = true,
                                isPreparingModel = false,
                                aiStatusMessage = null
                            )
                            syncHevyDataThenGreet()
                        }
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isAiAvailable = false,
                                isPreparingModel = false,
                                aiStatusMessage = prepared.error.message,
                                messages = listOf(
                                    ChatMessage(
                                        "ai",
                                        prepared.error.message
                                            ?: "On-device AI is not ready. Open the app again after Gemini Nano finishes downloading.",
                                        false
                                    )
                                )
                            )
                        }
                    }
                }
                AiFeatureStatus.Downloading -> {
                    _uiState.value = _uiState.value.copy(
                        isAiAvailable = false,
                        isPreparingModel = true,
                        aiStatusMessage = statusMessage,
                        messages = listOf(ChatMessage("ai", statusMessage, false))
                    )
                }
                AiFeatureStatus.Unavailable -> {
                    _uiState.value = _uiState.value.copy(
                        isAiAvailable = false,
                        isPreparingModel = false,
                        aiStatusMessage = statusMessage,
                        messages = listOf(ChatMessage("ai", statusMessage, false))
                    )
                }
            }
        }
    }

    private fun syncHevyDataThenGreet() {
        viewModelScope.launch {
            if (apiKeyManager.hasApiKey()) {
                _uiState.value = _uiState.value.copy(
                    isSyncingHevyData = true,
                    aiStatusMessage = "Syncing your Hevy workouts…"
                )
                val syncJob = async {
                    when (val result = flexRepository.syncAllData()) {
                        is Result.Success -> syncCoordinator.onSyncComplete()
                        is Result.Error ->
                            android.util.Log.w(
                                "AITrainerViewModel",
                                "Hevy sync before AI chat failed: ${result.error.message}"
                            )
                    }
                }
                syncJob.await()
                _uiState.value = _uiState.value.copy(isSyncingHevyData = false, aiStatusMessage = null)
            }
            loadDynamicContextAndGreeting()
        }
    }

    /** Rebuild Hevy context; pass [userQuery] to fetch live exercise history for that question. */
    private suspend fun refreshHevyContext(userQuery: String? = null): HevyAiDataAccessor.ContextSnapshot {
        return buildAiContextUseCase(userQuery)
    }

    private suspend fun loadDynamicContextAndGreeting() {
        _uiState.value = _uiState.value.copy(isTyping = true)

        val snapshot = try {
            refreshHevyContext()
        } catch (e: Exception) {
            android.util.Log.e("AITrainerViewModel", "Failed to build AI context", e)
            return@loadDynamicContextAndGreeting
        }

        systemContext = snapshot.text
        _uiState.value = _uiState.value.copy(
            hasHevyData = snapshot.hasWorkoutData,
            hevyWorkoutCount = snapshot.workoutCount,
            usesLiveExerciseHistory = snapshot.usesLiveExerciseHistory,
            isOfflineAi = true
        )

        val greetingPrompt = buildString {
            append(snapshot.text)
            append("\n\n")
            if (snapshot.hasWorkoutData) {
                append("Based on this user's real Hevy training data above, greet them by name and reference something specific from their recent training.")
            } else if (!snapshot.hasApiKey) {
                append("The user has not connected Hevy yet. Greet them and explain they need to add their Hevy API key in Settings for personalized coaching.")
            } else {
                append("Hevy is connected but no workouts are synced yet. Greet them and suggest syncing from Settings.")
            }
        }

        val result = aiClient.generateResponse(greetingPrompt)
        _uiState.value = _uiState.value.copy(isTyping = false)

        if (result is Result.Success) {
            addMessage(ChatMessage("ai", result.data, false))
        } else {
            val fallback = when {
                snapshot.hasWorkoutData -> "Hello! I've loaded your Hevy training data. What would you like to work on?"
                !snapshot.hasApiKey -> "Hello! Connect your Hevy API key in Settings so I can personalize advice to your workouts."
                else -> "Hello! Sync your Hevy workouts from Settings, then I can coach you using your real training history."
            }
            addMessage(ChatMessage("ai", fallback, false))
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || !_uiState.value.isAiAvailable) return

        addMessage(ChatMessage("user", text, true))
        _uiState.value = _uiState.value.copy(isTyping = true, error = null)

        viewModelScope.launch {
            val snapshot = try {
                refreshHevyContext(userQuery = text)
            } catch (e: Exception) {
                android.util.Log.e("AITrainerViewModel", "Failed to refresh context", e)
                null
            }

            snapshot?.let {
                systemContext = it.text
                _uiState.value = _uiState.value.copy(
                    hasHevyData = it.hasWorkoutData,
                    hevyWorkoutCount = it.workoutCount,
                    usesLiveExerciseHistory = it.usesLiveExerciseHistory
                )
            }

            val history = mutableListOf<Pair<String, String>>()
            systemContext?.let { ctx ->
                history.add("user" to "System Context:\n$ctx")
                history.add("model" to "Understood. I will coach using only this Hevy data and say when something is not available.")
            }

            _uiState.value.messages.forEach { msg ->
                val role = if (msg.sender == "user") "user" else "model"
                history.add(role to msg.text)
            }

            var currentMessageStarted = false
            val fullResponseBuilder = StringBuilder()

            try {
                aiClient.generateResponseStream(text, history).collect { chunk ->
                    if (chunk.startsWith("Error:")) {
                        _uiState.value = _uiState.value.copy(isTyping = false, error = chunk)
                    } else {
                        fullResponseBuilder.append(chunk)
                        val currentText = fullResponseBuilder.toString()

                        if (!currentMessageStarted) {
                            _uiState.value = _uiState.value.copy(isTyping = false)
                            addMessage(ChatMessage("ai", currentText, false))
                            currentMessageStarted = true
                        } else {
                            updateLastMessage(currentText)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AITrainerViewModel", "Failed to generate stream response", e)
                _uiState.value = _uiState.value.copy(isTyping = false, error = e.message)
                if (!currentMessageStarted) {
                    addMessage(
                        ChatMessage("ai", "I'm having trouble thinking right now. Please try again.", false)
                    )
                }
            } finally {
                _uiState.value = _uiState.value.copy(isTyping = false)
            }
        }
    }

    fun refreshHevySync() {
        viewModelScope.launch {
            if (!apiKeyManager.hasApiKey()) return@launch
            _uiState.value = _uiState.value.copy(isSyncingHevyData = true)
            syncManager.syncNow()
            when (val result = flexRepository.syncAllData()) {
                is Result.Success -> syncCoordinator.onSyncComplete()
                is Result.Error ->
                    android.util.Log.e(
                        "AITrainerViewModel",
                        "Manual Hevy sync failed: ${result.error.message}"
                    )
            }
            val snapshot = refreshHevyContext()
            systemContext = snapshot.text
            _uiState.value = _uiState.value.copy(
                isSyncingHevyData = false,
                hasHevyData = snapshot.hasWorkoutData,
                hevyWorkoutCount = snapshot.workoutCount,
                usesLiveExerciseHistory = snapshot.usesLiveExerciseHistory
            )
        }
    }

    private fun updateLastMessage(newText: String) {
        val currentList = _uiState.value.messages.toMutableList()
        if (currentList.isNotEmpty()) {
            val lastMsg = currentList.last()
            if (lastMsg.sender == "ai") {
                currentList[currentList.lastIndex] = lastMsg.copy(text = newText)
                _uiState.value = _uiState.value.copy(messages = currentList)
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        val currentList = _uiState.value.messages.toMutableList()
        currentList.add(message)
        _uiState.value = _uiState.value.copy(messages = currentList)
    }

    private fun AiFeatureStatus.toUserMessage(): String = when (this) {
        AiFeatureStatus.Ready -> "Preparing on-device AI…"
        AiFeatureStatus.Downloadable -> "Gemini Nano can be downloaded for offline coaching. Preparing model…"
        AiFeatureStatus.Downloading -> "Gemini Nano is downloading. This may take a few minutes on first use."
        AiFeatureStatus.Unavailable ->
            "On-device AI isn't available on this device. Test on a supported physical phone with Gemini Nano (AICore). Debug builds can enable stub responses under Settings → Debug AI UI stubs — that does not validate real Nano."
    }
}
