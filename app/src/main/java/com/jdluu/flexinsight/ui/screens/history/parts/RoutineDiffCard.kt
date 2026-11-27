package com.jdluu.flexinsight.ui.screens.history.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.model.RoutineComparison
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.theme.FlexTheme

@Composable
fun RoutineDiffCard(
    comparison: RoutineComparison?,
    modifier: Modifier = Modifier
) {
    if (comparison == null) return

    PlateCard(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = stringResource(R.string.history_routine_diff_title, comparison.routineName),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = comparison.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (comparison.improvements.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.history_routine_improvements),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = FlexTheme.colors.recovered
            )
            Spacer(modifier = Modifier.height(4.dp))
            comparison.improvements.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (comparison.regressions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.history_routine_regressions),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = FlexTheme.colors.strained
            )
            Spacer(modifier = Modifier.height(4.dp))
            comparison.regressions.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
