package com.example.flexinsight.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.data.sync.SyncState
import com.example.flexinsight.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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
                    text = "Syncing...",
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
                    text = "✓ Synced",
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
                    text = "Sync failed",
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

private fun formatTimeAgo(timestamp: Long): String? {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
