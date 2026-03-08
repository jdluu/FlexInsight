package com.example.flexinsight.ui.screens.settings.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.flexinsight.R
import com.example.flexinsight.ui.theme.*

/**
 * Dialog for entering API Key.
 */
@Composable
fun ApiKeyDialog(
    currentApiKey: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    error: String?
) {
    var apiKeyText by remember { mutableStateOf(currentApiKey ?: "") }
    // Basic validation logic matching ApiKeyManager
    val isValid = apiKeyText.isNotBlank() && apiKeyText.length >= 10

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.settings_dialog_api_key_title),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_dialog_api_key_desc),
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text(stringResource(id = R.string.api_key_label), color = TextSecondary) },
                    placeholder = { Text(stringResource(id = R.string.api_key_placeholder), color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = TextSecondary,
                        unfocusedLabelColor = TextSecondary
                    ),
                    singleLine = true,
                    isError = !isValid && apiKeyText.isNotEmpty(),
                    supportingText = {
                        if (!isValid && apiKeyText.isNotEmpty()) {
                            Text(
                                text = stringResource(id = R.string.api_key_error_length),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        } else if (error != null) {
                             Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(apiKeyText) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.5f)
                )
            ) {
                Text(stringResource(id = R.string.save), color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = TextSecondary)
            }
        },
        containerColor = SurfaceCardAlt,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Dialog for setting weekly goal.
 */
@Composable
fun WeeklyGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.settings_dialog_goal_title),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_dialog_goal_desc),
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = goalText,
                    onValueChange = {
                        goalText = it
                        error = null
                    },
                    label = { Text(stringResource(id = R.string.settings_dialog_goal_label), color = TextSecondary) },
                    placeholder = { Text(stringResource(id = R.string.settings_dialog_goal_placeholder), color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = TextSecondary,
                        unfocusedLabelColor = TextSecondary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                val errorText = error
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = RedAccent,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goal = goalText.toIntOrNull()
                    if (goal != null && goal > 0 && goal <= 7) {
                        onSave(goal)
                    } else {
                        error = "Please enter a number between 1 and 7" // Can leave this dynamic or resource mapped later
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(stringResource(id = R.string.save), color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = TextSecondary)
            }
        },
        containerColor = SurfaceCardAlt,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Dialog for selecting theme.
 */
@Composable
fun ThemeDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val themes = listOf("System", "Light", "Dark") // We skip true I18N here without a dynamic string mapper

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Theme", // This could also optionally be mapped
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themes.forEach { theme ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (theme == currentTheme) Primary.copy(alpha = 0.2f) else SurfaceCardAlt,
                        border = if (theme == currentTheme) BorderStroke(1.dp, Primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = theme,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = if (theme == currentTheme) FontWeight.Bold else FontWeight.Normal
                            )
                            if (theme == currentTheme) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = TextSecondary)
            }
        },
        containerColor = SurfaceCardAlt,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Dialog for clearing cache confirmation.
 */
@Composable
fun ClearCacheDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.settings_dialog_clear_cache_title),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.settings_dialog_clear_cache_desc),
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
            ) {
                Text(stringResource(id = R.string.settings_dialog_clear_action), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = TextSecondary)
            }
        },
        containerColor = SurfaceCardAlt,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Dialog for editing profile display name.
 */
@Composable
fun EditProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.settings_dialog_profile_title),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_dialog_profile_desc),
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        error = null
                    },
                    label = { Text(stringResource(id = R.string.settings_dialog_profile_label), color = TextSecondary) },
                    placeholder = { Text(stringResource(id = R.string.settings_dialog_profile_placeholder), color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = TextSecondary,
                        unfocusedLabelColor = TextSecondary
                    ),
                    singleLine = true
                )
                val errorText = error
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = RedAccent,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onSave(nameText.trim())
                    } else {
                        error = "Name cannot be empty"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(stringResource(id = R.string.save), color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = TextSecondary)
            }
        },
        containerColor = SurfaceCardAlt,
        shape = RoundedCornerShape(16.dp)
    )
}
