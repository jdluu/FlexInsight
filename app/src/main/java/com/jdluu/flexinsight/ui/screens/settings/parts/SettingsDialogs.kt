package com.jdluu.flexinsight.ui.screens.settings.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.theme.FlexTheme

@Composable
fun ApiKeyDialog(
    currentApiKey: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    error: String?
) {
    var apiKeyText by remember { mutableStateOf(currentApiKey ?: "") }
    val isValid = apiKeyText.isNotBlank() && apiKeyText.length >= 10

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(id = R.string.settings_dialog_api_key_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_dialog_api_key_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text(stringResource(id = R.string.api_key_label)) },
                    placeholder = { Text(stringResource(id = R.string.api_key_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = (!isValid && apiKeyText.isNotEmpty()) || error != null,
                    supportingText = {
                        when {
                            !isValid && apiKeyText.isNotEmpty() -> {
                                Text(stringResource(id = R.string.api_key_error_length))
                            }
                            error != null -> {
                                Text(error)
                            }
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(apiKeyText) },
                enabled = isValid
            ) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun WeeklyGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidGoalError = stringResource(id = R.string.settings_dialog_goal_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(id = R.string.settings_dialog_goal_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_dialog_goal_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = goalText,
                    onValueChange = {
                        goalText = it
                        error = null
                    },
                    label = { Text(stringResource(id = R.string.settings_dialog_goal_label)) },
                    placeholder = { Text(stringResource(id = R.string.settings_dialog_goal_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goal = goalText.toIntOrNull()
                    if (goal != null && goal in 1..7) {
                        onSave(goal)
                    } else {
                        error = invalidGoalError
                    }
                }
            ) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun ThemeDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val themes = listOf(
        "System" to stringResource(id = R.string.theme_system),
        "Light" to stringResource(id = R.string.theme_light),
        "Dark" to stringResource(id = R.string.theme_dark)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                text = stringResource(id = R.string.theme_label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEach { (themeKey, themeLabel) ->
                    val selected = themeKey == currentTheme
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelect(themeKey) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else FlexTheme.colors.hairline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = themeLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun ClearCacheDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(id = R.string.settings_dialog_clear_cache_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.settings_dialog_clear_cache_desc),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(id = R.string.settings_dialog_clear_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }
    val emptyNameError = stringResource(id = R.string.settings_dialog_profile_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(id = R.string.settings_dialog_profile_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_dialog_profile_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        error = null
                    },
                    label = { Text(stringResource(id = R.string.settings_dialog_profile_label)) },
                    placeholder = { Text(stringResource(id = R.string.settings_dialog_profile_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { msg -> { Text(msg) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onSave(nameText.trim())
                    } else {
                        error = emptyNameError
                    }
                }
            ) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
