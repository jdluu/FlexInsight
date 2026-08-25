# FlexInsight

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Gemini Nano](https://img.shields.io/badge/Gemini%20Nano-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)
![Room](https://img.shields.io/badge/Room-FF6F00?style=for-the-badge&logo=sqlite&logoColor=white)

Android companion app for [Hevy](https://www.hevyapp.com/) that adds analytics,
workout planning, recovery tracking, and on-device AI coaching powered by
Gemini Nano. It is an independent, unofficial companion and is **not affiliated
with, endorsed by, or sponsored by Hevy**.

## Features

- **Dashboard** — recent workouts, volume trends, training load, recovery preview, sync status
- **History** — filtering, comparisons, PR tracking, routine diff, analysis
- **Planner** — calendar view, rescheduling, AI plans, routine export to Hevy
- **Recovery** — composite recovery score (Hevy + optional Health Connect), soreness logging, muscle heatmap
- **AI Trainer** — on-device coaching via ML Kit GenAI (Gemini Nano), grounded in your Hevy data.
  Works via a Hevy data pipeline: workouts sync into Room, `HevyAiDataAccessor` assembles profile and
  recent sessions into the prompt, and query-aware API calls fetch exercise history on demand.
  Context is refreshed on every message.
- **Health Connect** — optional sleep, heart rate, steps, and exercise data for richer recovery and coaching
- **Background sync** — incremental sync with Hevy via WorkManager

FlexInsight is **view-only by default** so you can keep editing workouts in the Hevy app. In
**Settings → View-only mode**, turn this **off** to allow marking planned workouts complete,
rescheduling, and saving AI-generated routines to Hevy.

**Requirements:** Android 8.0+ (API 26). A [Hevy Pro](https://www.hevyapp.com/) account with a
developer API key (Settings → Developer). AI Trainer requires a physical device with Gemini Nano
/ AICore support (Pixel 8+, Galaxy S24+, etc.) — emulators cannot run AICore. Health Connect is
optional.

## Installation

### Obtainium

[![Get it on Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fadd%2Fhttps%3A%2F%2Fgithub.com%2Fjdluu%2FFlexInsight)

Tap the badge to add FlexInsight to [Obtainium](https://github.com/ImranR98/Obtainium), which
installs it straight from this repository's releases and keeps it up to date automatically.

### Manual APK

Download the latest APK from the [Releases page](https://github.com/jdluu/FlexInsight/releases)
and install it on your device.

### Setup

1. Clone the repository.
2. Open in Android Studio (Ladybug or newer recommended).
3. Build and run on a device or emulator:
   ```bash
   ./gradlew assembleDebug
   ```
4. On first launch, complete onboarding: Hevy API key → sync → optional Health Connect → AI check.

## Privacy

FlexInsight asks only for what it needs. Nothing is requested until you use the related feature
(except network access, which the app needs to talk to Hevy). No location, contacts, camera,
microphone, or SMS permissions are used.

- **Network:** `INTERNET` syncs workouts, routines, and exercise templates from the Hevy API using
  **your** API key. `ACCESS_NETWORK_STATE` shows when you are offline and avoids unnecessary sync
  attempts.
- **Hevy API key:** Stored locally in **encrypted** preferences on your device. Sent only to
  `https://api.hevyapp.com` when syncing or loading workout data. FlexInsight does not operate a
  backend that stores your workouts; data lives in local Room cache + Hevy's servers.
- **Health Connect (optional):** Before Android shows the system permission sheet, the app shows an
  in-app list of exactly what we request and why. Health Connect data is processed **on device**
  for Recovery, Dashboard, and AI prompts — it is not uploaded to FlexInsight servers.

  | Access | What we read or write | Why |
  |--------|----------------------|-----|
  | **Read sleep** | Last night's sleep duration | Recovery score, training load, deload hints, AI coaching context. |
  | **Read heart rate** | Resting heart rate samples | Recovery and coaching context (not for medical diagnosis). |
  | **Read steps** | Step count for today | Daily activity context for training load. |
  | **Read active calories** | Active calories burned today | Non-gym activity alongside Hevy volume. |
  | **Read exercise** | Exercise sessions from other apps | Count cardio / non-Hevy workouts in weekly load. |
  | **Write exercise** | Strength workout sessions | **Optional**, only if you turn on "Write workouts to Health Connect" and view-only mode is off — copies completed Hevy sessions into your Health Connect timeline (e.g. Google Fit / Samsung Health). |

- **On-device AI:** Gemini Nano runs locally on supported phones; chat prompts are not sent to a
  FlexInsight cloud service. Coaching uses synced Hevy data (and Health Connect summaries if
  enabled) injected into the prompt on your phone. Debug builds offer **Debug AI UI stubs** in
  Settings to click through the UI with placeholder text — not a substitute for device testing.

## Building from source

Requirements: JDK 17+ and the Android SDK.

```bash
./gradlew assembleDebug        # build debug APK
./gradlew testDebugUnitTest    # run unit tests
./gradlew lintDebug            # run lint checks
```

See [docs/HevyAPI.md](docs/HevyAPI.md) for endpoint documentation used by this project.

## License

[MIT](LICENSE)
