# DESIGN.md

Visual design system for FlexInsight — color, typography, spacing, shape, and component
conventions. Source of truth is `app/src/main/java/com/jdluu/flexinsight/ui/theme/` plus
`ui/components/`. For build/test rules see [AGENTS.md](AGENTS.md); for features and setup see
[README.md](README.md).

## Design direction

A dark, data-dense training console: near-black backgrounds, one saturated electric-blue accent,
generous corner radii, and numbers given visual weight through a geometric display face. Reference
screenshots are in [`screenshots/`](screenshots/).

Three principles hold across the app:

1. **Dark-first, light-supported.** Dark is the default and the design's home; light mode is a
   supported alternative, not an afterthought.
2. **Color carries meaning.** Blue is the app, amber and red are attention, green means recovered.
   Never decorative color.
3. **Stat-first hierarchy.** The number a user came for is the largest thing on screen.

## Color

Defined in `ui/theme/Color.kt`. The dark palette is a slate scale (`#030712` → `#384459`) with an
electric-blue primary; the light palette inverts to white surfaces with near-black text.

### Brand

| Token | Hex | Role |
| --- | --- | --- |
| `Primary` | `#2E5BFF` | Electric blue. Primary actions, active nav, data accents |
| `PrimaryDark` | `#0043CE` | Primary container (dark) |
| `PrimaryLight` | `#78A9FF` | Primary container (light), secondary |

`Primary` is the most-referenced color in the UI (52 uses) — the default accent for anything
interactive or highlighted.

### Surfaces

| Token | Hex | Use |
| --- | --- | --- |
| `BackgroundDark` | `#030712` | App background, dark |
| `BackgroundDarkAlt` / `SurfaceDark` | `#0F172A` | Secondary background |
| `BackgroundLight` | `#F8FAFC` | App background, light |
| `SurfaceCard` / `SurfaceVariant` | `#1E293B` | Card fill (dark) |
| `SurfaceCardAlt` | `#334155` | Secondary container, selected rows |
| `SurfaceHighlight` | `#384459` | Elevated / hover surface |
| `SurfaceCardAltLight` | `#F1F5F9` | Light-theme container fill |

### Text

| Token | Hex | Use |
| --- | --- | --- |
| `onSurface` | `#FFFFFF` dark / `#0F172A` light | Primary text |
| `TextSecondary` | `#94A3B8` | Labels, captions, supporting copy |
| `TextTertiary` | `#64748B` | Least-important text, timestamps |

### Semantic accents

| Token | Hex | Meaning | Uses |
| --- | --- | --- | --- |
| `RedAccent` | `#EF4444` | Error, destructive, PR, failure | 23 |
| `OrangeAccent` | `#FFA100` | Warning, deload alert, attention | 5 |
| `PurpleAccent` | `#8B5CF6` | Secondary highlight | 1 |
| `BlueAccent` | `#3B82F6` | Informational | 1 |

`PurpleAccent` and `BlueAccent` are barely used — a gap rather than a palette hole, so don't reach
for them by default. `Purple80`/`Pink80`/`Purple40` and friends in `Color.kt` are marked legacy;
don't use them in new UI.

### Status semantics

- **Error / destructive / personal record** → `RedAccent`
- **Warning, deload recommendation, sync failure** → `OrangeAccent`
- **AI and insight surfaces** → `Primary` gradients at low alpha

### Data-visualization scale

Charts, heatmaps, and progress rings use a fixed three-stop scale, not the brand accents —
green → amber → red maps to recovery state (`ui/components/MuscleHeatmap.kt:146`):

| Recovery | Color | Meaning |
| --- | --- | --- |
| `>= 0.66` | `#66BB6A` | Recovered |
| `0.33–0.66` | `#FFCA28` | Moderate |
| `< 0.33` | `#EF5350` | Fatigued |

These three are declared inline in `MuscleHeatmap.kt`, not in `Color.kt`. If another screen needs
the same scale, promote them to `Color.kt` rather than re-declaring the hexes.

### Theme wiring

`FlexInsightTheme` (`ui/theme/Theme.kt`) supplies both schemes to `MaterialTheme`. Dynamic color is
explicitly disabled (`dynamicColor = false`, and the parameter is ignored) so the brand palette
survives on Android 12+. Theme is user-selectable in Settings as System / Light / Dark, stored via
`UserPreferencesManager.themeFlow` and resolved in `MainActivity`.

