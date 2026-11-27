package com.jdluu.flexinsight.ui.screens.settings.parts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R

@Composable
fun HealthConnectPermissionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null
) {
    val scroll = rememberScrollState()
    val requestMode = onConfirm != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(
                    if (requestMode) R.string.health_permissions_dialog_title_request
                    else R.string.health_permissions_dialog_title_info
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.health_permissions_dialog_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PermissionExplainRow(
                    title = stringResource(R.string.health_perm_read_sleep_title),
                    reason = stringResource(R.string.health_perm_read_sleep_why)
                )
                PermissionExplainRow(
                    title = stringResource(R.string.health_perm_read_hr_title),
                    reason = stringResource(R.string.health_perm_read_hr_why)
                )
                PermissionExplainRow(
                    title = stringResource(R.string.health_perm_read_steps_title),
                    reason = stringResource(R.string.health_perm_read_steps_why)
                )
                PermissionExplainRow(
                    title = stringResource(R.string.health_perm_read_calories_title),
                    reason = stringResource(R.string.health_perm_read_calories_why)
                )
                PermissionExplainRow(
                    title = stringResource(R.string.health_perm_read_exercise_title),
                    reason = stringResource(R.string.health_perm_read_exercise_why)
                )
                PermissionExplainRow(
                    title = stringResource(R.string.health_perm_write_exercise_title),
                    reason = stringResource(R.string.health_perm_write_exercise_why)
                )
                Text(
                    text = stringResource(R.string.health_permissions_dialog_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (requestMode) {
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.health_permissions_dialog_allow))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.done))
                }
            }
        },
        dismissButton = {
            if (requestMode) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
private fun PermissionExplainRow(title: String, reason: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
