package com.example.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.flexinsight.R
import com.example.flexinsight.ui.components.MuscleHeatmap
import com.example.flexinsight.ui.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryAnalysisScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.history_analysis_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.history_analysis_back_desc))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.history_analysis_fatigue_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val recoveryMap = uiState.muscleGroupProgress.mapNotNull { 
                        // Map progress string (e.g. "HI", "MED") to a float for the heatmap
                        val strength = when (it.intensity) {
                            "HI" -> 0.2f
                            "MED" -> 0.5f
                            else -> 0.9f
                        }
                        
                        // Parse String to Enum
                        val enumVal = try {
                            com.example.flexinsight.data.model.MuscleGroup.valueOf(it.muscleGroup.uppercase())
                        } catch (e: Exception) {
                            null
                        }
                        
                        if (enumVal != null) enumVal to strength else null
                    }.toMap()
                    
                    if (recoveryMap.isNotEmpty()) {
                        MuscleHeatmap(recoveryMap = recoveryMap)
                    } else {
                        Text(
                            text = stringResource(id = R.string.history_analysis_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
