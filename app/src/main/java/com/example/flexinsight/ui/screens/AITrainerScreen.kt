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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AITrainerScreen() {
    val messages = remember {
        mutableStateListOf(
            ChatMessage("ai", "Good morning! I've analyzed your sleep data. You're 15% more recovered than yesterday. Ready to push for that 5K PR?", false),
            ChatMessage("user", "Actually, let's look at my last run first. My heart rate felt high.", false),
            ChatMessage("ai", "You're right. You peaked at 172 BPM near the 3km mark. Here is the breakdown:", true, hasChart = true),
            ChatMessage("user", "Okay, should I take a rest day then?", false)
        )
    }
    var isTyping by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(messages.size) {
        delay(100)
        listState.animateScrollToItem(messages.size - 1)
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
            
            items(messages) { message ->
                ChatBubble(message)
            }
            
            if (isTyping) {
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
                if (inputText.isNotBlank()) {
                    messages.add(ChatMessage("user", inputText, false))
                    inputText = ""
                    isTyping = true
                    // Simulate AI response
                    scope.launch {
                        delay(2000)
                        isTyping = false
                        messages.add(ChatMessage("ai", "Based on your data, I recommend...", false))
                    }
                }
            }
        )
    }
}
