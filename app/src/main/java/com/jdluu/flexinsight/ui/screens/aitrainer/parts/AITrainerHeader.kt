package com.jdluu.flexinsight.ui.screens.aitrainer.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITrainerHeader(
    onSettings: () -> Unit = {},
    hevyWorkoutCount: Int = 0,
    isSyncingHevy: Boolean = false,
    onRefreshHevy: () -> Unit = {},
    usesLiveExerciseHistory: Boolean = false,
    isOfflineAi: Boolean = true
) {
    val statusText = when {
        isSyncingHevy -> stringResource(id = R.string.ai_trainer_syncing_hevy)
        usesLiveExerciseHistory -> stringResource(id = R.string.ai_trainer_live_history)
        hevyWorkoutCount > 0 -> pluralStringResource(
            id = R.plurals.ai_trainer_hevy_connected,
            count = hevyWorkoutCount,
            hevyWorkoutCount
        )
        else -> stringResource(id = R.string.ai_trainer_hevy_no_data)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.nav_coach),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        if (isOfflineAi) {
                            PlateStatusPill(
                                label = stringResource(id = R.string.ai_trainer_offline_badge),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            actions = {
                if (isSyncingHevy) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(onClick = onRefreshHevy) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(id = R.string.ai_trainer_refresh_hevy),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(id = R.string.nav_settings),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
    }
}
