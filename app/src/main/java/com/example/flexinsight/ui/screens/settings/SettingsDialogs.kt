package com.example.flexinsight.ui.screens.settings

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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Hevy API Key",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your Hevy API key. You can get it from https://hevy.com/settings?developer",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text("API Key", color = TextSecondary) },
                    placeholder = { Text("Enter your API key", color = TextTertiary) },
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
                onClick = { onSave(apiKeyText) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Save", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
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
                text = "Weekly Goal",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Set your weekly workout goal (number of days per week)",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { 
                        goalText = it
                        error = null
                    },
                    label = { Text("Days per week", color = TextSecondary) },
                    placeholder = { Text("Enter number", color = TextTertiary) },
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
                        error = "Please enter a number between 1 and 7"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Save", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
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
    val themes = listOf("Dark", "Light", "System")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Theme",
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
                Text("Cancel", color = TextSecondary)
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
                text = "Clear Cache",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "This will clear all cached data (exercise templates, routines). Your workout data will not be affected.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
            ) {
                Text("Clear", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
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
                text = "Edit Profile",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your display name",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { 
                        nameText = it
                        error = null
                    },
                    label = { Text("Display Name", color = TextSecondary) },
                    placeholder = { Text("Enter your name", color = TextTertiary) },
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
                Text("Save", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceCardAlt,
        shape = RoundedCornerShape(16.dp)
    )
}
