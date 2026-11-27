package com.jdluu.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.flexinsight.ui.screens.aitrainer.parts.*
import com.jdluu.flexinsight.ui.viewmodel.AITrainerViewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun AITrainerScreen(
    viewModel: AITrainerViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val lastMessageLength = uiState.messages.lastOrNull()?.text?.length ?: 0

    LaunchedEffect(uiState.messages.size, lastMessageLength) {
        if (uiState.messages.isNotEmpty()) {
            delay(50)
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val currentFormattedTime = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, h:mm a"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AITrainerHeader(
            onSettings = onNavigateToSettings,
            hevyWorkoutCount = uiState.hevyWorkoutCount,
            isSyncingHevy = uiState.isSyncingHevyData,
            onRefreshHevy = { viewModel.refreshHevySync() },
            usesLiveExerciseHistory = uiState.usesLiveExerciseHistory,
            isOfflineAi = uiState.isOfflineAi
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics { contentDescription = "Coach Chat History" },
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DateDivider(currentFormattedTime)
            }

            items(
                items = uiState.messages,
                key = { message -> message.id }
            ) { message ->
                ChatBubble(message)
            }

            if (uiState.isTyping) {
                item {
                    Box(modifier = Modifier.semantics { contentDescription = "Coach is typing" }) {
                        TypingIndicator()
                    }
                }
            }
        }

        if (uiState.isAiAvailable && !uiState.isPreparingModel) {
            QuickActionChips(
                onSelectPrompt = { prompt -> viewModel.sendMessage(prompt) }
            )
        }

        uiState.aiStatusMessage?.let { status ->
            if (uiState.isPreparingModel) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        ChatInput(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText.trim())
                    inputText = ""
                }
            },
            enabled = uiState.isAiAvailable && !uiState.isPreparingModel
        )
    }
}
