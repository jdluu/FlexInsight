package com.example.hevyinsight.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hevyinsight.ui.common.UiError
import com.example.hevyinsight.ui.theme.*

/**
 * Composable for displaying error messages with different styles based on error type
 */
@Composable
fun ErrorBanner(
    error: UiError,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val (backgroundColor, textColor, icon) = when (error) {
        is UiError.Network -> Triple(
            RedAccent.copy(alpha = 0.15f),
            RedAccent,
            "📡"
        )
        is UiError.Auth -> Triple(
            RedAccent.copy(alpha = 0.15f),
            RedAccent,
            "🔐"
        )
        is UiError.Server -> Triple(
            RedAccent.copy(alpha = 0.15f),
            RedAccent,
            "⚠️"
        )
        is UiError.Unknown -> Triple(
            RedAccent.copy(alpha = 0.15f),
            RedAccent,
            "❌"
        )
    }
    
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = when (error) {
                        is UiError.Network -> "Network Error"
                        is UiError.Auth -> "Authentication Error"
                        is UiError.Server -> "Server Error"
                        is UiError.Unknown -> "Error"
                    },
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = error.message,
                    color = textColor.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
            
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(
                        text = "✕",
                        color = textColor,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
