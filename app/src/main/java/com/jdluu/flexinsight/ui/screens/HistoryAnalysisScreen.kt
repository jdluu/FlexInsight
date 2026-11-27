package com.jdluu.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.data.model.MuscleGroup
import com.jdluu.flexinsight.ui.components.MuscleHeatmap
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryAnalysisScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.history_analysis_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.history_analysis_back_desc)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )
        HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlateCard(contentPadding = PaddingValues(16.dp)) {
                Text(
                    text = stringResource(id = R.string.history_analysis_fatigue_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
                Spacer(modifier = Modifier.height(14.dp))

                val recoveryMap = uiState.muscleGroupProgress.mapNotNull { progress ->
                    val strength = when (progress.intensity) {
                        "HI" -> 0.2f
                        "MD", "MED" -> 0.5f
                        else -> 0.9f
                    }
                    val enumVal = runCatching {
                        MuscleGroup.valueOf(progress.muscleGroup.uppercase())
                    }.getOrNull()
                    if (enumVal != null) enumVal to strength else null
                }.toMap()

                if (recoveryMap.isNotEmpty()) {
                    MuscleHeatmap(
                        recoveryMap = recoveryMap,
                        figureHeight = 230.dp,
                        showLegend = true
                    )
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
