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
    private val aiClient: FlexAIClient,
    private val buildAiContextUseCase: com.example.flexinsight.domain.usecase.BuildAiContextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AITrainerUiState())
    val uiState: StateFlow<AITrainerUiState> = _uiState.asStateFlow()

    private var systemContext: String? = null

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

        // Generate rich context from repositories
        val context = try {
            buildAiContextUseCase()
        } catch (e: Exception) {
            android.util.Log.e("AITrainerViewModel", "Failed to build AI context", e)
            "User Data unavailable. Act as a fitness coach."
        }
        systemContext = context // Store for future turns
        
        // Construct prompt that includes the context for the INITIAL greeting
        val greetingPrompt = "$context\n\nBased on the above context, greet the user and ask a relevant question about their training."
        
        val result = aiClient.generateResponse(greetingPrompt)
        
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
        _uiState.value = _uiState.value.copy(
            isTyping = true,
            error = null // Clear previous errors
        )

        viewModelScope.launch {
            // 2. Build History
            val history = mutableListOf<Pair<String, String>>()
            
            // Inject System Context as the very first 'user' message (hidden from UI)
            // or as a distinct role if client supported it. For now, prepending to user history is safest for Nano.
            systemContext?.let {
                history.add("user" to "System Context:\n$it")
                history.add("model" to "Understood. I will act as a fitness coach with that context.")
            }

            // Append actual chat history
            _uiState.value.messages.forEach { msg ->
                val role = if (msg.sender == "user") "user" else "model"
                history.add(role to msg.text)
            }

            // 3. Prepare AI Message Placeholder
            var currentMessageId: String? = null
            var fullResponseBuilder = StringBuilder()

            try {
                aiClient.generateResponseStream(text, history).collect { chunk ->
                    if (chunk.startsWith("Error:")) {
                        // Handle streaming error reported as text
                         _uiState.value = _uiState.value.copy(
                            isTyping = false,
                            error = chunk
                        )
                    } else {
                        fullResponseBuilder.append(chunk)
                        val currentText = fullResponseBuilder.toString()
                        
                        if (currentMessageId == null) {
                            // First chunk received: Create specific message instance
                            _uiState.value = _uiState.value.copy(isTyping = false)
                            val newMessage = ChatMessage("ai", currentText, false)
                            addMessage(newMessage)
                            currentMessageId = "started"
                        } else {
                            // Update last message
                             updateLastMessage(currentText)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AITrainerViewModel", "Failed to generate stream response", e)
                _uiState.value = _uiState.value.copy(
                    isTyping = false,
                    error = e.message
                )
                if (currentMessageId == null) {
                     addMessage(ChatMessage("ai", "I'm having trouble thinking right now. Please try again.", false))
                }
            } finally {
                 _uiState.value = _uiState.value.copy(isTyping = false)
            }
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
}
