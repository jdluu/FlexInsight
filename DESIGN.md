---
name: "FlexInsight — Calibrated Plate Rack"
description: "Tactile, daylight- and gym-legible Android training console inspired by calibrated IPF competition barbell plates and stamped gym iron."
colors:
  chalk-ground: "#F3F2EE"
  chalk-surface: "#FFFFFF"
  chalk-surface-alt: "#EAE8E1"
  chalk-surface-highest: "#E0DDD4"
  chalk-hairline: "#D6D3CB"
  chalk-outline: "#B3AFA4"
  ink-primary: "#13151A"
  ink-secondary: "#4B505C"
  ink-muted: "#6B7180"
  iron-ground: "#121316"
  iron-surface: "#1B1D22"
  iron-surface-alt: "#252830"
  iron-surface-highest: "#2F333D"
  iron-hairline: "#2D313B"
  iron-outline: "#484E5C"
  chalk-ink: "#F3F2EE"
  steel-secondary: "#A8AFBD"
  steel-muted: "#818999"
  cobalt-plate-light: "#1E4EC7"
  on-cobalt-light: "#FFFFFF"
  cobalt-container-light: "#DCE5FF"
  on-cobalt-container-light: "#0C2464"
  cobalt-plate-dark: "#5B8BFF"
  on-cobalt-dark: "#08122B"
  cobalt-container-dark: "#1A2C59"
  on-cobalt-container-dark: "#DCE5FF"
  green-plate-light: "#19734B"
  on-green-light: "#FFFFFF"
  green-container-light: "#D8F2E4"
  on-green-container-light: "#0B3823"
  green-plate-dark: "#34D399"
  on-green-dark: "#06281B"
  green-container-dark: "#12382B"
  on-green-container-dark: "#C7F7E3"
  amber-plate-light: "#B45309"
  on-amber-light: "#FFFFFF"
  amber-container-light: "#FEF0D6"
  on-amber-container-light: "#4F2203"
  amber-plate-dark: "#F59E0B"
  on-amber-dark: "#291802"
  amber-container-dark: "#3E290A"
  on-amber-container-dark: "#FDE3B0"
  crimson-plate-light: "#C62828"
  on-crimson-light: "#FFFFFF"
  crimson-container-light: "#FCE2E2"
  on-crimson-container-light: "#5F0E0E"
  crimson-plate-dark: "#F87171"
  on-crimson-dark: "#340B0B"
  crimson-container-dark: "#48191B"
  on-crimson-container-dark: "#FCD6D6"
typography:
  display:
    fontFamily: "Barlow Condensed, sans-serif"
    fontSize: "44px"
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: "-0.01em"
    fontFeature: "tnum"
  headline:
    fontFamily: "Barlow Condensed, sans-serif"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: 1.17
    letterSpacing: "0em"
  title:
    fontFamily: "Schibsted Grotesk, sans-serif"
    fontSize: "16px"
    fontWeight: 600
    lineHeight: 1.38
    letterSpacing: "0em"
  body:
    fontFamily: "Schibsted Grotesk, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.47
    letterSpacing: "0.007em"
  label:
    fontFamily: "Schibsted Grotesk, sans-serif"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: 1.33
    letterSpacing: "0.02em"
  mono-metric:
    fontFamily: "Chivo Mono, monospace"
    fontSize: "14px"
    fontWeight: 500
    lineHeight: 1.29
    letterSpacing: "0em"
    fontFeature: "tnum"
rounded:
  extra-small: "6px"
  small: "10px"
  medium: "14px"
  large: "16px"
  extra-large: "22px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  base: "16px"
  lg: "20px"
  xl: "24px"
components:
  plate-card:
    backgroundColor: "{colors.iron-surface}"
    textColor: "{colors.chalk-ink}"
    rounded: "{rounded.large}"
    padding: "16px"
  button-primary:
    backgroundColor: "{colors.cobalt-plate-dark}"
    textColor: "{colors.on-cobalt-dark}"
    typography: "{typography.label}"
    rounded: "{rounded.small}"
    padding: "12px 16px"
    height: "48px"
  button-outlined:
    backgroundColor: "{colors.iron-surface}"
    textColor: "{colors.chalk-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.small}"
    padding: "12px 16px"
    height: "48px"
  status-pill:
    backgroundColor: "{colors.cobalt-container-dark}"
    textColor: "{colors.on-cobalt-container-dark}"
    typography: "{typography.label}"
    rounded: "{rounded.extra-small}"
    padding: "4px 8px"
  plate-bar:
    backgroundColor: "{colors.iron-surface-highest}"
    rounded: "{rounded.extra-small}"
    height: "10px"
  input-field:
    backgroundColor: "{colors.iron-surface}"
    textColor: "{colors.chalk-ink}"
    typography: "{typography.body}"
    rounded: "{rounded.medium}"
    padding: "12px 16px"
  bottom-nav:
    backgroundColor: "{colors.iron-surface}"
    textColor: "{colors.steel-secondary}"
    height: "80px"
---

# Design System: FlexInsight — Calibrated Plate Rack

## Overview

