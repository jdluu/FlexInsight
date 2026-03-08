package com.example.flexinsight.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.screens.aitrainer.parts.*
import com.example.flexinsight.ui.viewmodel.AITrainerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AITrainerScreen(
    viewModel: AITrainerViewModel,
    onNavigateBack: () -> Unit
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
            .statusBarsPadding()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AITrainerHeader(onBack = onNavigateBack)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics { contentDescription = "AI Trainer Chat History" },
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DateDivider("Today, 9:41 AM")
            }

            items(
                items = uiState.messages,
                key = { message -> message.id }
            ) { message ->
                ChatBubble(message)
            }

            if (uiState.isTyping) {
                item {
                    Box(modifier = Modifier.semantics { contentDescription = "AI is typing" }) {
                        TypingIndicator()
                    }
                }
            }
        }



        // Suggested chips row
        val suggestions = listOf("How's my recovery?", "Analyze my last session", "Next workout ideas")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (suggestion in suggestions) {
                Surface(
                    onClick = { viewModel.sendMessage(suggestion) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = suggestion,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        ChatInput(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                viewModel.sendMessage(inputText)
                inputText = ""
            },
            enabled = uiState.isAiAvailable
        )
    }
}
