package com.jdluu.flexinsight.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jdluu.flexinsight.data.sync.SyncState
import com.jdluu.flexinsight.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.jdluu.flexinsight.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Composable for displaying sync status
 */
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Primary,
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(id = R.string.sync_status_syncing),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        is SyncState.Success -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.sync_status_synced),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                val timeAgo = formatTimeAgo(syncState.timestamp)
                if (timeAgo != null) {
                    Text(
                        text = timeAgo,
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }
        is SyncState.Error -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.sync_status_failed),
                    color = RedAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        is SyncState.Idle -> {
            // Don't show anything when idle
        }
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
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
