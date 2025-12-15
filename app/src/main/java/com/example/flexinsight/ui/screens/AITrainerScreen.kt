package com.example.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.screens.aitrainer.parts.*
import com.example.flexinsight.ui.viewmodel.AITrainerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AITrainerScreen(
    viewModel: AITrainerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AITrainerHeader()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DateDivider("Today, 9:41 AM")
            }

            items(uiState.messages) { message ->
                ChatBubble(message)
            }

            if (uiState.isTyping) {
                item {
                    TypingIndicator()
                }
            }
        }

        QuickActionChips()

        ChatInput(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                viewModel.sendMessage(inputText)
                inputText = ""
            }
        )
    }
}
