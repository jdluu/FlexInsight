package com.jdluu.flexinsight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.model.MuscleGroup
import com.jdluu.flexinsight.ui.theme.FlexTheme

/**
 * Calibrated Plate Rack anatomical silhouette displaying muscle recovery.
 * Uses fixed normalized coordinate registration so Dashboard previews and
 * dedicated Recovery breakdowns align at any viewport height without clipping.
 */
@Composable
fun MuscleHeatmap(
    recoveryMap: Map<MuscleGroup, Float>,
    modifier: Modifier = Modifier,
    figureHeight: Dp = 240.dp,
    selectedGroup: MuscleGroup? = null,
    showLegend: Boolean = false,
    onGroupSelected: ((MuscleGroup) -> Unit)? = null
) {
    val recoveredColor = FlexTheme.colors.recovered
    val moderateColor = FlexTheme.colors.moderateFatigue
    val strainedColor = FlexTheme.colors.strained
    val trackColor = FlexTheme.colors.chalkTrack
    val borderColor = FlexTheme.colors.hairline
    val selectedBorderColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SilhouetteView(
                title = stringResource(id = R.string.muscle_heatmap_front),
                recoveryMap = recoveryMap,
                isFront = true,
                figureHeight = figureHeight,
                selectedGroup = selectedGroup,
                recoveredColor = recoveredColor,
                moderateColor = moderateColor,
                strainedColor = strainedColor,
                headColor = trackColor,
                borderColor = borderColor,
                selectedBorderColor = selectedBorderColor,
                onGroupSelected = onGroupSelected,
                modifier = Modifier.weight(1f)
            )
            SilhouetteView(
                title = stringResource(id = R.string.muscle_heatmap_back),
                recoveryMap = recoveryMap,
                isFront = false,
                figureHeight = figureHeight,
                selectedGroup = selectedGroup,
                recoveredColor = recoveredColor,
                moderateColor = moderateColor,
                strainedColor = strainedColor,
                headColor = trackColor,
                borderColor = borderColor,
                selectedBorderColor = selectedBorderColor,
                onGroupSelected = onGroupSelected,
                modifier = Modifier.weight(1f)
            )
        }

        if (showLegend) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendSwatch(color = recoveredColor, label = "Ready 70%+")
                Spacer(modifier = Modifier.width(16.dp))
                LegendSwatch(color = moderateColor, label = "Moderate")
                Spacer(modifier = Modifier.width(16.dp))
                LegendSwatch(color = strainedColor, label = "Fatigued")
            }
        }
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SilhouetteView(
    title: String,
    recoveryMap: Map<MuscleGroup, Float>,
    isFront: Boolean,
    figureHeight: Dp,
    selectedGroup: MuscleGroup?,
    recoveredColor: Color,
    moderateColor: Color,
    strainedColor: Color,
    headColor: Color,
    borderColor: Color,
    selectedBorderColor: Color,
    onGroupSelected: ((MuscleGroup) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        val tapModifier = if (onGroupSelected != null) {
            Modifier.pointerInput(isFront, onGroupSelected) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val scale = minOf(w / 220f, h / 380f)
                    if (scale <= 0f) return@detectTapGestures
                    val cx = w / 2f
                    val topY = (h - 380f * scale) / 2f
                    val vx = (offset.x - cx) / scale
                    val vy = (offset.y - topY) / scale
                    hitTestMusclePlate(vx = vx, vy = vy, isFront = isFront)?.let(onGroupSelected)
                }
            }
        } else {
            Modifier
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(figureHeight)
                .then(tapModifier)
        ) {
            // Virtual design space: 220w x 380h centered horizontally
            val scale = minOf(size.width / 220f, size.height / 380f)
            val cx = size.width / 2f
            val topY = (size.height - 380f * scale) / 2f

            fun px(dx: Float, dy: Float) = Offset(cx + dx * scale, topY + dy * scale)
            fun sz(w: Float, h: Float) = Size(w * scale, h * scale)

            fun colorFor(group: MuscleGroup): Color {
                val fraction = recoveryMap[group] ?: 1.0f
                return when {
                    fraction < 0.35f -> strainedColor
                    fraction < 0.70f -> moderateColor
                    else -> recoveredColor
                }
            }

            fun strokeFor(group: MuscleGroup): Color =
                if (selectedGroup == group) selectedBorderColor else borderColor

            fun strokeWidthFor(group: MuscleGroup): Float =
                if (selectedGroup == group) 2.5f * scale else 1.2f * scale

            // Head & neck (neutral machined outline)
            drawRoundRect(
                color = headColor,
                topLeft = px(-17f, 12f),
                size = sz(34f, 40f),
                cornerRadius = CornerRadius(14f * scale, 14f * scale)
            )
            drawRoundRect(
                color = borderColor,
                topLeft = px(-17f, 12f),
                size = sz(34f, 40f),
                cornerRadius = CornerRadius(14f * scale, 14f * scale),
                style = Stroke(width = 1.2f * scale)
            )

            if (isFront) {
                // Shoulders (deltoid plates)
                drawPlateSegment(
                    topLeft = px(-64f, 64f),
                    size = sz(26f, 42f),
                    fill = colorFor(MuscleGroup.SHOULDERS),
                    stroke = strokeFor(MuscleGroup.SHOULDERS),
                    strokeWidth = strokeWidthFor(MuscleGroup.SHOULDERS),
                    radius = 10f * scale
                )
                drawPlateSegment(
                    topLeft = px(38f, 64f),
                    size = sz(26f, 42f),
                    fill = colorFor(MuscleGroup.SHOULDERS),
                    stroke = strokeFor(MuscleGroup.SHOULDERS),
                    strokeWidth = strokeWidthFor(MuscleGroup.SHOULDERS),
                    radius = 10f * scale
                )

                // Chest (twin pec plates)
                drawPlateSegment(
                    topLeft = px(-35f, 66f),
                    size = sz(33f, 48f),
                    fill = colorFor(MuscleGroup.CHEST),
                    stroke = strokeFor(MuscleGroup.CHEST),
                    strokeWidth = strokeWidthFor(MuscleGroup.CHEST),
                    radius = 8f * scale
                )
                drawPlateSegment(
                    topLeft = px(2f, 66f),
                    size = sz(33f, 48f),
                    fill = colorFor(MuscleGroup.CHEST),
                    stroke = strokeFor(MuscleGroup.CHEST),
                    strokeWidth = strokeWidthFor(MuscleGroup.CHEST),
                    radius = 8f * scale
                )

                // Core
                drawPlateSegment(
                    topLeft = px(-26f, 118f),
                    size = sz(52f, 84f),
                    fill = colorFor(MuscleGroup.CORE),
                    stroke = strokeFor(MuscleGroup.CORE),
                    strokeWidth = strokeWidthFor(MuscleGroup.CORE),
                    radius = 8f * scale
                )

                // Arms (biceps / forearms)
                drawPlateSegment(
                    topLeft = px(-82f, 110f),
                    size = sz(22f, 94f),
                    fill = colorFor(MuscleGroup.ARMS),
                    stroke = strokeFor(MuscleGroup.ARMS),
                    strokeWidth = strokeWidthFor(MuscleGroup.ARMS),
                    radius = 9f * scale
                )
                drawPlateSegment(
                    topLeft = px(60f, 110f),
                    size = sz(22f, 94f),
                    fill = colorFor(MuscleGroup.ARMS),
                    stroke = strokeFor(MuscleGroup.ARMS),
                    strokeWidth = strokeWidthFor(MuscleGroup.ARMS),
                    radius = 9f * scale
                )

                // Legs (quads & calves)
                drawPlateSegment(
                    topLeft = px(-40f, 208f),
                    size = sz(36f, 152f),
                    fill = colorFor(MuscleGroup.LEGS),
                    stroke = strokeFor(MuscleGroup.LEGS),
                    strokeWidth = strokeWidthFor(MuscleGroup.LEGS),
                    radius = 11f * scale
                )
                drawPlateSegment(
                    topLeft = px(4f, 208f),
                    size = sz(36f, 152f),
                    fill = colorFor(MuscleGroup.LEGS),
                    stroke = strokeFor(MuscleGroup.LEGS),
                    strokeWidth = strokeWidthFor(MuscleGroup.LEGS),
                    radius = 11f * scale
                )
            } else {
                // Rear Shoulders
                drawPlateSegment(
                    topLeft = px(-64f, 64f),
                    size = sz(24f, 40f),
                    fill = colorFor(MuscleGroup.SHOULDERS),
                    stroke = strokeFor(MuscleGroup.SHOULDERS),
                    strokeWidth = strokeWidthFor(MuscleGroup.SHOULDERS),
                    radius = 10f * scale
                )
                drawPlateSegment(
                    topLeft = px(40f, 64f),
                    size = sz(24f, 40f),
                    fill = colorFor(MuscleGroup.SHOULDERS),
                    stroke = strokeFor(MuscleGroup.SHOULDERS),
                    strokeWidth = strokeWidthFor(MuscleGroup.SHOULDERS),
                    radius = 10f * scale
                )

                // Back (traps + lats trapezoid plate)
                val backPath = Path().apply {
                    moveTo(cx - 38f * scale, topY + 66f * scale)
                    lineTo(cx + 38f * scale, topY + 66f * scale)
                    lineTo(cx + 26f * scale, topY + 196f * scale)
                    lineTo(cx - 26f * scale, topY + 196f * scale)
                    close()
                }
                drawPath(path = backPath, color = colorFor(MuscleGroup.BACK))
                drawPath(
                    path = backPath,
                    color = strokeFor(MuscleGroup.BACK),
                    style = Stroke(width = strokeWidthFor(MuscleGroup.BACK))
                )

                // Arms (triceps)
                drawPlateSegment(
                    topLeft = px(-82f, 108f),
                    size = sz(22f, 94f),
                    fill = colorFor(MuscleGroup.ARMS),
                    stroke = strokeFor(MuscleGroup.ARMS),
                    strokeWidth = strokeWidthFor(MuscleGroup.ARMS),
                    radius = 9f * scale
                )
                drawPlateSegment(
                    topLeft = px(60f, 108f),
                    size = sz(22f, 94f),
                    fill = colorFor(MuscleGroup.ARMS),
                    stroke = strokeFor(MuscleGroup.ARMS),
                    strokeWidth = strokeWidthFor(MuscleGroup.ARMS),
                    radius = 9f * scale
                )

                // Posterior Legs (glutes / hamstrings / calves)
                drawPlateSegment(
                    topLeft = px(-40f, 204f),
                    size = sz(36f, 156f),
                    fill = colorFor(MuscleGroup.LEGS),
                    stroke = strokeFor(MuscleGroup.LEGS),
                    strokeWidth = strokeWidthFor(MuscleGroup.LEGS),
                    radius = 11f * scale
                )
                drawPlateSegment(
                    topLeft = px(4f, 204f),
                    size = sz(36f, 156f),
                    fill = colorFor(MuscleGroup.LEGS),
                    stroke = strokeFor(MuscleGroup.LEGS),
                    strokeWidth = strokeWidthFor(MuscleGroup.LEGS),
                    radius = 11f * scale
                )
            }
        }
    }
}

