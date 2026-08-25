# FlexInsight

[![Get it on Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fadd%2Fhttps%3A%2F%2Fgithub.com%2Fjdluu%2FFlexInsight)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Gemini Nano](https://img.shields.io/badge/Gemini%20Nano-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)
![Room](https://img.shields.io/badge/Room-FF6F00?style=for-the-badge&logo=sqlite&logoColor=white)

Android companion app for [Hevy](https://www.hevyapp.com/) that adds analytics, workout planning, recovery tracking, and on-device AI coaching powered by Gemini Nano.

## Features

- **Dashboard** — recent workouts, volume trends, training load, recovery preview, sync status
- **History** — filtering, comparisons, PR tracking, routine diff, analysis
- **Planner** — calendar view, rescheduling, AI plans, routine export to Hevy
- **Recovery** — composite recovery score (Hevy + optional Health Connect), soreness logging, muscle heatmap
- **AI Trainer** — on-device coaching via ML Kit GenAI (Gemini Nano), grounded in your Hevy data
- **Health Connect** — optional sleep, heart rate, steps, and exercise data for richer recovery and coaching
- **Background sync** — incremental sync with Hevy via WorkManager

## Requirements

- Android 8.0+ (API 26) for the core app
- A [Hevy Pro](https://www.hevyapp.com/) account with a developer API key (Settings → Developer)
- **AI Trainer**: device with Gemini Nano / AICore support (Pixel 8+, Galaxy S24+, etc.) on a **physical device**. Emulators cannot run AICore; debug builds offer **Debug AI UI stubs** in Settings to click through the UI with placeholder text, not real Nano inference.
- **Health Connect** (optional): [Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect) app installed on the device

## Setup

1. Clone the repository.
2. Open in Android Studio (Ladybug or newer recommended).
3. Build and run on a device or emulator:

```bash
./gradlew assembleDebug
```

4. On first launch, complete onboarding: Hevy API key → sync → optional Health Connect → AI check.

## Permissions and data access

FlexInsight asks only for what it needs. Nothing below is requested until you use the related feature (except network access, which the app needs to talk to Hevy).

### Network (always declared)

| Permission | Why |
|------------|-----|
| `INTERNET` | Sync workouts, routines, and exercise templates from the Hevy API using **your** API key. |
| `ACCESS_NETWORK_STATE` | Show when you are offline and avoid unnecessary sync attempts. |

No location, contacts, camera, microphone, or SMS permissions are used.

### Hevy API key (you provide it)

- Stored locally in **encrypted** preferences on your device.
- Sent only to `https://api.hevyapp.com` when syncing or loading workout data.
- FlexInsight does not operate a backend that stores your workouts; data lives in local Room cache + Hevy’s servers.

### Health Connect (optional — only if you enable it in Settings or onboarding)

Before Android shows the system permission sheet, the app shows an in-app list of exactly what we request and why. You can also open **Settings → What does Health Connect access?** anytime.

| Access | What we read or write | Why |
|--------|----------------------|-----|
| **Read sleep** | Last night’s sleep duration | Recovery score, training load, deload hints, AI coaching context. |
| **Read heart rate** | Resting heart rate samples | Recovery and coaching context (not for medical diagnosis). |
| **Read steps** | Step count for today | Daily activity context for training load. |
| **Read active calories** | Active calories burned today | Non-gym activity alongside Hevy volume. |
| **Read exercise** | Exercise sessions from other apps | Count cardio / non-Hevy workouts in weekly load. |
| **Write exercise** | Strength workout sessions | **Optional**, only if you turn on “Write workouts to Health Connect” and view-only mode is off — copies completed Hevy sessions into your Health Connect timeline (e.g. Google Fit / Samsung Health). |

Health Connect data is processed **on device** for Recovery, Dashboard, and AI prompts. It is not uploaded to FlexInsight servers.

### On-device AI

- Gemini Nano runs locally on supported phones; chat prompts are not sent to a FlexInsight cloud service.
- Coaching uses synced Hevy data (and Health Connect summaries if enabled) injected into the prompt on your phone.
- **Testing:** validate AI on a real device with Nano/AICore. The debug-only **Debug AI UI stubs** toggle skips availability checks and may return canned text when inference fails — it is not a substitute for device testing.

## Architecture

```
UI (Compose) → ViewModels → Use Cases → Repositories → Room / Hevy API
                                              ↓
                                    WorkManager (periodic sync)
```

- **DI**: Hilt
- **Local storage**: Room
- **Networking**: Retrofit 3 + OkHttp
- **API key**: Encrypted SharedPreferences (migrates from legacy DataStore automatically)

## Hevy API

See [docs/HevyAPI.md](docs/HevyAPI.md) for endpoint documentation used by this project.

## AI Trainer and Hevy data

The AI Trainer does **not** use MCP or cloud function calling (on-device Gemini Nano does not support that today). Instead it uses a **Hevy data pipeline**:

1. **Sync** — Workouts sync from the Hevy API into Room (on app launch, periodic background sync, and when opening AI Trainer).
2. **Context build** — `HevyAiDataAccessor` assembles profile, recent sessions, PRs, volume trends, muscle recovery, routines, planned workouts, and optional Health Connect summaries into the system prompt.
3. **Query-aware API calls** — When you ask about a specific lift (e.g. “bench press”), the app calls Hevy’s `GET /v1/exercise_history/{id}` and injects that history into the prompt for that turn.

Context is **refreshed on every message** so answers stay aligned with your latest synced data.

## Hevy edits (view-only by default)

FlexInsight is **view-only by default** so you can keep editing workouts in the Hevy app. In **Settings → View-only mode**, turn this **off** to allow FlexInsight to:

- Mark planned workouts complete
- Reschedule workouts
- Save AI-generated routines to Hevy

## Development

- **Package / application ID:** `com.jdluu.flexinsight`
- **Room migrations:** SQL in `FlexDatabaseMigrations.kt`; checked-in schemas in `app/schemas/`. KSP `exportSchema` stays off (Room 2.8 + kotlinx-serialization mismatch on this toolchain). Migration correctness is covered by `FlexDatabaseMigrationTest` (instrumented) against those JSON files.

```bash
./gradlew test                    # unit tests (parser, matcher, sync, stats, SyncPreferencesManager via Robolectric)
./gradlew connectedDebugAndroidTest   # Room migration test (requires device/emulator)
./gradlew assembleDebug           # debug APK
./gradlew assembleRelease         # release APK (R8 minification enabled)
```

HTTP request/response bodies are logged only in **debug** builds.

## Third-party notices

- **Hevy** is a trademark of its respective owner. FlexInsight is an independent, unofficial companion and is **not affiliated with, endorsed by, or sponsored by Hevy**.
- Workout data is accessed only via the user’s own Hevy API key; users must comply with [Hevy’s terms](https://www.hevyapp.com/) and API usage policies.
- **Gemini Nano** / **ML Kit GenAI** are Google services; availability varies by device and region.
- **Health Connect** is provided by Google on supported Android devices; permission grants are managed through the Health Connect app.

## License

This project is licensed under the **[MIT License](LICENSE)**.

```
Copyright (c) 2025 jdluu
SPDX-License-Identifier: MIT
```

You may use, modify, and distribute this software under the terms in [LICENSE](LICENSE). The full license text must be included in copies or substantial portions of the Software.

**Disclaimer:** The software is provided “as is”, without warranty of any kind. See [LICENSE](LICENSE) for details.
