package com.example.hevyinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hevyinsight.ui.theme.*

@Composable
fun HistoryScreen(
    onNavigateToWorkoutDetail: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDarkAlt),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            HistoryHeader()
        }
        item {
            TabSelector(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        }
        item {
            AIInsightsCard()
        }
        item {
            StatsGrid()
        }
        item {
            TotalVolumeCard()
        }
        item {
            AnalysisBreakdown()
        }
        item {
            RecentPRsSection(onNavigateToWorkoutDetail = onNavigateToWorkoutDetail)
        }
    }
}

@Composable
fun HistoryHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Workout History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Filter",
                tint = Primary
            )
        }
    }
}

@Composable
fun TabSelector(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Summary", "Exercises", "Compare")
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCardAlt
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == index) Primary else Color.Transparent,
                    onClick = { onTabSelected(index) }
                ) {
                    Text(
                        text = tab,
                        fontSize = 14.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == index) BackgroundDarkAlt else TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AIInsightsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI INSIGHTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "You've increased your chest volume by 15% this month! Keep pushing on compound movements.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    lineHeight = 22.sp
                )
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary.copy(alpha = 0.1f),
                        contentColor = Primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = "View Analysis",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatsGrid() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("42", "Workouts", modifier = Modifier.weight(1f))
        StatCard("12.4k", "Avg Vol (kg)", modifier = Modifier.weight(1f))
        StatCard("Oct 12", "Best Week", modifier = Modifier.weight(1f), isHighlighted = true)
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier, isHighlighted: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCardAlt
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (value.contains("k")) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = value.substringBefore("k"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted) Primary else Color.White
                    )
                    Text(
                        text = "k",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted) Primary else TextSecondary
                    )
                }
            } else {
                Text(
                    text = value,
                    fontSize = if (isHighlighted) 20.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) Primary else Color.White
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun TotalVolumeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Total Volume",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "124,500",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "kg",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextSecondary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "12%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }
            
            // Simplified chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                // Chart representation - simplified
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Placeholder for chart
                    Text(
                        text = "Volume Chart",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) { index ->
                    Text(
                        text = "W${index + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisBreakdown() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Analysis Breakdown",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnalysisCard("Volume by Muscle", modifier = Modifier.weight(1f))
            AnalysisCard("Duration Trend", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AnalysisCard(title: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            if (title == "Duration Trend") {
                Text(
                    text = "55 min avg",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(6) { index ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(if (index == 3) 64.dp else (20 + index * 8).dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (index == 3) Primary
                                        else SurfaceHighlight
                                    )
                            )
                            Text(
                                text = listOf("M", "T", "W", "T", "F", "S")[index],
                                fontSize = 10.sp,
                                fontWeight = if (index == 3) FontWeight.Bold else FontWeight.Normal,
                                color = if (index == 3) Color.White else TextSecondary
                            )
                        }
                    }
                }
            } else {
                // Volume by Muscle - simplified pie chart representation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MuscleLegend("Chest", Primary)
                            MuscleLegend("Legs", PrimaryDark)
                            MuscleLegend("Back", Color(0xFF065F24))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MuscleLegend(name: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun RecentPRsSection(onNavigateToWorkoutDetail: (String) -> Unit) {
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
                text = "Recent PRs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            TextButton(onClick = {}) {
                Text(
                    text = "View All",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Primary
                )
            }
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PRCard(
                exercise = "Bench Press",
                date = "Oct 14",
                muscle = "Chest",
                weight = "225",
                unit = "lbs",
                isNewPR = true,
                onClick = { onNavigateToWorkoutDetail("1") }
            )
            PRCard(
                exercise = "Back Squat",
                date = "Oct 12",
                muscle = "Legs",
                weight = "315",
                unit = "lbs",
                isNewPR = false,
                onClick = { onNavigateToWorkoutDetail("2") }
            )
        }
    }
}

@Composable
fun PRCard(
    exercise: String,
    date: String,
    muscle: String,
    weight: String,
    unit: String,
    isNewPR: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceHighlight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (muscle == "Chest") Icons.Default.FitnessCenter else Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = if (isNewPR) Primary else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = exercise,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$date • $muscle",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            // Mini chart placeholder
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            )
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = weight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary
                    )
                }
                if (isNewPR) {
                    Text(
                        text = "NEW 1RM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                } else {
                    Text(
                        text = "Vol: 4.2k",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
