# FlexInsight Comprehensive Refactor Plan

> **For Hermes:** Orchestrate via OpenCode (`opencode run`) as the implementing junior engineer.
> One PR per phase. Review every diff as senior engineer before merge.

**Goal:** Refactor FlexInsight to SOLID/DRY standards with comprehensive test coverage, hooked into CI, without breaking existing features.

**Architecture:** Incremental refactor on `main`-adjacent feature branches. Each phase lands a PR that keeps the build green (`assembleDebug` + `testDebugUnitTest` + `lintDebug`). No behavior changes except where explicitly fixing defects found during testing.

**Tech Stack:** Kotlin 2.1.20, Compose, Room 2.7, Hilt 2.56, Retrofit/OkHttp, WorkManager, ML Kit GenAI (Gemini Nano), JUnit4 + Robolectric + Turbine + MockK/Mockito, GitHub Actions CI.

---

## Baseline (verified)

- ~18.5k LOC Kotlin across data/domain/ui layers; clean layered structure already present
- Existing tests: 10 unit test files + 1 migration instrumented test — far from comprehensive
- **No CI at all** (no `.github/`)
- Largest hotspots: StatsRepositoryImpl (555), WorkoutRepositoryImpl (525), SettingsScreen (440), HevyAiDataAccessor (402), SettingsViewModel (383)
- Known duplication: stats calculations spread across StatsRepositoryImpl, StatsCalculator, DashboardStats.kt, HistoryStats.kt, WorkoutStats.kt (UI copies)
- Build env: JDK 21 works for AGP 8.13; Android SDK at ~/.local/android-sdk; adb device OFF-LIMITS (occupied by another session) — all tests must be JVM-side (Robolectric) or non-emulator instrumented only in CI via emulator later if user approves

## Guiding principles

1. Every PR: build green + all tests pass + lint passes before merge.
2. TDD: new abstractions get tests written first against current behavior (characterization tests where behavior is subtle, e.g., training load, recovery score, deload detection).
3. No device/emulator use. Robolectric for anything Android-framework-dependent.
4. Small PRs (~300-600 diff lines). Conventional Commits. Squash merges.
5. Extensibility targets: AI provider abstraction (Gemini Nano today, others tomorrow), repository interfaces already exist but leak implementation details; sync pipeline hardwired to Hevy.

## Phase 0 — CI bootstrap (PR #1, branch ci/github-actions)

Files:
- `.github/workflows/ci.yml`

Steps:
1. Write workflow: JDK 17 (temurin), gradle cache, jobs:
   - `lint`: ./gradlew lintDebug
   - `unit`: ./gradlew testDebugUnitTest (+ publish JUnit XML report summary)
   - `build`: ./gradlew assembleDebug, upload APK artifact on main
2. Verify locally first: run all three commands on this machine, confirm exit 0.
3. Push, open PR, watch checks pass, merge.
4. Add branch protection is out of scope (user decision).

Acceptance: PR green on GitHub Actions; subsequent PRs gated by it.

## Phase 1 — Test foundation & characterization tests (PR #2, branch test/foundation)

Files:
- `gradle/libs.versions.toml`: add turbine, mockk, truth (or assertk)
- Create `app/src/test/.../domain/usecase/*Test.kt` for ALL existing use cases:
  - CalculateTrainingLoadUseCase, DetectDeloadUseCase, GetMuscleRecoveryUseCase,
    GetWeeklyProgressUseCase, GetMuscleGroupProgressUseCase, GetPRDetailsUseCase,
    CompareRoutineSessionsUseCase, ExplainWorkoutUseCase, ExportCoachReportUseCase,
    BuildAiContextUseCase
- Create `app/src/test/.../data/repository/*Test.kt` for repository impls using fake DAOs (no mock-heavy tests for logic-bearing code; fakes over mocks per Google guidance)
- Create `app/src/test/.../core/errors/ErrorHandlerTest.kt`, ResultTest, NetworkMonitorTest (Robolectric)

Steps (per module): write failing/skeleton test -> confirm it compiles and characterizes CURRENT behavior -> document any surprising behavior in test comments (do not fix yet).
Run: `./gradlew testDebugUnitTest` — expect all pass (characterization = encode reality).

Acceptance: every domain use case and repository has a test file; suite green; coverage of domain/data logic substantially up (report jacoco numbers).

Add `.github/workflows` step or separate job: JaCoCo coverage report artifact (add jacoco to app/build.gradle.kts).

## Phase 2 — Domain purity & DRY stats consolidation (PR #3, branch refactor/stats-core)

