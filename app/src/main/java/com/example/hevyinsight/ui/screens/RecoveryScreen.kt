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

@Composable
fun RecoveryScreen() {
    var moodValue by remember { mutableStateOf(7.5f) }
    var notesText by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDarkAlt),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            RecoveryHeader()
        }
        item {
            RecoveryScoreCard()
        }
        item {
            SleepRestSection()
        }
        item {
            FatigueSection()
        }
        item {
            MoodLogSection(
                moodValue = moodValue,
                onMoodChange = { moodValue = it },
                notesText = notesText,
                onNotesChange = { notesText = it }
            )
        }
    }
}

@Composable
fun RecoveryHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recovery",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Synced",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun RecoveryScoreCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
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
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(192.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = 0.82f,
                        modifier = Modifier.fillMaxSize(),
                        color = Primary,
                        strokeWidth = 12.dp,
                        backgroundColor = SurfaceVariant.copy(alpha = 0.5f)
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "82",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "SCORE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Text(
                        text = "High Readiness",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Your HRV is up significantly. AI suggests today is a good day for a PR attempt.",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Primary,
    strokeWidth: androidx.compose.ui.unit.Dp = 8.dp,
    backgroundColor: Color = Color.Gray.copy(alpha = 0.3f)
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (size.minDimension - strokeWidthPx) / 2
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            
            // Background circle
            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx)
            )
            
            // Progress arc
            val sweepAngle = 360 * progress
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidthPx,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                ),
                topLeft = androidx.compose.ui.geometry.Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }
    }
}

@Composable
fun SleepRestSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Sleep & Rest",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Last 7 Days",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "7h 42m",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "avg",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
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
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "+15%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "vs last week",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                
                // Sleep chart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val sleepData = listOf(0.5f, 0.6f, 0.65f, 0.85f, 0.4f, 0.9f, 0.75f)
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    
                    sleepData.forEachIndexed { index, value ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(128.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index == 3) Primary
                                        else SurfaceVariant
                                    )
                                    .fillMaxHeight(value)
                            )
                            Text(
                                text = days[index],
                                fontSize = 10.sp,
                                fontWeight = if (index == 3) FontWeight.Bold else FontWeight.Normal,
                                color = if (index == 3) Color.White else TextSecondary
                            )
                        }
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Primary.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Primary.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            }
                        }
                        Text(
                            text = "Strong correlation: Sleep +10% on days following cardio sessions.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FatigueSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Fatigue",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Leg Day (Heavy)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Yesterday at 6:00 PM",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = RedAccent.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, RedAccent.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "Recovering",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedAccent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FatigueStatBox(
                            label = "Elapsed",
                            value = "14h",
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                        FatigueStatBox(
                            label = "Train In",
                            value = "6h",
                            icon = Icons.Default.Bolt,
                            modifier = Modifier.weight(1f),
                            isHighlighted = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FatigueStatBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isHighlighted) Primary.copy(alpha = 0.1f) else SurfaceVariant,
        border = if (isHighlighted) BorderStroke(1.dp, Primary.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlighted) Primary else TextSecondary
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) Primary.copy(alpha = 0.8f) else TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) Primary else Color.White
                )
            }
        }
    }
}

@Composable
fun MoodLogSection(
    moodValue: Float,
    onMoodChange: (Float) -> Unit,
    notesText: String,
    onNotesChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Mood Log",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "How do you feel today?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = moodValue.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                    
                    Slider(
                        value = moodValue,
                        onValueChange = onMoodChange,
                        valueRange = 1f..10f,
                        steps = 17,
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = Primary,
                            inactiveTrackColor = SurfaceVariant
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Drained",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Unstoppable",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        TextField(
                            value = notesText,
                            onValueChange = onNotesChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add notes on soreness, pain, or injury...", color = TextSecondary) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            maxLines = 4
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "\"You often report lower mood ratings the day after heavy leg sessions. Consider active recovery today.\"",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
