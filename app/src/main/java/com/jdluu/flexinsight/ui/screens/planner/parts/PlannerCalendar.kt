package com.jdluu.flexinsight.ui.screens.planner.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.data.model.DayInfo
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium

data class PlannerDayUiModel(
    val name: String,
    val date: Int,
    val hasWorkout: Boolean = false,
    val isSelected: Boolean = false,
    val isCompleted: Boolean = false,
    val isToday: Boolean = false
)

@Composable
fun WeekCalendar(
    weekCalendarData: List<DayInfo> = emptyList(),
    selectedDayIndex: Int = 0,
    onDaySelected: (Int) -> Unit = {}
) {
    val today = System.currentTimeMillis()
    val dayModels = weekCalendarData.mapIndexed { index, dayData ->
        val dayStart = dayData.timestamp
        val dayEnd = dayStart + 24L * 60L * 60L * 1000L
        PlannerDayUiModel(
            name = dayData.name,
            date = dayData.date,
            hasWorkout = dayData.hasWorkout,
            isSelected = index == selectedDayIndex,
            isCompleted = dayData.isCompleted,
            isToday = today in dayStart until dayEnd
        )
    }

    if (dayModels.size <= 7) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dayModels.forEachIndexed { index, dayUiModel ->
                DayCard(
                    day = dayUiModel,
                    onClick = { onDaySelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(count = dayModels.size, key = { it }) { index ->
                DayCard(
                    day = dayModels[index],
                    onClick = { onDaySelected(index) },
                    modifier = Modifier.width(52.dp)
                )
            }
        }
    }
}

@Composable
fun DayCard(
    day: PlannerDayUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        day.isSelected -> MaterialTheme.colorScheme.primary
        day.isToday -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surface
    }
    val borderStroke = when {
        day.isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        day.isToday -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else -> BorderStroke(1.dp, FlexTheme.colors.hairline)
    }
    val labelColor = when {
        day.isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        day.isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dateColor = when {
        day.isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .height(82.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${day.name} ${day.date}" +
                    (if (day.isToday) ", Today" else "") +
                    (if (day.isCompleted) ", Completed" else if (day.hasWorkout) ", Planned" else "")
            },
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = borderStroke,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = day.name.take(3),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (day.isSelected || day.isToday) FontWeight.Bold else FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )

            Text(
                text = day.date.toString(),
                style = MonoMetricMedium,
                fontWeight = FontWeight.Bold,
                color = dateColor,
                maxLines = 1
            )

            Box(
                modifier = Modifier.height(14.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    day.isCompleted -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (day.isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                FlexTheme.colors.recovered
                            },
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    day.hasWorkout -> {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (day.isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = if (day.isSelected) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}