Problem: stat math duplicated between StatsRepositoryImpl / StatsCalculator / UI composables.

Steps:
1. Extract pure calculation functions into `domain/calc/` (e.g., VolumeCalculator, TrainingLoadCalculator, RecoveryScoreCalculator) — no Android deps, no coroutines, plain input->output.
2. Move logic from StatsRepositoryImpl + StatsCalculator into these; repositories become orchestration-only (SRP).
3. Update UI parts files to call shared calculators instead of local copies (delete duplicated math).
4. Characterization tests from Phase 1 must still pass unchanged (this proves no behavior change). Add unit tests for each extracted calculator directly.

Acceptance: grep shows zero volume/EMA/load math outside `domain/calc/`; full suite green; net LOC reduction in data layer.

## Phase 3 — Repository SRP split (PR #4, branch refactor/repo-split)

Steps:
1. Split WorkoutRepositoryImpl (525L): separate query concerns (read paths) from mutation/sync-write concerns into WorkoutQueryRepository / WorkoutMutationRepository behind existing interfaces; keep old interface delegating so ViewModels unchanged initially.
2. Same treatment for StatsRepositoryImpl if still oversized after Phase 2.
3. Introduce mappers module `data/mapper/` — move API-entity <-> domain mapping out of repositories (single responsibility, reusable).
4. Tests: fakes updated; new mapper tests.

Acceptance: repos under ~250 lines; ViewModels untouched; suite green.

## Phase 4 — AI layer extensibility (PR #5, branch refactor/ai-provider)

Steps:
1. Define `interface AiClient { suspend fun generate(prompt: AiPrompt): Result<AiResponse>; fun isAvailable(): Flow<AiFeatureStatus> }`.
2. GeminiNanoClient implements AiClient; FlexAIClient becomes a facade selecting provider (Strategy pattern); DI binds via @Binds in AiModule.
3. Extract prompt assembly from HevyAiDataAccessor into `domain/ai/PromptBuilder` (pure, testable).
4. Tests: PromptBuilder exhaustive tests (context truncation, exercise-history injection, empty states); FakeAiClient for AITrainerViewModel tests.

Acceptance: adding a second AI provider = one new class + one DI line. AITrainerViewModel tested against fake.

## Phase 5 — Sync pipeline abstraction (PR #6, branch refactor/sync-pipeline)

Steps:
1. Extract `HevySyncSource` interface (fetch workouts/routines/templates pages) from SyncCoordinator/SyncManager; inject instead of direct Retrofit service calls.
2. SyncManager orchestrates generic sources; Hevy becomes one implementation.
3. Tests: SyncCoordinator with fake source — incremental cursor handling, error/retry, offline skip paths.

Acceptance: sync tests cover incremental/error/offline paths without network.

## Phase 6 — UI layer hygiene (PR #7, branch refactor/ui-hygiene)

Steps:
1. Break up 400+ line screens/parts files into smaller composables (mechanical, no visual change).
2. Standardize ViewModel state pattern: single immutable UiState data class per screen (where not already done), collected via `collectAsStateWithLifecycle`.
3. ViewModel tests with fakes + Turbine for state emissions (DashboardViewModel, PlannerViewModel, SettingsViewModel, AITrainerViewModel, HistoryViewModel).

Acceptance: lint passes, no screen file >300 lines, viewmodel state tests green.

## Phase 7 — Final hardening (PR #8, branch chore/hardening)

1. Full JaCoCo coverage gate in CI (fail under threshold — set realistically based on Phase 1 baseline, e.g., 60% domain/data, exclude ui/theme/generated).
2. ktlint/detekt configured with lenient baseline; fix violations incrementally.
3. README architecture section update.
4. Tag v1.1.0 if user approves release.

---

## Risks / tradeoffs

- stealth/ox-alpha rate limits (popular free model): retry with backoff; fall back to poolside/laguna-s-2.1 if blocked for >10 min.
- Characterization tests may reveal real bugs — log them, do NOT fix inside refactor PRs; separate fix PRs.
- Robolectric can't cover WorkManager+Hilt worker init fully — BackgroundSyncWorker gets constructor-level unit tests only.
- No emulator available: instrumented migration test stays manual/local-device; CI runs JVM tests only (documented limitation).
- Compose refactors risk visual regressions — mechanical moves only, no restyling.

## Verification protocol (every PR)

```
./gradlew assembleDebug testDebugUnitTest lintDebug   # exit 0 required
git push -u origin <branch>                            # open PR via gh
gh pr checks --watch                                   # all green
gh pr merge --squash --delete-branch
```
