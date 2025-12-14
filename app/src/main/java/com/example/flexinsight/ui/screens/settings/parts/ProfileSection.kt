package com.example.flexinsight.ui.screens.settings.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.data.model.ProfileInfo
import com.example.flexinsight.data.sync.SyncState
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.common.UiError
import com.example.flexinsight.ui.components.ErrorBanner
import com.example.flexinsight.ui.components.SyncStatusIndicator
import com.example.flexinsight.ui.theme.BackgroundDarkAlt
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.SurfaceCardAlt
import com.example.flexinsight.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Profile section showing user stats and sync status.
 *
 * @param profileInfo User profile information
 * @param syncState Current synchronization state
 * @param syncError Synchronization error if any
 * @param viewOnlyMode Whether the app is in view-only mode
 * @param onSyncClick Callback when sync button is clicked
 * @param onEditProfileClick Callback when edit profile is clicked
 */
@Composable
fun ProfileSection(
    profileInfo: ProfileInfo?,
    syncState: LoadingState,
    syncError: UiError?,
    viewOnlyMode: Boolean,
    onSyncClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    val isSyncing = syncState.isLoading
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(112.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(4.dp, MaterialTheme.colorScheme.secondaryContainer)
            ) {}
            if (!viewOnlyMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(4.dp, MaterialTheme.colorScheme.background)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onEditProfileClick)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit profile",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        Text(
            text = getDisplayName(profileInfo),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (profileInfo?.isProMember == true) "Pro Member" else "Free Member",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Profile stats
        if (profileInfo != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatNumber(profileInfo.totalWorkouts),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Workouts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (profileInfo.memberSince != null) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatMemberSince(profileInfo.memberSince),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Member since",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Button(
            onClick = onSyncClick,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSyncing
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSyncing) "Syncing..." else "Sync Hevy Data",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        // Sync status indicator
        val syncStateForIndicator = when (syncState) {
            is LoadingState.Loading -> SyncState.Syncing
            is LoadingState.Success -> SyncState.Success(
                timestamp = System.currentTimeMillis()
            )
            is LoadingState.Error -> SyncState.Error(
                error = syncState.error
            )
            else -> SyncState.Idle
        }
        SyncStatusIndicator(
            syncState = syncStateForIndicator,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Sync error banner
        syncError?.let { error ->
            ErrorBanner(
                error = error,
                modifier = Modifier.fillMaxWidth(),
                onDismiss = null
            )
        }
    }
}

/**
 * Format member since date
 */
private fun formatMemberSince(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

/**
 * Get display name from profile info
 */
private fun getDisplayName(profileInfo: ProfileInfo?): String {
    return profileInfo?.displayName ?: "User"
}

/**
 * Format number nicely
 */
private fun formatNumber(number: Int): String {
    return java.text.NumberFormat.getNumberInstance(Locale.getDefault()).format(number)
}
