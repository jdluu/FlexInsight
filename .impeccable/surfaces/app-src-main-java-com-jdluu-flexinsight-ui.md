---
version: 1
slug: "app-src-main-java-com-jdluu-flexinsight-ui"
primary_target: "app/src/main/java/com/jdluu/flexinsight/ui"
related_targets: []
---

# FlexInsight Unified UI Redesign

- **Scope & Visitor Mode**: Native Android phone app (`minSdk = 26`, `targetSdk = 36`, Jetpack Compose + Material 3) plus Glance home-screen widget; **Operate** surface.
- **Audience & Job**: Strength and resistance-training athletes using a Hevy Pro account with an API key, checking composite recovery, next planned sets, weekly training load, PR history, and private on-device Gemini Nano coaching between gym sets and during post-session review.
- **Selected Structure**: 4-tab Material 3 `NavigationBar` (`Today`, `History`, `Planner`, `Coach`) with Profile/Settings reached from the TopAppBar without a conflicting Back arrow, and a new dedicated `Recovery` destination opened directly from the `Today` Readiness & Muscle Recovery card.
- **Core Constraints**: Strictly on-device AI (`AICore` / Gemini Nano) and optional local Health Connect; view-only safety default (`viewOnlyMode = true`); non-affiliated Hevy companion; dual System / Light / Dark themes; WCAG 2.1 AA contrast and 1.3x `font_scale` tolerance; `domain` >= 95% and `data/mapper` >= 90% coverage.

## Direction contract

- **THESIS**: **Calibrated Plate Rack** unifies every FlexInsight screen around the rapid, unmistakable legibility of calibrated IPF competition barbell plates and stamped gym iron—refusing generic glowing dark-mode fitness trackers and sterile clinical wellness apps alike.
- **OWN-WORLD**: Magnesium-chalk light (`#F3F2EE` ground, `#FFFFFF`/`#EAE8E1` tonal iron cards) and cast-iron dark (`#121316` ground, `#1B1D22`/`#252830` tonal surfaces), bound by 1dp machined hairline borders (`#D6D3CB` light / `#2D313B` dark), IPF competition accent roles (`#1E4EC7`/`#5B8BFF` cobalt primary, `#19734B`/`#34D399` recovered green, `#B45309`/`#F59E0B` amber fatigue, `#C62828`/`#F87171` strain crimson), `Barlow Condensed` display numerals, `Schibsted Grotesk` (or `Work Sans`) UI copy, and `Chivo Mono` tabular figures (`tnum`).
- **STORY**: Athletes glance once between sets to confirm composite readiness and recovered muscle groups, launch or inspect their next planned session, log soreness/mood in dedicated Recovery, review progressive overload in History/PRs, and consult private on-device AI Coach without fighting clipped bars or low-contrast dark text.
- **FIRST VIEWPORT**: Material 3 `TopAppBar` ("Today", last-sync status pill & Profile button) over: (1) Readiness hero strip showing composite score, plate-banded recovery capacity bar, and tap-through to `RecoveryScreen`; (2) Next Planned Session card with quick-jump to Planner/workout details; (3) Segmented weekly volume load plate bar and key metrics row; anchored by the 4-tab Material 3 `NavigationBar`. Signature move: calibrated horizontal IPF plate-band progress bars and registered fixed-scale MuscleHeatmap plates shared cleanly between Today and Recovery.
- **FORM**: Grounded candidate #1 (`Calibrated Plate Rack`, chosen by user over Roll #5 `Alpine Field Log`) · `seed:87f4b72214190bed`.
- **FINISH**: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
