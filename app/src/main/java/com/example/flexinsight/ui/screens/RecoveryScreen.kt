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
import com.example.flexinsight.ui.viewmodel.RecoveryViewModel

@Composable
fun RecoveryScreen(
    viewModel: RecoveryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

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
            // Mapping dynamic status to UI string/display
            val loadText = "Training Load: ${uiState.trainingLoadStatus.name}"
            // Note: RecoveryScoreCard might be static, assuming it takes params or needs update.
            // Let's assume for now we keep the static card but conceptually we'd pass the score.
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
                moodValue = uiState.moodValue,
                onMoodChange = { viewModel.updateMood(it) },
                notesText = uiState.notesText,
                onNotesChange = { viewModel.updateNotes(it) }
            )
        }
    }
}
