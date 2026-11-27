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

FlexInsight is built for strength and resistance-training athletes checking composite readiness, next planned sessions, training load, PR history, and private on-device AI coaching between sets on Android phones. The visual system draws directly from calibrated IPF competition barbell plates and stamped gym iron: flat, matte surfaces anchored by crisp 1dp machined hairlines, tabular competition numerals, and color bands restricted to functional status.

Both Magnesium Chalk light (`ChalkGround` / `ChalkSurface`) and Cast Iron dark (`IronGround` / `IronSurface`) are first-class operating themes selectable via System, Light, or Dark preferences. Light mode stays crisp under bright gym skylights; dark mode reduces glare in dim garage or evening sessions. Generic glowing gradients, drop-shadow glass cards, decorative rainbow accents, and uppercase eyebrow kickers above headings are explicitly rejected across every surface.

**Key Characteristics:**
- Dual first-class chalk-light and cast-iron-dark material grounds tuned to WCAG 2.1 AA contrast (`>= 4.5:1` body text, `>= 3:1` large display).
- Flat tonal containers framed by 1dp machined hairline borders (`ChalkHairline` / `IronHairline`), zero drop shadows, and zero nested bordered cards.
- Strictly semantic IPF plate accents: Cobalt for primary interactive controls and AI coaching surfaces; Emerald Green, Warm Amber, and Crimson reserved solely for recovery readiness, moderate fatigue/deload alerts, and strain/error states.
- Three-voice bundled typography (`Barlow Condensed` for heavy lift numerals and screen titles, `Schibsted Grotesk` for UI copy, `Chivo Mono` with `tnum` for weights, timestamps, and set telemetry).

## Colors

All color tokens live in `app/src/main/java/com/jdluu/flexinsight/ui/theme/Color.kt` and resolve dynamically through `MaterialTheme.colorScheme` and `FlexTheme.colors` (`Theme.kt`), with dynamic wallpaper color disabled (`dynamicColor = false`) so plate-color meaning never shifts unpredictably.

### Primary
- **Cobalt Plate Light** (`#1E4EC7`) / **Cobalt Plate Dark** (`#5B8BFF`): Primary actions, active bottom navigation text, sync action icon, active filter highlights, and AI coaching indicators.
- **Cobalt Container Light** (`#DCE5FF`, on `#0C2464`) / **Cobalt Container Dark** (`#1A2C59`, on `#DCE5FF`): Active navigation indicator pill, on-device AI status pills, and primary-highlighted badges.

### Secondary
- **Green Plate Light** (`#19734B`, container `#D8F2E4` / `#0B3823`) / **Green Plate Dark** (`#34D399`, container `#12382B` / `#C7F7E3`): Recovered muscle readiness (`>= 70%`), PR markers, and sync success confirmation.

### Tertiary
- **Amber Plate Light** (`#B45309`, container `#FEF0D6` / `#4F2203`) / **Amber Plate Dark** (`#F59E0B`, container `#3E290A` / `#FDE3B0`): Moderate recovery (`35%–69%`), deload advisories, and view-only lock reminders.
- **Crimson Plate Light** (`#C62828`, container `#FCE2E2` / `#5F0E0E`) / **Crimson Plate Dark** (`#F87171`, container `#48191B` / `#FCD6D6`): High muscle strain (`< 35%`), offline/network banners, API/sync errors, and destructive clear-cache actions.

### Neutral
- **Magnesium Chalk Light Ground & Surfaces**: `ChalkGround` (`#F3F2EE`) screen background, `ChalkSurface` (`#FFFFFF`) elevated cards, `ChalkSurfaceAlt` (`#EAE8E1`) secondary tiles/pills, `ChalkSurfaceHighest` (`#E0DDD4`) calibrated bar tracks, `ChalkHairline` (`#D6D3CB`) 1dp dividers and card strokes, `ChalkOutline` (`#B3AFA4`) control outlines.
- **Cast Iron Dark Ground & Surfaces**: `IronGround` (`#121316`) screen background, `IronSurface` (`#1B1D22`) elevated cards, `IronSurfaceAlt` (`#252830`) secondary tiles/pills, `IronSurfaceHighest` (`#2F333D`) calibrated bar tracks, `IronHairline` (`#2D313B`) 1dp dividers and card strokes, `IronOutline` (`#484E5C`) control outlines.
- **Ink & Steel Typography**: `InkPrimary` (`#13151A`) / `InkSecondary` (`#4B505C`) / `InkMuted` (`#6B7180`) on light; `ChalkInk` (`#F3F2EE`) / `SteelSecondary` (`#A8AFBD`) / `SteelMuted` (`#818999`) on dark.

