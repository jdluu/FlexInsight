package com.example.hevyinsight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color(0xFF000000),
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = PrimaryLight,
    onSecondary = Color(0xFF000000),
    tertiary = OrangeAccent,
    onTertiary = Color(0xFF000000),
    background = BackgroundDark,
    onBackground = Color(0xFFFFFFFF),
    surface = SurfaceCard,
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = SurfaceHighlight,
    onSurfaceVariant = TextSecondary,
    error = RedAccent,
    onError = Color(0xFFFFFFFF),
    outline = Color(0x1AFFFFFF),
    outlineVariant = Color(0x0DFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color(0xFF000000),
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = PrimaryLight,
    onSecondary = Color(0xFF000000),
    tertiary = OrangeAccent,
    onTertiary = Color(0xFF000000),
    background = BackgroundLight,
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF666666),
    error = RedAccent,
    onError = Color(0xFFFFFFFF),
    outline = Color(0x1A000000),
    outlineVariant = Color(0x0D000000)
)

@Composable
fun HevyInsightTheme(
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