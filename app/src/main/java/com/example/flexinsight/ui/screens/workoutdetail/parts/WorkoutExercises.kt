package com.example.flexinsight.ui.screens.workoutdetail.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.SurfaceCard
import com.example.flexinsight.ui.theme.TextSecondary
import com.example.flexinsight.ui.utils.UnitConverter
import com.example.flexinsight.ui.viewmodel.ExerciseWithSets

data class SetData(
    val number: Int,
    val weight: String,
    val reps: String,
    val rpe: String,
    val isPR: Boolean = false
)

@Composable
fun ExercisesSection(
    exercisesWithSets: List<ExerciseWithSets>,
    viewOnlyMode: Boolean = false,
    useMetric: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Exercises",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (!viewOnlyMode) {
                TextButton(onClick = {}) {
                    Text(
                        text = "Edit workout",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Primary
                    )
                }
            }
        }
        
        exercisesWithSets.forEachIndexed { index, exerciseWithSets ->
            val exercise = exerciseWithSets.exercise
            val sets = exerciseWithSets.sets
            
            // Convert sets to SetData format
            val setsData = sets.map { set ->
                SetData(
                    number = set.number,
                    weight = UnitConverter.formatWeight(set.weight, useMetric),
                    reps = set.reps?.toString() ?: "-",
                    rpe = if (set.rpe != null) String.format("%.1f", set.rpe) else "-",
                    isPR = set.isPersonalRecord
                )
            }
            
            // Find best set (highest weight * reps)
            val bestSet = sets.maxByOrNull { (it.weight ?: 0.0) * (it.reps ?: 0) }
            val bestSetText = bestSet?.let {
                val weight = UnitConverter.convertWeight(it.weight, useMetric) ?: 0.0
                val reps = it.reps ?: 0
                if (weight > 0 && reps > 0) {
                    "Best: ${String.format("%.0f", weight)}x$reps"
                } else null
            }
            
            ExpandableExerciseCard(
                number = index + 1,
                exerciseName = exercise.name,
                sets = sets.size,
                equipment = "Exercise", // TODO: Get equipment from exercise template if available
                isExpanded = index == 0, // Expand first exercise by default
                setsData = setsData,
                bestSet = bestSetText,
                useMetric = useMetric
            )
        }
    }
}

@Composable
fun ExpandableExerciseCard(
    number: Int,
    exerciseName: String,
    sets: Int,
    equipment: String,
    isExpanded: Boolean,
    setsData: List<SetData> = emptyList(),
    improvement: String? = null,
    bestSet: String? = null,
    useMetric: Boolean = false
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.05f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = number.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = exerciseName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$sets sets • $equipment",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (bestSet != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.05f)
                        ) {
                            Text(
                                text = bestSet,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(24.dp)
                            .then(
                                if (expanded) Modifier.rotate(180f)
                                else Modifier
                            )
                    )
                }
            }
            
            if (expanded && setsData.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Set",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.width(40.dp),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = UnitConverter.getWeightUnit(useMetric),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Reps",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "RPE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    setsData.forEach { set ->
                        SetRow(set)
                    }
                    
                    if (improvement != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = improvement,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetRow(set: SetData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (set.isPR) Primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f),
        border = if (set.isPR) BorderStroke(1.dp, Primary.copy(alpha = 0.2f)) else null
    ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (set.isPR) Modifier.border(2.dp, Primary, RoundedCornerShape(8.dp))
                        else Modifier
                    )
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = set.number.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.width(40.dp)
                )
                Text(
                    text = set.weight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = set.reps,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                if (set.isPR) {
                    Row(
                        modifier = Modifier.width(40.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "PR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                } else {
                    Text(
                        text = set.rpe,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
