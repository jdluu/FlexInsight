package com.jdluu.flexinsight.ui.screens.aitrainer.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdluu.flexinsight.ui.components.PlateCard

@Composable
fun CoachingContextBanner(
    title: String,
    summary: String,
    modifier: Modifier = Modifier
) {
    PlateCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