### Named Rules
**The Plate Color Law.** Plate hues (`Green`, `Amber`, `Crimson`) carry strictly diagnostic recovery, fatigue, or error meaning; never use semantic plate colors as decorative card accents, and never hardcode hex literals inside screen composables (the sole exception is the Glance `FlexHomeWidget`, which mirrors the exact Light/Dark `ColorProvider` pairs from `Color.kt`).

**The Contrast-Paired Container Rule.** Every light-mode status banner, pill, and icon container pairs dark ink (`on*Container`) on a high-lightness container (`*ContainerLight`) rather than raw white text on saturated amber or crimson, preserving `>= 4.5:1` readability in bright light.

## Typography

**Display Font:** `Barlow Condensed` (bundled `.ttf` weights `500`, `600`, `700` in `res/font/`)
**Body Font:** `Schibsted Grotesk` (bundled variable font weights `400`, `500`, `600`, `700`)
**Label/Mono Font:** `Chivo Mono` (bundled variable font weights `400`, `500`, `600`, `700` with `tnum`)

**Character:** Compressed competition-plate display lettering creates dense, unmistakable telemetry readouts (`44sp` readiness percentage, `30sp` weekly volume totals) while neutral neo-grotesque body copy and tabular monospace figures keep set logs, rep counts, and timestamps vertically aligned under `1.3x` system font scaling.

### Hierarchy
- **Display & Plate Numerals**: Material `displayLarge`–`displaySmall` (`52sp` / `40sp` / `32sp`, `Barlow Condensed Bold`, `tnum`) plus custom `PlateNumeralLarge` (`44sp`, `lineHeight = 46sp`), `PlateNumeralMedium` (`30sp`, `lineHeight = 32sp`), and `PlateNumeralSmall` (`22sp`, `SemiBold`, `lineHeight = 24sp`) for readiness hero percentages, PR weights, and summary metrics.
- **Headline**: `headlineLarge` (`28sp Bold`) and `headlineMedium` (`24sp SemiBold`) in `Barlow Condensed` for `TopAppBar` screen headers; `headlineSmall` (`20sp Bold`) in `Schibsted Grotesk` for modal/subsection headings.
- **Title**: `titleLarge` (`19sp Bold`), `titleMedium` (`16sp SemiBold`), and `titleSmall` (`14sp SemiBold`) in `Schibsted Grotesk` for card titles (`PlateCard`), section headers (`PlateSectionHeader`), and exercise rows.
- **Body**: `bodyLarge` (`15sp Normal`, `lineHeight = 22sp`), `bodyMedium` (`14sp Normal`, `lineHeight = 20sp`), and `bodySmall` (`12sp Normal`, `lineHeight = 16sp`) in `Schibsted Grotesk` for coach responses, descriptions, and workout notes.
- **Label & Mono Telemetry**: `labelLarge` (`14sp SemiBold`), `labelMedium` (`12sp Medium`), and `labelSmall` (`11sp SemiBold`) in `Schibsted Grotesk` for navigation tabs, chips, and buttons; `MonoMetricLarge` (`16sp SemiBold`), `MonoMetricMedium` (`14sp Medium`), and `MonoMetricSmall` (`12sp Medium`) in `Chivo Mono` (`tnum`) for set weights, dates, sync times, and percentages.

### Named Rules
**The Tabular Lock Rule.** Every numeric telemetry value—lift weights, volume totals, sync timestamps, recovery percentages, and exercise indices—uses `fontFeatureSettings = "tnum"` via `PlateNumeral*` or `MonoMetric*` so columns never jitter as values update.

**The No-Kicker Rule.** Screen headers and cards never stack small uppercase eyebrow kickers above titles. Headings carry their own weight via scale, bold contrast, and a `1dp` bottom divider (`TopAppBar` hairline).

## Layout

