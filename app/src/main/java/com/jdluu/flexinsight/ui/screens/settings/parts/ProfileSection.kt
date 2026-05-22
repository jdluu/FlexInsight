package com.jdluu.flexinsight.ui.screens.settings.parts

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
import com.jdluu.flexinsight.data.model.ProfileInfo
import com.jdluu.flexinsight.data.sync.SyncState
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import com.jdluu.flexinsight.ui.components.ErrorBanner
import com.jdluu.flexinsight.ui.components.SyncStatusIndicator
import com.jdluu.flexinsight.ui.theme.BackgroundDarkAlt
import com.jdluu.flexinsight.ui.theme.Primary
import com.jdluu.flexinsight.ui.theme.SurfaceCardAlt
import com.jdluu.flexinsight.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Profile section showing user stats and sync status.
 *
 * @param profileInfo User profile information
 * @param syncState Current synchronization state
 * @param syncError Synchronization error if any
 * @param onSyncClick Callback when sync button is clicked
 * @param onEditProfileClick Callback when edit profile is clicked
 */
@Composable
fun ProfileSection(
    profileInfo: ProfileInfo?,
    syncState: LoadingState,
    syncError: UiError?,
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
            // Edit profile button - always visible (edits local display name)
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatNumber(profileInfo.totalWorkouts),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Workouts",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(modifier = Modifier.height(32.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    if (profileInfo.memberSince != null) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatMemberSince(profileInfo.memberSince),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Member Since",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSyncClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp),
            enabled = !isSyncing,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isSyncing) "SYNCING..." else "SYNC HEVY DATA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
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
