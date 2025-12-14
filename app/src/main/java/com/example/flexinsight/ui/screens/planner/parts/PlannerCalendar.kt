package com.example.flexinsight.ui.screens.planner.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.data.model.DayInfo
import com.example.flexinsight.ui.theme.BackgroundDarkAlt
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.TextSecondary
import androidx.compose.foundation.border

/**
 * UI model for detailed day display including selection state
 */
data class PlannerDayUiModel(
    val name: String,
    val date: Int,
    val hasWorkout: Boolean = false,
    val isSelected: Boolean = false,
    val isCompleted: Boolean = false
)

@Composable
fun WeekCalendar(
    weekCalendarData: List<DayInfo> = emptyList(),
    selectedDayIndex: Int = 0,
    onDaySelected: (Int) -> Unit = {}
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(weekCalendarData.size) { index ->
            val dayData = weekCalendarData[index]
            val dayUiModel = PlannerDayUiModel(
                name = dayData.name,
                date = dayData.date,
                hasWorkout = dayData.hasWorkout,
                isSelected = index == selectedDayIndex,
                isCompleted = dayData.isCompleted
            )
            DayCard(
                day = dayUiModel,
                onClick = { onDaySelected(index) }
            )
        }
    }
}

@Composable
fun DayCard(day: PlannerDayUiModel, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(68.dp)
            .height(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (day.isSelected) Primary else Color(0xFFE8ECE9),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (day.isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                radius = 100f
                            )
                        )
                )
            }
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = day.name,
                    fontSize = 12.sp,
                    fontWeight = if (day.isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (day.isSelected) BackgroundDarkAlt else TextSecondary,
                    letterSpacing = if (day.isSelected) 1.sp else 0.sp
                )
                Text(
                    text = day.date.toString(),
                    fontSize = if (day.isSelected) 20.sp else 18.sp,
                    fontWeight = if (day.isSelected) FontWeight.Black else FontWeight.Bold,
                    color = if (day.isSelected) BackgroundDarkAlt else Color.Black
                )
                if (day.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BackgroundDarkAlt,
                        modifier = Modifier.size(12.dp)
                    )
                } else if (day.hasWorkout) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (day.isSelected) BackgroundDarkAlt else Primary)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.Gray, CircleShape)
                    )
                }
            }
        }
    }
}
