package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.data.model.Workout
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.ui.screens.aitrainer.parts.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class AITrainerUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false
)


@HiltViewModel
class AITrainerViewModel @Inject constructor(
    private val repository: FlexRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AITrainerUiState())
    val uiState: StateFlow<AITrainerUiState> = _uiState.asStateFlow()

    private val _messages = mutableListOf<ChatMessage>()

    init {
        // Initial static conversation for demo purposes, but with dynamic greeting
        viewModelScope.launch {
            loadDynamicGreeting()
        }
    }

    private suspend fun loadDynamicGreeting() {
        _uiState.value = _uiState.value.copy(isTyping = true)

        // Fetch latest workout for context
        val latestWorkout = try {
            repository.getRecentWorkouts(1).first().firstOrNull()
        } catch (e: Exception) {
            null
        }

        delay(1000) // Simulate AI "thinking"

        val greeting = generateGreeting(latestWorkout)

        _messages.add(ChatMessage("ai", greeting, false))

        // Add some context-aware follow-up if we have a workout
        if (latestWorkout != null) {
             _messages.add(ChatMessage("user", "Actually, let's look at that session. My heart rate felt high.", false))
             _messages.add(ChatMessage("ai", "You're right. You peaked at 172 BPM near the end. Here is the breakdown:", true, hasChart = true))
             _messages.add(ChatMessage("user", "Okay, should I take a rest day then?", false))
        } else {
             _messages.add(ChatMessage("ai", "I don't see any recent workouts. Ready to start your first session?", false))
        }

        _uiState.value = AITrainerUiState(messages = _messages.toList(), isTyping = false)
    }

    private fun generateGreeting(lastWorkout: Workout?): String {
        if (lastWorkout == null) {
            return "Good morning! Ready to start your fitness journey today?"
        }

        val workoutName = lastWorkout.name ?: "workout"
        return "Good morning! I've analyzed your sleep data. You're 15% more recovered than yesterday. Ready to recover from your $workoutName?"
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val newMessages = _uiState.value.messages.toMutableList()
        newMessages.add(ChatMessage("user", text, false))

        _uiState.value = _uiState.value.copy(messages = newMessages, isTyping = true)

        // Simulate AI response
        viewModelScope.launch {
            delay(2000)
            val responseMessages = _uiState.value.messages.toMutableList()
            responseMessages.add(ChatMessage("ai", "Based on your data, I recommend focusing on recovery today.", false))
            _uiState.value = _uiState.value.copy(messages = responseMessages, isTyping = false)
        }
    }
}
