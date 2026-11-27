# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Strength and resistance-training athletes using a [Hevy Pro](https://www.hevyapp.com/) account with a developer API key, checking progress on Android phones during gym sessions (rest periods, check-ins) and post-workout review.

## Product Purpose

An independent Android companion to Hevy that turns synced workout, routine, and exercise history into actionable training analytics, weekly planning, composite muscle recovery tracking, and private on-device AI coaching — plus a Glance home-screen widget.

## Positioning

Local-first, privacy-respecting companion backed by Room persistence, optional Health Connect biometrics/cardio context, and on-device Gemini Nano (`AICore`) inference so coaching and workout context stay on the user's phone with zero FlexInsight cloud backend.

## Operating Context

- **Hardware target**: Android phones (`minSdk = 26`, `targetSdk = 36`, Jetpack Compose + Material 3) plus a Glance home-screen widget; tablets/foldables and Wear OS are out of scope.
- **Core surfaces**: Onboarding, Dashboard, History (+ History Analysis & PR List), Workout Detail, Planner, dedicated Recovery (surfacing composite recovery score, soreness/mood logging, and muscle heatmap), Plate Rack & Rest Timer (`PlateRackScreen` + persistent `LiveRestTimerStrip`), AI Trainer, Settings/Profile, and Home Widget.
- **Gym-floor & offline usage**: Works offline against cached Room data when gym reception drops; syncs incrementally with `https://api.hevyapp.com` in the background via WorkManager.

## Capabilities and Constraints

- **Hevy integration**: Reads workouts, workout counts, incremental workout events, exercise templates, exercise history, routines, and routine folders; currently writes only new routines via `POST /v1/routines`.
- **View-only safety default**: FlexInsight ships view-only by default (`viewOnlyMode = true`) so users keep Hevy as their primary workout editor unless they explicitly unlock writes in Settings.
- **On-device AI**: Powered by Gemini Nano through Android AICore (`ML Kit GenAI Prompt API`); requires compatible physical Android hardware (emulators cannot run AICore; debug builds include stub mode).
- **Optional Health Connect**: Reads sleep, heart rate, steps, active calories, and non-strength exercise sessions locally, with optional user-enabled write-back of completed strength workouts.
- **Dual-theme support**: User-selectable System / Light / Dark themes stored in DataStore.
- **Repository boundary**: Business math stays in pure Kotlin `domain/calc/` (`domain` >= 95% and `data/mapper` >= 90% JaCoCo coverage floors).

## Brand Commitments

- **Product name**: FlexInsight.
- **Privacy-first commitment**: Keep on-device Gemini Nano AI and optional Health Connect processing strictly local, with no FlexInsight cloud account or server storage.
- **Non-affiliation**: Independent, unofficial Hevy companion (not affiliated with, endorsed by, or sponsored by Hevy).

## Evidence on Hand

- Existing app screenshots in `screenshots/dashboard.png`, `screenshots/planner.png`, and `screenshots/profile.png`.
- Endpoint surface documented in `docs/HevyAPI.md` and `core/data/src/main/java/com/jdluu/flexinsight/data/api/FlexApiService.kt`.
- Recovery mood/notes preferences already persisted in `UserPreferencesManager` (`recoveryMoodFlow`, `recoveryNotesFlow`) and localized strings in `app/src/main/res/values/strings.xml`.
- Undecided / unimplemented gaps tracked outside `PRODUCT.md`: whether local workout completion/rescheduling (`needsSync`) should push via `PUT /v1/workouts/{workoutId}` or remain strictly local.

## Product Principles

1. **Glanceable under gym conditions**: Athletes scan load, readiness, PRs, and next sets in seconds between sets.
2. **Local truth & privacy first**: User workout and health data stays on-device; AI never ships personal training logs to a FlexInsight server.
3. **Non-destructive companion**: Never mutate or surprise the user's Hevy account without explicit opt-in.
4. **Honest data states**: Surface offline status, sync freshness, missing API keys, and device AI/Health Connect readiness transparently without faking data.

## Accessibility & Inclusion

- Meet WCAG 2.1 AA contrast across both Dark and Light themes and respect Android `font_scale` (1.3x+) and 48dp minimum touch targets.
