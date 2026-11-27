package com.jdluu.flexinsight.ui.screens.settings.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.domain.model.ProfileInfo
import com.jdluu.flexinsight.data.sync.SyncState
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import com.jdluu.flexinsight.ui.components.ErrorBanner
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.components.SyncStatusIndicator
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.PlateNumeralMedium
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileSection(
    profileInfo: ProfileInfo?,
    syncState: LoadingState,
    syncError: UiError?,
    onSyncClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    val isSyncing = syncState.isLoading
    val displayName = getDisplayName(profileInfo)
    val initials = displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.size(84.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (initials.isNotEmpty() && displayName != "Athlete") {
                    Text(
                        text = initials,
                        style = PlateNumeralMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onEditProfileClick) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            PlateStatusPill(
                label = if (profileInfo?.isProMember == true) "Hevy Pro" else "Hevy Account",
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                borderColor = FlexTheme.colors.hairline
            )
        }

        if (profileInfo != null) {
            PlateCard(containerColor = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = formatNumber(profileInfo.totalWorkouts),
                            style = PlateNumeralMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Workouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    profileInfo.memberSince?.let { memberSince ->
                        VerticalDivider(
                            modifier = Modifier.height(36.dp),
                            thickness = 1.dp,
                            color = FlexTheme.colors.hairline
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = formatMemberSince(memberSince),
                                style = MonoMetricMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Member Since",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onSyncClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = MaterialTheme.shapes.small,
            enabled = !isSyncing,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSyncing) "Syncing Hevy…" else "Sync Hevy Data",
                style = MaterialTheme.typography.labelLarge
            )
        }

        val syncStateForIndicator = when (syncState) {
            is LoadingState.Loading -> SyncState.Syncing
            is LoadingState.Success -> SyncState.Success(timestamp = System.currentTimeMillis())
            is LoadingState.Error -> SyncState.Error(error = syncState.error)
            else -> SyncState.Idle
        }
        SyncStatusIndicator(
            syncState = syncStateForIndicator,
            modifier = Modifier.fillMaxWidth()
        )

        syncError?.let { error ->
            ErrorBanner(
                error = error,
                modifier = Modifier.fillMaxWidth(),
                onDismiss = null
            )
        }
    }
}

private fun formatMemberSince(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

private fun getDisplayName(profileInfo: ProfileInfo?): String {
    return profileInfo?.displayName?.takeIf { it.isNotBlank() } ?: "Athlete"
}

private fun formatNumber(number: Int): String {
    return NumberFormat.getNumberInstance(Locale.getDefault()).format(number)
}
