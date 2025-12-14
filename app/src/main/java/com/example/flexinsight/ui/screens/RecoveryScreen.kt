package com.example.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.screens.recovery.parts.*

@Composable
fun RecoveryScreen() {
    var moodValue by remember { mutableStateOf(7.5f) }
    var notesText by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            RecoveryHeader()
        }
        item {
            RecoveryScoreCard()
        }
        item {
            SleepRestSection()
        }
        item {
            FatigueSection()
        }
        item {
            MoodLogSection(
                moodValue = moodValue,
                onMoodChange = { moodValue = it },
                notesText = notesText,
                onNotesChange = { notesText = it }
            )
        }
    }
}
