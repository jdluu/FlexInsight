package com.example.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.screens.recovery.parts.*
import androidx.compose.ui.res.stringResource
import com.example.flexinsight.R
import com.example.flexinsight.data.model.MuscleGroup
import com.example.flexinsight.ui.viewmodel.RecoveryViewModel

import com.example.flexinsight.ui.components.RecoverySkeleton

@Composable
fun RecoveryScreen(
    viewModel: RecoveryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            RecoverySkeleton()
        }
        return
    }

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
             if (uiState.isGeneratingInsight) {
                 Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                         Spacer(modifier = Modifier.width(8.dp))
                         Text(stringResource(id = R.string.recovery_smart_analysis_running), style = MaterialTheme.typography.bodySmall)
                     }
                 }
             } else if (uiState.aiInsight != null) {
                 Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                 ) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             // Icon could go here
                             Text(stringResource(id = R.string.recovery_ai_insight), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                         }
                         Spacer(modifier = Modifier.height(4.dp))
                         Text(uiState.aiInsight!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                     }
                 }
             }
        }
        item {
            SleepRestSection()
        }
        item {
            FatigueSection(
                muscleRecovery = uiState.muscleRecovery
            )
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