**Creative North Star: "Calibrated Plate Rack"**

FlexInsight is a phone-first Android training console inspired by calibrated IPF competition barbell plates and stamped gym iron: flat matte surfaces anchored by `1dp` machined hairlines, tabular competition numerals, and semantic color bands restricted to functional status. Both Magnesium Chalk light (`ChalkGround`, `ChalkSurface`) and Cast Iron dark (`IronGround`, `IronSurface`) are first-class themes (System, Light, or Dark).

**Key Characteristics:**
- Dual light and dark grounds tuned to WCAG 2.1 AA (`>= 4.5:1` body, `>= 3:1` display).
- Flat tonal containers framed once by `1dp` hairlines (`ChalkHairline`, `IronHairline`), zero shadows, zero nested bordered cards.
- Semantic IPF plate accents: Cobalt for primary controls and AI; Green, Amber, and Crimson strictly for recovery, moderate fatigue/deload, and strain/error states.
- Three-voice bundled typography (`Barlow Condensed`, `Schibsted Grotesk`, `Chivo Mono` with `tnum`).

## Colors

All color tokens live in `app/src/main/java/com/jdluu/flexinsight/ui/theme/Color.kt` and resolve via `MaterialTheme.colorScheme` and `FlexTheme.colors` in `app/src/main/java/com/jdluu/flexinsight/ui/theme/Theme.kt` (`dynamicColor = false` so plate-color meaning never shifts).

- **Primary** (`CobaltPlate*`, `CobaltContainer*`): Primary CTAs, active nav tab and pill, sync control, active filters, and on-device AI badges.
- **Secondary** (`GreenPlate*`, `GreenContainer*`): Recovered readiness (`>= 70%`), PR markers, and sync success.
- **Tertiary** (`AmberPlate*`, `AmberContainer*`): Moderate recovery (`35%–69%`), deload advisories, and view-only lock reminders.
- **Error & Strain** (`CrimsonPlate*`, `CrimsonContainer*`): High strain (`< 35%`), offline and sync errors, and destructive clear-cache actions.
- **Neutral & Sleeves**: `Chalk*` (light ground, cards, tracks, hairlines), `Iron*` (dark ground, cards, tracks, hairlines), `Ink*` and `Steel*` text hierarchy, plus `SteelPlate*`, `WhitePlate*`, and `BlackPlate*` for barbell sleeve discs.

**The Plate Color Law.** Semantic hues (`Green`, `Amber`, `Crimson`) carry strictly diagnostic recovery, fatigue, or error meaning (and IPF barbell sleeve colors in `BarbellSleeveDiagram`). Never hardcode `Color(0x...)` inside screens (only `Color.kt` and Glance `FlexHomeWidget` define day-night pairs).

**The Contrast-Paired Container Rule.** Light-mode banners, pills, and icon containers pair dark `on*Container` ink with high-lightness `*ContainerLight` fills instead of white text on saturated amber or crimson (`>= 4.5:1` in daylight).

## Typography

Bundled fonts live in `app/src/main/res/font/` and map in `app/src/main/java/com/jdluu/flexinsight/ui/theme/Type.kt`: `Barlow Condensed` (weights 500, 600, 700), `Schibsted Grotesk` (variable 400–700), and `Chivo Mono` (variable 400–700, `tnum`).

- **Display & Plate Numerals**: `displayLarge`–`displaySmall` (52sp, 40sp, 32sp `Barlow Condensed Bold`, `tnum`) plus `PlateNumeralLarge` (44sp), `PlateNumeralMedium` (30sp), and `PlateNumeralSmall` (22sp) for readiness percentages, PRs, and timers.
- **Headline**: `headlineLarge` (28sp) and `headlineMedium` (24sp) in `Barlow Condensed` for `TopAppBar` titles; `headlineSmall` (20sp Bold, `Schibsted Grotesk`) for modals and subsections.
- **Title, Body & Label**: `Schibsted Grotesk` (`titleLarge` 19sp, `titleMedium` 16sp, `titleSmall` 14sp; `bodyLarge` 15sp, `bodyMedium` 14sp, `bodySmall` 12sp; `labelLarge` 14sp, `labelMedium` 12sp, `labelSmall` 11sp).
- **Mono Telemetry**: `MonoMetricLarge` (16sp), `MonoMetricMedium` (14sp), and `MonoMetricSmall` (12sp) in `Chivo Mono` (`tnum`) for set weights, dates, sync timestamps, and plate weights.

**The Tabular Lock Rule.** Numeric telemetry uses `fontFeatureSettings = "tnum"` (`PlateNumeral*` and `MonoMetric*`) so figures stay column-aligned across updates and `1.3x` font scaling.

**The No-Kicker Rule.** Screen headers and cards never stack small uppercase eyebrow kickers above titles; headings lead directly above a `1dp` divider.

## Layout

