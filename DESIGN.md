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

<!-- impeccable:design-map -->
Name: FlexInsight — Calibrated Plate Rack
Colors: primary: cobalt-plate-light #1E4EC7; secondary: green-plate-light #19734B; tertiary: amber-plate-light #B45309; neutral: chalk-ground #F3F2EE; primary: on-cobalt-light #FFFFFF (+45 more in the sidecar)
Fonts: Barlow Condensed (display, headline); Schibsted Grotesk (title, body, label); Chivo Mono (mono-metric)
Spacing: xs 4px, sm 8px, md 12px, base 16px, lg 20px, xl 24px
Radius: extra-small 6px, small 10px, medium 14px, large 16px, extra-large 22px
North Star: "Calibrated Plate Rack"
Rules:
- Plate Color Law: semantic hues (`Color.kt`/`FlexHomeWidget.kt` only).
- Contrast-Paired Container: `on*Container` ink on `*ContainerLight` (`>= 4.5:1`).
- Tabular Lock: `tnum` numerals (`PlateNumeral*`/`MonoMetric*`).
- No-Kicker: no uppercase eyebrow kickers above titles.
- Single-Boundary: `1.dp` hairline border, `tonalElevation = 0.dp`.
- Scrim-Free Chrome: flush `1.dp` dividers, zero blur/gradients.
Don't:
- No nested bordered cards in `PlateCard`/`ExpandableExerciseCard`.
- No shadows, blur, or decorative gradients.
- No eyebrow kickers, raw hex in screens, or emojis for `Icons`.
Map: app/src/main/java/com/jdluu/flexinsight/ui/theme/Color.kt
Map: app/src/main/java/com/jdluu/flexinsight/ui/theme/Theme.kt
Map: app/src/main/res/font/
Map: app/src/main/java/com/jdluu/flexinsight/widget/FlexHomeWidget.kt
Map: app/src/main/java/com/jdluu/flexinsight/ui/theme/Shape.kt
Map: app/src/main/java/com/jdluu/flexinsight/ui/components/
Map: app/src/main/java/com/jdluu/flexinsight/ui/screens/platerack/parts/
Full: .impeccable/design-full.md (read a section when you need its detail)