FlexInsight is a compact phone-first (`minSdk = 26`, `targetSdk = 36`) edge-to-edge Compose application wrapped in `MainActivity` (`enableEdgeToEdge()`) with a 4-destination Material 3 `NavigationBar`: **Today** (`Dashboard`), **History**, **Planner**, and **Coach** (`AITrainer`). Top-level destinations do not show a Back button; secondary flows (`Recovery`, `WorkoutDetail`, `HistoryAnalysis`, `PRList`, `Settings`) push above the bottom bar with a top-bar `ArrowBack` navigation icon that respects Android system Back and applies `.navigationBarsPadding()` and `.imePadding()`.

Screen-level vertical scrolling uses `LazyColumn` on a `4dp` base grid: `16.dp` outer horizontal margins on `PlateCard` items, `14.dp`–`20.dp` vertical spacing between top-level cards, `16.dp` internal card padding (`14.dp`–`20.dp` for dense list rows or onboarding cards), and `24.dp` horizontal margins for onboarding/hero padding. Every interactive element—including the Dashboard sync pill, `FilterChip` muscle selectors, `History` tab selector (`48.dp`), `ChatInput` send button (`48.dp`), and `ErrorBanner` dismiss/retry controls—meets Android's `48×48dp` minimum interactive touch target.

`FlexHomeWidget` (`widget/FlexHomeWidget.kt`) targets Android Home Screen Glance widgets (`14.dp` outer padding, `12dp×10dp` inner metric card) using synced `Day`/`Night` `ColorProvider` values identical to `Color.kt`.

## Elevation & Depth

FlexInsight uses **zero drop shadows and zero tonal surface tint elevation** (`tonalElevation = 0.dp` on `PlateCard` and `NavigationBar`). Depth comes strictly from step contrast between the screen ground (`ChalkGround` / `IronGround`), card surface (`ChalkSurface` / `IronSurface`), and recessed track containers (`ChalkSurfaceAlt` / `IronSurfaceAlt`), bounded by `1.dp` machined hairline strokes (`FlexTheme.colors.hairline`).

### Named Rules
**The Single-Boundary Rule.** Cards frame content once with `BorderStroke(1.dp, FlexTheme.colors.hairline)` at `tonalElevation = 0.dp`. Sub-regions inside a `PlateCard` separate using `HorizontalDivider(1.dp, FlexTheme.colors.hairline)` or unbordered tonal fills (`surfaceContainerLow`), never nested bordered cards.

**The Scrim-Free Chrome Rule.** `TopAppBar` and `FlexBottomNavigation` sit flush on the background/surface plane separated by a single `1.dp` hairline divider instead of blurred glass or gradient fades.

## Shapes

Corner geometry is defined centrally in `Shape.kt` (`FlexShapes`) and avoids soft oversized pill bubbles:

- **Extra Small (`6.dp`)**: `PlateStatusPill`, status tags, and legend swatches (with `3.dp` cells inside `CalibratedPlateBar`).
- **Small (`10.dp`)**: Primary/outlined `Button`s, `FilterChip`s, `Onboarding` text fields/icon badges, `History` tab indicator, and `WorkoutExercises` index tiles.
- **Medium (`14.dp`)**: `ErrorBanner` alerts, `ChatInput` field, and `EmptyState` icon container.
- **Large (`16.dp`)**: Standard `PlateCard` containers, workout rows, expandable exercise cards, and `ChatBubble` messages.
- **Extra Large (`22.dp`)**: Dialogs and large overlay containers.

## Components

Shared primitives reside in `app/src/main/java/com/jdluu/flexinsight/ui/components/` and `ui/screens/*/parts/`:

### Buttons
- **Shape:** Machined medium-tight corners (`10.dp` / `MaterialTheme.shapes.small`).
- **Primary:** Material `Button` with `primary` fill (`#1E4EC7` light / `#5B8BFF` dark), `onPrimary` label (`labelLarge`), `heightIn(min = 48.dp)`, and `0.dp` shadow elevation.
- **Secondary / Outlined / Ghost:** Material `OutlinedButton` with `1dp` `outline` border (`ChalkOutline` / `IronOutline`), and `TextButton` in `primary` cobalt for inline section actions (`PlateSectionHeader` trailing links, `ErrorBanner` retry).

### Chips
- **Style:** `PlateStatusPill` (`extraSmall` `6.dp` radius, `8dp × 4dp` padding, semantic `*Container` fill with `on*Container` `labelSmall` text, optional `13dp` leading Material icon) and Material 3 `FilterChip` (`small` `10.dp` radius, `8dp` color dot, `Chivo Mono` percentage readout, `1.5dp` `primary` border when selected).
- **State:** Unselected chips use `surfaceContainerLow` + `hairline` border; selected chips shift to `primaryContainer` / `onPrimaryContainer` + `1.5dp` `primary` border.