Compact phone-first (`minSdk = 26`, `targetSdk = 36`) edge-to-edge layout in `MainActivity` with a 4-destination `FlexBottomNavigation` (**Today**, **History**, **Planner**, **Coach**). Secondary screens (`Recovery`, `WorkoutDetail`, `HistoryAnalysis`, `PRList`, `PlateRack`, `Settings`) push above the bar with `ArrowBack`, system Back support, `.navigationBarsPadding()`, and `.imePadding()`. Active rest timers dock via a flat `LiveRestTimerStrip` above the bottom bar (or pinned to navigation insets on secondary screens).

`LazyColumn` feeds follow a `4dp` grid (16dp outer and card padding, 14–20dp vertical spacing, 24dp onboarding and hero margins). Every interactive control (`StepButton`, `FilterChip`, tabs, sync pill, send button, banner actions) enforces `>= 48×48dp` touch targets. `FlexHomeWidget` (`app/src/main/java/com/jdluu/flexinsight/widget/FlexHomeWidget.kt`) mirrors `Color.kt` `Day` and `Night` `ColorProvider` tokens.

## Elevation & Depth

Zero drop shadows and zero tonal surface tint (`tonalElevation = 0.dp`). Depth comes strictly from ground to card to recessed track step contrast framed by `1dp` hairlines (`FlexTheme.colors.hairline`).

**The Single-Boundary Rule.** Frame cards once with `BorderStroke(1.dp, FlexTheme.colors.hairline)` at `tonalElevation = 0.dp`. Separate subsections via `HorizontalDivider(1.dp, hairline)` or unbordered tonal `surfaceContainerLow` fills, never nested bordered cards.

**The Scrim-Free Chrome Rule.** `TopAppBar`, `LiveRestTimerStrip`, and `FlexBottomNavigation` sit flush on the ground-and-surface plane behind `1dp` dividers with zero blur or gradient scrims.

## Shapes

Corner tokens live in `app/src/main/java/com/jdluu/flexinsight/ui/theme/Shape.kt` (`FlexShapes`): `extraSmall` (`PlateStatusPill`, tags; `3dp` cells in `CalibratedPlateBar`), `small` (buttons, `FilterChip`, `StepButton`, tabs, exercise tiles), `medium` (`ErrorBanner`, `ChatInput`, `BarbellSleeveDiagram`, `EmptyState`), `large` (`PlateCard`, workout rows, `ChatBubble`), `extraLarge` (dialogs).

## Components

Shared primitives live in `app/src/main/java/com/jdluu/flexinsight/ui/components/` and screen sections live in `app/src/main/java/com/jdluu/flexinsight/ui/screens/` (such as `app/src/main/java/com/jdluu/flexinsight/ui/screens/platerack/parts/`).

- **Buttons**: `small` radius, `>= 48dp` height, `0dp` shadow; `Button` (`primary` on `onPrimary`), `OutlinedButton` (`1dp` `outline`), inline `TextButton` (`primary`).
- **Chips**: `PlateStatusPill` (`extraSmall`, `8×4dp` padding, paired `*Container` colors) and M3 `FilterChip` (`small`, `surfaceContainerLow` with `hairline` unselected; `primaryContainer` with `1.5dp` `primary` border selected).
- **Cards**: `PlateCard` (`large` radius, `surface`, `0dp` elevation, `1dp` `hairline` stroke, `16dp` padding).
- **Inputs**: `OutlinedTextField` (`small` radius) for Onboarding, Recovery, and PlateRack; hairline-framed `TextField` (`medium` radius) plus `48dp` send button in `ChatInput`.
- **Navigation**: `FlexBottomNavigation` (`1dp` top divider, `tonalElevation = 0.dp`, `navigationBars` insets, `Filled` active and `Outlined` inactive Material icons).
- **Signature**: `CalibratedPlateBar` (3–12 `3dp` segments on `chalkTrack`), `BarbellSleeveDiagram` (38–96dp scaled competition plates), `MuscleHeatmap` (`220×380` bilateral front-and-back Canvas silhouette), and `FlexHomeWidget`.

## Do's and Don'ts

### Do:
- **Do** route every color through `MaterialTheme.colorScheme` or `FlexTheme.colors` (`Color.kt`) and verify Light + Dark contrast.
- **Do** format numeric telemetry with `PlateNumeral*` (`Barlow Condensed`, `tnum`) or `MonoMetric*` (`Chivo Mono`, `tnum`).
- **Do** enforce `>= 48×48dp` touch targets on every clickable pill, chip, tab, stepper, and icon button.
- **Do** separate subsections inside `PlateCard` using `1dp` `HorizontalDivider` or unbordered `surfaceContainerLow` fills.
- **Do** apply `navigationBarsPadding()` and `imePadding()` on full-screen detail destinations.

### Don't:
- **Don't** nest bordered `Surface` cards or bordered tiles inside `PlateCard` or `ExpandableExerciseCard`.
- **Don't** add drop shadows (`tonalElevation > 0.dp`, `.shadow()`), glass blur, or decorative gradient washes.
- **Don't** add uppercase eyebrow kickers above section or screen headings.
- **Don't** hardcode `Color(0x...)` inside screen or component composables (only `Color.kt` and `FlexHomeWidget.kt`).
- **Don't** use Unicode emojis in place of Material `Icons.Outlined` or `Icons.Filled` vectors.