## Typography

Defined in `ui/theme/Type.kt`. Two Google fonts loaded through Play Services
(`com.google.android.gms.fonts`) with a bundled certificate list.

| Family | Use |
| --- | --- |
| **Outfit** — geometric sans | Display and headline roles. Anything large or numeric |
| **Inter** — neutral sans | Title, body, and label roles. Anything read in running text |

The split is the core typographic idea: **Outfit carries emphasis, Inter carries information.**

| Material role | Font | Weight | Size / line height |
| --- | --- | --- | --- |
| `displayLarge` | Outfit | Bold | 57 / 64 |
| `displayMedium` | Outfit | Bold | 45 / 52 |
| `displaySmall` | Outfit | Bold | 36 / 44 |
| `headlineLarge` | Outfit | SemiBold | 32 / 40 |
| `headlineMedium` | Outfit | SemiBold | 28 / 36 |
| `headlineSmall` | Outfit | Medium | 24 / 32 |
| `titleLarge` | Outfit | Medium | 22 / 28 |
| `titleMedium` | Inter | Medium | 16 / 24 |
| `titleSmall` | Inter | Medium | 14 / 20 |
| `bodyLarge` | Inter | Normal | 16 / 24 |
| `bodyMedium` | Inter | Normal | 14 / 20 |
| `bodySmall` | Inter | Normal | 12 / 16 |
| `labelLarge` | Inter | Medium | 14 / 20 |
| `labelMedium` | Inter | Medium | 12 / 16 |
| `labelSmall` | Inter | Medium | 11 / 16 |

All sizes in `sp`. Outfit is loaded at Bold, SemiBold, and Medium; Inter at Medium and Normal.

## Spacing

A 4 dp base grid. Measured usage across `ui/`:

| Value | Uses | Role |
| --- | --- | --- |
| `16.dp` | 162 | Default screen and card padding — the workhorse |
| `8.dp` | 89 | Tight internal gaps, list item padding |
| `12.dp` | 78 | Element-to-element spacing |
| `4.dp` | 65 | Hairline gaps, icon-to-label |
| `20.dp` | 54 | Section separation |
| `24.dp` | 47 | Screen horizontal padding, major section breaks |
| `1.dp` | 39 | Dividers and borders |
| `2.dp` | 28 | Sub-pixel borders, progress track insets |
| `6.dp` | 26 | Badge and chip padding |
| `32.dp`+ | ~25 | Hero blocks and large empty states |

`1.dp` and `2.dp` are legitimate — dividers and borders, not spacing. Prefer `16.dp` as the default;
`24.dp` at screen edges, `8.dp` inside a card.

## Shape

Corner radii cluster on four values. There is no `Shapes` object — radii are passed inline to
`RoundedCornerShape`.

| Radius | Uses | Role |
| --- | --- | --- |
| `16.dp` | 24 | Cards and large containers — the default |
| `12.dp` | 24 | Inner elements, chips, list rows |
| `24.dp` | 16 | Hero surfaces, sheets, large panels |
| `8.dp` | 14 | Small controls, inputs, compact cards |
| `20.dp` | 8 | Mid-size containers |
| `4–6.dp` | 13 | Badges, tags, tiny chips |

Cards are `16.dp`; anything smaller than a card steps down to `12.dp` or `8.dp`.

## Elevation and depth

The dark palette builds depth through **surface lightness, not shadows** —
`SurfaceDark` → `SurfaceCard` → `SurfaceCardAlt` → `SurfaceHighlight` is the elevation ladder.
Shadows are not a primary depth cue; borders and tonal steps do the work.

## Gradients

Gradients are used sparingly and read from `MaterialTheme.colorScheme` rather than raw hex, so they
follow the active theme:

- `Brush.verticalGradient` — onboarding and bottom-nav surfaces (scrim/fade into background)
- `Brush.linearGradient` — AI surfaces: AI Trainer header, chat bubbles, planner insights, integration cards
- `Brush.radialGradient` — empty states and progress rings (glow behind an icon or focal point)

The radial glow behind an empty state or ring is a recurring motif — `EmptyState.kt:44`,
`HistoryStats.kt:307`, `PlannerCalendar.kt:105`.