private fun DrawScope.drawPlateSegment(
    topLeft: Offset,
    size: Size,
    fill: Color,
    stroke: Color,
    strokeWidth: Float,
    radius: Float
) {
    val cornerRadius = CornerRadius(radius, radius)
    drawRoundRect(
        color = fill,
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        color = stroke,
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = strokeWidth)
    )
}

private fun hitTestMusclePlate(vx: Float, vy: Float, isFront: Boolean): MuscleGroup? {
    fun inRect(x: Float, y: Float, w: Float, h: Float) =
        vx in x..(x + w) && vy in y..(y + h)

    return if (isFront) {
        when {
            inRect(-64f, 64f, 26f, 42f) || inRect(38f, 64f, 26f, 42f) -> MuscleGroup.SHOULDERS
            inRect(-35f, 66f, 33f, 48f) || inRect(2f, 66f, 33f, 48f) -> MuscleGroup.CHEST
            inRect(-26f, 118f, 52f, 84f) -> MuscleGroup.CORE
            inRect(-82f, 110f, 22f, 94f) || inRect(60f, 110f, 22f, 94f) -> MuscleGroup.ARMS
            inRect(-40f, 208f, 36f, 152f) || inRect(4f, 208f, 36f, 152f) -> MuscleGroup.LEGS
            else -> null
        }
    } else {
        when {
            inRect(-64f, 64f, 24f, 40f) || inRect(40f, 64f, 24f, 40f) -> MuscleGroup.SHOULDERS
            vy in 66f..196f && kotlin.math.abs(vx) <= (38f - 12f * ((vy - 66f) / 130f)) -> MuscleGroup.BACK
            inRect(-82f, 108f, 22f, 94f) || inRect(60f, 108f, 22f, 94f) -> MuscleGroup.ARMS
            inRect(-40f, 204f, 36f, 156f) || inRect(4f, 204f, 36f, 156f) -> MuscleGroup.LEGS
            else -> null
        }
    }
}
