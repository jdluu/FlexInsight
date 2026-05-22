package com.jdluu.flexinsight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    secondary = PrimaryLight,
    onSecondary = Color.Black,
    tertiary = OrangeAccent,
    onTertiary = Color.Black,
    secondaryContainer = SurfaceCardAlt,
    onSecondaryContainer = TextSecondary,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceCard,
    onSurface = Color.White,
    surfaceVariant = SurfaceHighlight,
    onSurfaceVariant = TextSecondary,
    error = RedAccent,
    onError = Color.White,
    outline = Color(0x1AFFFFFF),
    outlineVariant = Color(0x0DFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = PrimaryDark,
    onSecondary = Color.White,
    tertiary = OrangeAccent,
    onTertiary = Color.White,
    secondaryContainer = SurfaceCardAltLight,
    onSecondaryContainer = Color(0xFF1E293B),
    background = BackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    error = RedAccent,
    onError = Color.White,
    outline = Color(0x1A000000),
    outlineVariant = Color(0x0D000000)
)

@Composable
fun FlexInsightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic colors for consistent design
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}