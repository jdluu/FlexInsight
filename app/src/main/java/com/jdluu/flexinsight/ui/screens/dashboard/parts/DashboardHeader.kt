package com.jdluu.flexinsight.ui.screens.dashboard.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHeader(
    userName: String?,
    lastSyncAt: Long?,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onProfileClick: () -> Unit,
    onOpenPlateRack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> stringResource(id = R.string.dashboard_greeting_morning)
        hour < 18 -> stringResource(id = R.string.dashboard_greeting_afternoon)
        else -> stringResource(id = R.string.dashboard_greeting_evening)
    }
    val athleteName = userName?.takeIf { it.isNotBlank() }
        ?: stringResource(id = R.string.dashboard_welcome_guest)

    val syncLabel = when {
        isSyncing -> stringResource(id = R.string.sync_status_syncing)
        lastSyncAt != null -> {
            val diff = System.currentTimeMillis() - lastSyncAt
            when {
                diff < 60_000 -> stringResource(id = R.string.sync_status_just_now)
                diff < 3600_000 -> stringResource(id = R.string.sync_status_mins_ago, diff / 60_000)
                diff < 86400_000 -> stringResource(id = R.string.sync_status_hours_ago, diff / 3600_000)
                else -> stringResource(id = R.string.sync_status_synced)
            }
        }
        else -> stringResource(id = R.string.dashboard_tap_sync)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = stringResource(id = R.string.nav_today),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = "$greeting, $athleteName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable(enabled = !isSyncing, onClick = onSyncClick)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, FlexTheme.colors.hairline),
                        modifier = Modifier.heightIn(min = 34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = stringResource(id = R.string.dashboard_tap_sync),
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = syncLabel,
                                style = MonoMetricSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
                IconButton(onClick = onOpenPlateRack) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = stringResource(id = R.string.content_desc_plate_rack),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onProfileClick) {
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
