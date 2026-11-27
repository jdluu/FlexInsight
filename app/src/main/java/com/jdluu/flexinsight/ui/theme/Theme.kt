package com.jdluu.flexinsight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FlexExtendedColors(
    val recovered: Color,
    val onRecovered: Color,
    val recoveredContainer: Color,
    val onRecoveredContainer: Color,
    val moderateFatigue: Color,
    val onModerateFatigue: Color,
    val moderateContainer: Color,
    val onModerateContainer: Color,
    val strained: Color,
    val onStrained: Color,
    val strainedContainer: Color,
    val onStrainedContainer: Color,
    val plateCobalt: Color,
    val plateGreen: Color,
    val plateAmber: Color,
    val plateCrimson: Color,
    val plateSteel: Color,
    val onPlateSteel: Color,
    val plateWhite: Color,
    val onPlateWhite: Color,
    val plateBlack: Color,
    val onPlateBlack: Color,
    val hairline: Color,
    val chalkTrack: Color
)

private val LightFlexExtendedColors = FlexExtendedColors(
    recovered = GreenPlateLight,
    onRecovered = OnGreenLight,
    recoveredContainer = GreenContainerLight,
    onRecoveredContainer = OnGreenContainerLight,
    moderateFatigue = AmberPlateLight,
    onModerateFatigue = OnAmberLight,
    moderateContainer = AmberContainerLight,
    onModerateContainer = OnAmberContainerLight,
    strained = CrimsonPlateLight,
    onStrained = OnCrimsonLight,
    strainedContainer = CrimsonContainerLight,
    onStrainedContainer = OnCrimsonContainerLight,
    plateCobalt = CobaltPlateLight,
    plateGreen = GreenPlateLight,
    plateAmber = AmberPlateLight,
    plateCrimson = CrimsonPlateLight,
    plateSteel = SteelPlateLight,
    onPlateSteel = OnSteelPlateLight,
    plateWhite = WhitePlateLight,
    onPlateWhite = OnWhitePlateLight,
    plateBlack = BlackPlateLight,
    onPlateBlack = OnBlackPlateLight,
    hairline = ChalkHairline,
    chalkTrack = ChalkSurfaceHighest
)

private val DarkFlexExtendedColors = FlexExtendedColors(
    recovered = GreenPlateDark,
    onRecovered = OnGreenDark,
    recoveredContainer = GreenContainerDark,
    onRecoveredContainer = OnGreenContainerDark,
    moderateFatigue = AmberPlateDark,
    onModerateFatigue = OnAmberDark,
    moderateContainer = AmberContainerDark,
    onModerateContainer = OnAmberContainerDark,
    strained = CrimsonPlateDark,
    onStrained = OnCrimsonDark,
    strainedContainer = CrimsonContainerDark,
    onStrainedContainer = OnCrimsonContainerDark,
    plateCobalt = CobaltPlateDark,
    plateGreen = GreenPlateDark,
    plateAmber = AmberPlateDark,
    plateCrimson = CrimsonPlateDark,
    plateSteel = SteelPlateDark,
    onPlateSteel = OnSteelPlateDark,
    plateWhite = WhitePlateDark,
    onPlateWhite = OnWhitePlateDark,
    plateBlack = BlackPlateDark,
    onPlateBlack = OnBlackPlateDark,
    hairline = IronHairline,
    chalkTrack = IronSurfaceHighest
)

val LocalFlexColors = staticCompositionLocalOf { LightFlexExtendedColors }

object FlexTheme {
    val colors: FlexExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFlexColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = CobaltPlateDark,
    onPrimary = OnCobaltDark,
    primaryContainer = CobaltContainerDark,
    onPrimaryContainer = OnCobaltContainerDark,
    secondary = GreenPlateDark,
    onSecondary = OnGreenDark,
    secondaryContainer = IronSurfaceAlt,
    onSecondaryContainer = ChalkInk,
    tertiary = AmberPlateDark,
    onTertiary = OnAmberDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = OnAmberContainerDark,
    background = IronGround,
    onBackground = ChalkInk,
    surface = IronSurface,
    onSurface = ChalkInk,
    surfaceVariant = IronSurfaceHighest,
    onSurfaceVariant = SteelSecondary,
    surfaceContainerLowest = IronGround,
    surfaceContainerLow = IronSurfaceAlt,
    surfaceContainer = IronSurface,
    surfaceContainerHigh = IronSurfaceHighest,
    surfaceContainerHighest = IronSurfaceHighest,
    error = CrimsonPlateDark,
    onError = OnCrimsonDark,
    errorContainer = CrimsonContainerDark,
    onErrorContainer = OnCrimsonContainerDark,
    outline = IronOutline,
    outlineVariant = IronHairline
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltPlateLight,
    onPrimary = OnCobaltLight,
    primaryContainer = CobaltContainerLight,
    onPrimaryContainer = OnCobaltContainerLight,
    secondary = GreenPlateLight,
    onSecondary = OnGreenLight,
    secondaryContainer = ChalkSurfaceAlt,
    onSecondaryContainer = InkPrimary,
    tertiary = AmberPlateLight,
    onTertiary = OnAmberLight,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = OnAmberContainerLight,
    background = ChalkGround,
    onBackground = InkPrimary,
    surface = ChalkSurface,
    onSurface = InkPrimary,
    surfaceVariant = ChalkSurfaceHighest,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = ChalkGround,
    surfaceContainerLow = ChalkSurfaceAlt,
    surfaceContainer = ChalkSurface,
    surfaceContainerHigh = ChalkSurfaceAlt,
    surfaceContainerHighest = ChalkSurfaceHighest,
    error = CrimsonPlateLight,
    onError = OnCrimsonLight,
    errorContainer = CrimsonContainerLight,
    onErrorContainer = OnCrimsonContainerLight,
    outline = ChalkOutline,
    outlineVariant = ChalkHairline
)

@Composable
fun FlexInsightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkFlexExtendedColors else LightFlexExtendedColors

    CompositionLocalProvider(LocalFlexColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = FlexShapes,
            content = content
        )
    }
}