### Cards / Containers
- **Corner Style:** `16.dp` (`MaterialTheme.shapes.large`).
- **Background:** `MaterialTheme.colorScheme.surface` (`#FFFFFF` light / `#1B1D22` dark); `surfaceContainerLow` for `EmptyStateIllustration`; semantic `*Container` (`moderateContainer` / `primaryContainer.copy(0.55f)`) for deload and AI insight callouts.
- **Shadow Strategy:** `0.dp` elevation (flat-by-default).
- **Border:** Single `BorderStroke(1.dp, FlexTheme.colors.hairline)`.
- **Internal Padding:** `16.dp` default (`14.dp` compact list rows, `20.dp` onboarding cards), separated internally by `1.dp` `HorizontalDivider`.

### Inputs / Fields
- **Style:** `OutlinedTextField` (`10.dp` `small` radius) on Onboarding and Recovery notes; hairline-framed transparent `TextField` (`14.dp` `medium` radius) paired with a `48.dp` `FilledIconButton` send control in `ChatInput`.
- **Focus / Error:** Uses Material 3 `primary` border on focus and `error` (`CrimsonPlate*`) container/supporting text on validation failure.

### Navigation
- **Style:** `FlexBottomNavigation` renders a `1.dp` top `HorizontalDivider` above a flat (`tonalElevation = 0.dp`) Material 3 `NavigationBar` (`Today`, `History`, `Planner`, `Coach`), honoring `WindowInsets.navigationBars`. Selected items highlight with `primaryContainer` indicator pill, `Filled` Material icon (`onPrimaryContainer`), and `Bold` `primary` label; unselected items use `Outlined` Material icons and `onSurfaceVariant`.

### Signature Components
- **`CalibratedPlateBar`**: Segmented horizontal capacity bar (`3–10` cells, `3.dp` spacing, `3.dp` radius, `8–10dp` height) rendered over `FlexTheme.colors.chalkTrack` to visualize recovery capacity, weekly session goals, and onboarding step progress.
- **`MuscleHeatmap`**: Fixed-scale (`220×380` virtual design space) bilateral anatomical plate silhouette (`Front` / `Back`) drawn on Compose `Canvas` using semantic recovery fills (`>= 70%` green, `35%–69%` amber, `< 35%` crimson) and `2.5dp` `onSurface` highlight strokes for selected muscle groups.
- **`FlexHomeWidget`**: Glance app widget mirroring `ChalkGround`/`IronGround`, `ChalkSurface`/`IronSurface`, `CobaltPlate*`, and recovery plate semantics directly via `ColorProvider`.

## Do's and Don'ts

### Do:
- **Do** route every color through `MaterialTheme.colorScheme` or `FlexTheme.colors` (`Color.kt`) and verify contrast in both Light and Dark themes.
- **Do** format every numeric telemetry string with `PlateNumeral*` (`Barlow Condensed`, `tnum`) or `MonoMetric*` (`Chivo Mono`, `tnum`).
- **Do** enforce `48×48dp` minimum touch targets on every clickable pill, chip, tab, icon button, and banner action.
- **Do** separate subsections inside a `PlateCard` with `1.dp` `HorizontalDivider` or unbordered tonal `Box` fills (`surfaceContainerLow`).
- **Do** preserve `navigationBarsPadding()` and `imePadding()` on detail/full-screen destinations (`Recovery`, `WorkoutDetail`, `AITrainer`).

### Don't:
- **Don't** nest bordered `Surface` cards or bordered tiles inside a `PlateCard` or `ExpandableExerciseCard`.
- **Don't** add drop shadows (`tonalElevation > 0.dp`, `.shadow()`), glass blur, or decorative gradient washes behind text or icons.
- **Don't** add uppercase eyebrow kickers above section or screen headings.
- **Don't** hardcode `Color(0x...)` inside screen or component composables (only `Color.kt` and Glance `FlexHomeWidget.kt` define day/night palette literals).
- **Don't** use Unicode emojis or ad-hoc glyphs in place of Material `Icons.Outlined` / `Icons.Filled` vectors.
