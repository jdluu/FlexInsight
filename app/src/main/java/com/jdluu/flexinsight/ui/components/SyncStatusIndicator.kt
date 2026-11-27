package com.jdluu.flexinsight.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.data.sync.SyncState
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SyncStatusIndicator(
    syncState: SyncState,
    modifier: Modifier = Modifier
) {
    when (syncState) {
        is SyncState.Syncing -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(id = R.string.sync_status_syncing),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is SyncState.Success -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = FlexTheme.colors.recovered,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = stringResource(id = R.string.sync_status_synced),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val timeAgo = formatTimeAgo(syncState.timestamp)
                if (timeAgo != null) {
                    Text(
                        text = timeAgo,
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is SyncState.Error -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = FlexTheme.colors.strained,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = stringResource(id = R.string.sync_status_failed),
                    style = MaterialTheme.typography.labelMedium,
                    color = FlexTheme.colors.strained,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        is SyncState.Idle -> Unit
    }
}

@Composable
private fun formatTimeAgo(timestamp: Long): String? {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> stringResource(id = R.string.sync_status_just_now)
        diff < 3600_000 -> stringResource(id = R.string.sync_status_mins_ago, diff / 60_000)
        diff < 86400_000 -> stringResource(id = R.string.sync_status_hours_ago, diff / 3600_000)
        else -> {
            val formatter = remember {
                DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
            }
            formatter.format(Instant.ofEpochMilli(timestamp))
        }
    }
}
