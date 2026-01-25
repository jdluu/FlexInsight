package com.example.flexinsight.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.data.model.MuscleGroup

/**
 * A geometric representation of a human silhouette that highlights muscle recovery.
 * Color scales from Green (Recovered) to Red (Fatigued).
 */
@Composable
fun MuscleHeatmap(
    recoveryMap: Map<MuscleGroup, Float>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .height(300.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
             // Front View
             SilhouetteView(
                 title = "FRONT",
                 recoveryMap = recoveryMap,
                 isFront = true,
                 modifier = Modifier.weight(1f)
             )
             
             // Back View
             SilhouetteView(
                 title = "BACK",
                 recoveryMap = recoveryMap,
                 isFront = false,
                 modifier = Modifier.weight(1f)
             )
        }
    }
}

@Composable
fun SilhouetteView(
    title: String,
    recoveryMap: Map<MuscleGroup, Float>,
    isFront: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scale = size.minDimension / 400f
                
                // Draw Head
                drawBodyPart(
                    center = Offset(size.width / 2, 40 * scale),
                    partSize = Size(40 * scale, 45 * scale),
                    color = Color.Gray.copy(alpha = 0.2f)
                )

                if (isFront) {
                    // Front Muscles
                    drawMuscle(MuscleGroup.SHOULDERS, Offset(size.width / 2 - 60 * scale, 100 * scale), Size(40 * scale, 50 * scale), recoveryMap)
                    drawMuscle(MuscleGroup.SHOULDERS, Offset(size.width / 2 + 20 * scale, 100 * scale), Size(40 * scale, 50 * scale), recoveryMap)
                    
                    drawMuscle(MuscleGroup.CHEST, Offset(size.width / 2 - 45 * scale, 105 * scale), Size(40 * scale, 60 * scale), recoveryMap)
                    drawMuscle(MuscleGroup.CHEST, Offset(size.width / 2 + 5 * scale, 105 * scale), Size(40 * scale, 60 * scale), recoveryMap)
                    
                    drawMuscle(MuscleGroup.CORE, Offset(size.width / 2 - 25 * scale, 170 * scale), Size(50 * scale, 80 * scale), recoveryMap)
                    
                    drawMuscle(MuscleGroup.ARMS, Offset(size.width / 2 - 95 * scale, 130 * scale), Size(30 * scale, 90 * scale), recoveryMap)
                    drawMuscle(MuscleGroup.ARMS, Offset(size.width / 2 + 65 * scale, 130 * scale), Size(30 * scale, 90 * scale), recoveryMap)
                    
                    drawMuscle(MuscleGroup.LEGS, Offset(size.width / 2 - 50 * scale, 260 * scale), Size(45 * scale, 120 * scale), recoveryMap)
                    drawMuscle(MuscleGroup.LEGS, Offset(size.width / 2 + 5 * scale, 260 * scale), Size(45 * scale, 120 * scale), recoveryMap)
                } else {
                    // Back Muscles
                    drawMuscle(MuscleGroup.BACK, Offset(size.width / 2 - 55 * scale, 100 * scale), Size(110 * scale, 130 * scale), recoveryMap)
                    
                    drawMuscle(MuscleGroup.SHOULDERS, Offset(size.width / 2 - 70 * scale, 110 * scale), Size(35 * scale, 45 * scale), recoveryMap)
                    drawMuscle(MuscleGroup.SHOULDERS, Offset(size.width / 2 + 35 * scale, 110 * scale), Size(35 * scale, 45 * scale), recoveryMap)

                    drawMuscle(MuscleGroup.ARMS, Offset(size.width / 2 - 100 * scale, 140 * scale), Size(25 * scale, 80 * scale), recoveryMap)
                    drawMuscle(MuscleGroup.ARMS, Offset(size.width / 2 + 75 * scale, 140 * scale), Size(25 * scale, 80 * scale), recoveryMap)

                    drawMuscle(MuscleGroup.LEGS, Offset(size.width / 2 - 50 * scale, 260 * scale), Size(45 * scale, 120 * scale), recoveryMap)
                    drawMuscle(MuscleGroup.LEGS, Offset(size.width / 2 + 5 * scale, 260 * scale), Size(45 * scale, 120 * scale), recoveryMap)
                }
            }
        }
    }
}

fun DrawScope.drawBodyPart(center: Offset, partSize: Size, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - partSize.width / 2, center.y - partSize.height / 2),
        size = partSize,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
}

fun DrawScope.drawMuscle(
    group: MuscleGroup,
    topLeft: Offset,
    size: Size,
    recoveryMap: Map<MuscleGroup, Float>
) {
    val recovery = recoveryMap[group] ?: 1.0f
    val color = when {
        recovery < 0.33f -> Color(0xFFEF5350) // Red (Fatigued)
        recovery < 0.66f -> Color(0xFFFFCA28) // Amber (Moderate)
        else -> Color(0xFF66BB6A) // Green (Recovered)
    }
    
    // Abstract glow effect
    drawRoundRect(
        color = color.copy(alpha = 0.2f),
        topLeft = Offset(topLeft.x - 4, topLeft.y - 4),
        size = Size(size.width + 8, size.height + 8),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
    )
    
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )
}
