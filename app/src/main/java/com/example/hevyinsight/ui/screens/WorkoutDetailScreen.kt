package com.example.hevyinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hevyinsight.ui.theme.*
import com.example.hevyinsight.ui.utils.rememberViewOnlyMode
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkoutDetailScreen(
    workoutId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val viewOnlyMode = rememberViewOnlyMode()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            WorkoutDetailHeader(
                title = "Leg Day Destruction",
                date = "Tue, Oct 24, 2023",
                onNavigateBack = onNavigateBack
            )
        }
        item {
            WorkoutStatsCard()
        }
        item {
            ExercisesSection(viewOnlyMode = viewOnlyMode)
        }
        item {
            NotesSection()
        }
        item {
            AICoachReflectionCard()
        }
    }
}

@Composable
fun WorkoutDetailHeader(
    title: String,
    date: String,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark.copy(alpha = 0.8f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = date,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Primary
                )
            }
        }
    }
}

@Composable
fun WorkoutStatsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 200f
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatItem("1h 15m", "Duration", modifier = Modifier.weight(1f))
                    StatItem("12,450", "Vol (lbs)", modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatItem("18", "Sets", modifier = Modifier.weight(1f))
                    StatItem("145", "Reps", modifier = Modifier.weight(1f))
                }
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TagChip("PR DAY", Primary, Icons.Default.EmojiEvents)
                    TagChip("AI Summary", Color(0xFF9333EA), Icons.Default.AutoAwesome)
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun TagChip(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun ExercisesSection(viewOnlyMode: Boolean = false) {
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
        
        ExpandableExerciseCard(
            number = 1,
            exerciseName = "Back Squat",
            sets = 3,
            equipment = "Barbell",
            isExpanded = true,
            setsData = listOf(
                SetData(1, "225", "5", "7"),
                SetData(2, "235", "5", "PR", isPR = true),
                SetData(3, "235", "4", "9")
            ),
            improvement = "+10 lbs vs last time"
        )
        
        ExpandableExerciseCard(
            number = 2,
            exerciseName = "Leg Press",
            sets = 4,
            equipment = "Machine",
            isExpanded = false,
            bestSet = "Best: 450x8"
        )
        
        ExpandableExerciseCard(
            number = 3,
            exerciseName = "Romanian Deadlift",
            sets = 3,
            equipment = "Dumbbell",
            isExpanded = false
        )
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
    bestSet: String? = null
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
                            text = "lbs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Reps",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "RPE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.width(40.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = set.reps,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

data class SetData(
    val number: Int,
    val weight: String,
    val reps: String,
    val rpe: String,
    val isPR: Boolean = false
)

@Composable
fun NotesSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Edit",
                    tint = TextSecondary
                )
            }
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = BackgroundDark.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Felt strong today, knee didn't hurt much. Good warm up helped.",
                        fontSize = 14.sp,
                        color = Color.White,
                        lineHeight = 20.sp
                    )
                    IconButton(
                        onClick = {},
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AICoachReflectionCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Primary, Color(0xFF4ADE80))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI Coach Reflection",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Great intensity! Your volume is up 5% from last week. Specifically, your Squat strength is trending upwards. Consider resting tomorrow to maximize recovery.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

