package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.data.ai.FlexAIClient
import com.example.flexinsight.data.model.Workout
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.ui.screens.aitrainer.parts.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class AITrainerUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val isAiAvailable: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AITrainerViewModel @Inject constructor(
    private val repository: FlexRepository,
    private val aiClient: FlexAIClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(AITrainerUiState())
    val uiState: StateFlow<AITrainerUiState> = _uiState.asStateFlow()

    init {
        checkAiAvailability()
    }

    private fun checkAiAvailability() {
        viewModelScope.launch {
            val isAvailable = aiClient.isAvailable()
            _uiState.value = _uiState.value.copy(isAiAvailable = isAvailable)
            
            if (isAvailable) {
                loadDynamicContextAndGreeting()
            } else {
                 _uiState.value = _uiState.value.copy(
                     messages = listOf(ChatMessage("ai", "Sorry, on-device AI is not supported on this device.", false))
                 )
            }
        }
    }

    private suspend fun loadDynamicContextAndGreeting() {
        _uiState.value = _uiState.value.copy(isTyping = true)

        val latestWorkout = try {
            repository.getRecentWorkouts(1).first().firstOrNull()
        } catch (e: Exception) {
            null
        }

        // Construct initial context prompt
        val contextPrompt = if (latestWorkout != null) {
            "The user's last workout was '${latestWorkout.name}' on ${java.util.Date(latestWorkout.startTime)}. " +
            "Act as a professional fitness coach. Greet the user and ask how they are feeling after their last session."
        } else {
            "The user has no recorded workouts. Act as a professional fitness coach. " +
            "Greet the user enthusiastically and ask them about their fitness goals."
        }
        
        // We treat the "system instructions" as a prompt for the greeting generation here
        val result = aiClient.generateResponse(contextPrompt)
        
        _uiState.value = _uiState.value.copy(isTyping = false)

        if (result is Result.Success) {
            val greeting = result.data
            addMessage(ChatMessage("ai", greeting, false))
        } else {
            addMessage(ChatMessage("ai", "Hello! I'm your AI Trainer. Ready to workout?", false))
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // 1. Add User Message
        addMessage(ChatMessage("user", text, true))
        _uiState.value = _uiState.value.copy(isTyping = true)

        viewModelScope.launch {
            // 2. Build History
            val history = _uiState.value.messages.map { msg ->
                val role = if (msg.sender == "user") "user" else "model"
                role to msg.text
            }

            // 3. Generate Response
            val result = aiClient.generateResponse(text, history)
            
            _uiState.value = _uiState.value.copy(isTyping = false)

            when (result) {
                is Result.Success -> {
                    addMessage(ChatMessage("ai", result.data, false))
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.error.message)
                    addMessage(ChatMessage("ai", "I'm having trouble thinking right now. Please try again.", false))
                }
            }
        }
    }
    
    private fun addMessage(message: ChatMessage) {
        val currentList = _uiState.value.messages.toMutableList()
        currentList.add(message)
        _uiState.value = _uiState.value.copy(messages = currentList)
    }
}
