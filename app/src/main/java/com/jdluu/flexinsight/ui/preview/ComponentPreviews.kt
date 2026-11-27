package com.jdluu.flexinsight.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.domain.model.MuscleGroup
import com.jdluu.flexinsight.data.sync.SyncState
import com.jdluu.flexinsight.domain.model.RoutineComparison
import com.jdluu.flexinsight.domain.model.TrainingLoadScore
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.EmptyStateIllustration
import com.jdluu.flexinsight.ui.components.MuscleHeatmap
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.components.SyncStatusIndicator
import com.jdluu.flexinsight.ui.screens.dashboard.parts.TrainingLoadCard
import com.jdluu.flexinsight.ui.screens.history.parts.ComparisonView
import com.jdluu.flexinsight.ui.screens.history.parts.RoutineDiffCard
import com.jdluu.flexinsight.ui.theme.FlexInsightTheme
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.PlateNumeralLarge
import com.jdluu.flexinsight.ui.viewmodel.ComparisonData

@Preview(name = "Plate Card Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Plate Card Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlateCardPreview() {
    FlexInsightTheme {
        PlateCard(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlateStatusPill(
                    label = "Ready",
                    containerColor = FlexTheme.colors.recoveredContainer,
                    contentColor = FlexTheme.colors.onRecoveredContainer
                )
                Text(
                    text = "86%",
                    style = PlateNumeralLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                CalibratedPlateBar(
                    progress = 0.86f,
                    activeColor = FlexTheme.colors.recovered
                )
            }
        }
    }
}

@Preview(name = "Muscle Heatmap Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Muscle Heatmap Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MuscleHeatmapPreview() {
    FlexInsightTheme {
        MuscleHeatmap(
            recoveryMap = mapOf(
                MuscleGroup.CHEST to 0.85f,
                MuscleGroup.BACK to 0.52f,
                MuscleGroup.LEGS to 0.28f,
                MuscleGroup.SHOULDERS to 0.90f
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Empty state", showBackground = true)
@Composable
private fun EmptyStatePreview() {
    FlexInsightTheme {
        EmptyStateIllustration(
            icon = Icons.Outlined.FitnessCenter,
            title = "No workouts yet",
            description = "Sync with Hevy to see your training history here."
        )
    }
}

@Preview(name = "Training load", showBackground = true)
@Composable
private fun TrainingLoadCardPreview() {
    FlexInsightTheme {
        TrainingLoadCard(
            score = TrainingLoadScore(
                overall = 72,
                hevyVolumeScore = 80,
                cardioScore = 55,
                sleepScore = 70,
                label = "Moderate load",
                detail = "Volume is up 12% vs last week"
            )
        )
    }
}

@Preview(name = "Period comparison", showBackground = true)
@Composable
private fun ComparisonViewPreview() {
    FlexInsightTheme {
        ComparisonView(
            data = ComparisonData(
                currentPeriodLabel = "May",
                previousPeriodLabel = "April",
                totalVolumeCurrent = 45200.0,
                totalVolumePrevious = 38100.0,
                totalWorkoutsCurrent = 12,
                totalWorkoutsPrevious = 9,
                avgDurationCurrent = 3240,
                avgDurationPrevious = 3060
            ),
            useMetric = false
        )
    }
}

@Preview(name = "Routine diff", showBackground = true)
@Composable
private fun RoutineDiffCardPreview() {
    FlexInsightTheme {
        RoutineDiffCard(
            comparison = RoutineComparison(
                routineName = "Push Day A",
                regressions = listOf("Bench Press volume down 8%"),
                improvements = listOf("Overhead Press +5 kg top set"),
                summary = "Overall similar effort; bench eased back while OHP progressed."
            )
        )
    }
}

@Preview(name = "Sync status", showBackground = true)
@Composable
private fun SyncStatusPreview() {
    FlexInsightTheme {
        Column(Modifier.padding(16.dp)) {
            SyncStatusIndicator(syncState = SyncState.Syncing)
            SyncStatusIndicator(
                syncState = SyncState.Success(timestamp = System.currentTimeMillis()),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
