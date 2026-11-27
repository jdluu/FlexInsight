package com.jdluu.flexinsight.ui.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.ui.theme.FlexTheme

@Composable
fun rememberReducedMotion(): Boolean {
    if (LocalInspectionMode.current) return true
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Calibrated Plate Rack primary container card: flat tonal iron/chalk surface
 * framed by a 1dp machined hairline border without drop shadows or nested elevation clutter.
 */
@Composable
fun PlateCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = FlexTheme.colors.hairline,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(clickableModifier),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Calibrated IPF horizontal plate-band capacity / progress bar.
 * Displays discrete calibrated segments with crisp 1dp machined spacing.
 */
@Composable
fun CalibratedPlateBar(
    progress: Float,
    modifier: Modifier = Modifier,
    segments: Int = 10,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = FlexTheme.colors.chalkTrack,
    barHeight: Dp = 10.dp
) {
    val reducedMotion = rememberReducedMotion()
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(durationMillis = 360, easing = FastOutSlowInEasing)
        },
        label = "calibrated_plate_bar_progress"
    )
    val filledExact = animatedProgress * segments

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until segments) {
            val fillFraction = (filledExact - index).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(trackColor)
            ) {
                if (fillFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillFraction)
                            .background(activeColor)
                    )
                }
            }
        }
    }
}

/**
 * Calibrated status chip (e.g., Ready, Moderate, Strain, On-device AI, PR).
 */
@Composable
fun PlateStatusPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color? = null
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
        border = borderColor?.let { BorderStroke(1.dp, it) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = contentColor
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Clean section header without uppercase eyebrow kickers above titles.
 */
@Composable
fun PlateSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingActionLabel: String? = null,
    onTrailingAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (trailingActionLabel != null && onTrailingAction != null) {
            TextButton(
                onClick = onTrailingAction,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = trailingActionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Resolve calibrated recovery semantic palette from 0f..1f fraction.
 */
@Composable
fun recoveryColorFor(fraction: Float): Color = when {
    fraction < 0.35f -> FlexTheme.colors.strained
    fraction < 0.70f -> FlexTheme.colors.moderateFatigue
    else -> FlexTheme.colors.recovered
}

@Composable
fun recoveryContainerFor(fraction: Float): Pair<Color, Color> = when {
    fraction < 0.35f -> FlexTheme.colors.strainedContainer to FlexTheme.colors.onStrainedContainer
    fraction < 0.70f -> FlexTheme.colors.moderateContainer to FlexTheme.colors.onModerateContainer
    else -> FlexTheme.colors.recoveredContainer to FlexTheme.colors.onRecoveredContainer
}