## Components

Shared composables live in `ui/components/` and should be reused rather than rebuilt per screen.

| Component | Purpose |
| --- | --- |
| `BottomNavigation` | Top-level nav — Dashboard, History, AI Trainer, Planner, Profile — with a vertical-gradient scrim |
| `EmptyState` | Radially-glowed icon + title + body + optional action |
| `ErrorBanner` | Inline error surface, red at 15% alpha with a `RedAccent` border |
| `MuscleHeatmap` | Muscle-group recovery heatmap using the three-stop recovery scale |
| `NetworkStatusIndicator` | Offline badge (`RedAccent` at 20% alpha) |
| `SyncStatusIndicator` | Sync state pill; red on failure |
| `ShimmerEffect` | Linear-gradient loading shimmer |
| `Skeletons` | Placeholder shapes for loading states |

State conventions in `ui/common/State.kt`: `LoadingState` and `UiError` standardize loading and
error presentation across every screen. `ui/utils/UnitConverter.kt` handles Imperial/Metric
conversion, so units are a data concern, never a per-screen decision.

## Motion

Motion is functional. `ShimmerEffect` animates a linear gradient for indeterminate loading;
`MuscleHeatmap` and progress rings animate value changes. There is no shared motion spec — keep
transitions short and purposeful rather than expressive.

## Widget

`widget/FlexHomeWidget.kt` is Glance and **cannot use the Compose theme**. It hardcodes dark values:
`#0F172A` background, `#94A3B8` primary text, `#64748B` secondary. The widget is therefore
dark-only by construction. If the palette changes, update the widget in the same change.

## Accessibility

Measured WCAG 2.1 contrast ratios for the dark theme, computed from the token values above:

| Pairing | Ratio | Result |
| --- | --- | --- |
| `onSurface` white on `SurfaceCard` | 15.6 | AAA |
| `TextSecondary` on `BackgroundDark` | 7.9 | AAA |
| Black on `PrimaryLight` | 8.9 | AAA |
| White on `PrimaryDark` | 7.8 | AAA |
| `TextSecondary` on `SurfaceCard` | 5.7 | AA |
| White on `Primary` | 5.2 | AA |
| `Primary` on white | 5.2 | AA |
| Recovery green `#66BB6A` on `SurfaceCard` | 6.2 | AA |
| `TextTertiary` on `BackgroundDark` | 4.2 | AA-large |
| `RedAccent` on `SurfaceCard` | 3.9 | AA-large |

Dark theme is in good shape: body text passes AA everywhere, and only `TextTertiary` and large
accent text land in the AA-large band.

### Known accessibility gaps

Two measured failures, recorded not fixed.

1. **`TextSecondary` in light mode — 2.56:1, fails AA.** `TextSecondary` (`#94A3B8`) is hardcoded
   in **57 places** across `ui/` rather than read from `MaterialTheme.colorScheme`, so it does not
   adapt when a user selects the Light theme. On light surfaces it falls well below 4.5:1. Fix by
   routing these through `colorScheme.onSurfaceVariant` (already correctly `#64748B` in the light
   scheme).
2. **`onTertiary` in light mode — 2.03:1, fails AA.** `LightColorScheme` sets
   `onTertiary = Color.White` on `tertiary = OrangeAccent` (`#FFA100`). The dark scheme uses black
   and scores 10.4:1. Light's `onTertiary` should be black or a dark brown.

Related: `RedAccent` reaches only ~3.9:1 as text on card surfaces — acceptable for large text and
icons, not for body-size red text.

## Conventions for UI changes

- Reuse `ui/components/` before writing a new shared composable; if you need one twice, promote it.
- Take colors from `MaterialTheme.colorScheme` or the tokens in `Color.kt` — never inline a raw hex
  in a screen. Inline hexes currently exist and are the root cause of the light-mode failures above.
- Radii come from the 16/12/8 ladder. Default card padding `16.dp`, screen padding `24.dp`.
- A new status color means a new semantic token in `Color.kt` with a documented meaning, not a local hex.
- Outfit for display and stat values, Inter for body and labels.
- Check both themes. If a color is not theme-aware, say so in the PR.
- Touch targets: use Material defaults; the current UI relies on component padding rather than
  custom sizing.
