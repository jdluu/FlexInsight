package com.jdluu.flexinsight.data.ai

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.data.repository.ExerciseRepository
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.data.repository.RoutineRepository
import com.jdluu.flexinsight.data.repository.StatsRepository
import com.jdluu.flexinsight.data.repository.WorkoutRepository
import com.jdluu.flexinsight.domain.ai.AiContextProvider
import com.jdluu.flexinsight.domain.ai.PromptBuilder
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides Hevy workout data to the AI Trainer.
 *
 * Data path: Hevy API → Room (background sync) → this accessor → prompt context.
 * On-device Gemini Nano does not support MCP/function calling, so we pre-fetch and
 * inject structured context, with optional live Hevy API calls for exercise history.
 *
 * Fetching and failure isolation live here; prompt assembly is delegated to the pure
 * [PromptBuilder] so its output can be pinned by characterization tests.
 */
@Singleton
class HevyAiDataAccessor @Inject constructor(
    private val flexRepository: FlexRepository,
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val statsRepository: StatsRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val apiKeyManager: ApiKeyManager,
    private val healthConnectRepository: HealthConnectRepository,
    private val promptBuilder: PromptBuilder
) : AiContextProvider {

    data class ContextSnapshot(
        val text: String,
        val hasWorkoutData: Boolean,
        val hasApiKey: Boolean,
        val workoutCount: Int,
        /** True when live Hevy exercise_history API was queried for this turn. */
        val usesLiveExerciseHistory: Boolean = false
    )

    override suspend fun buildContext(userQuery: String?): ContextSnapshot {
        val hasApiKey = apiKeyManager.hasApiKey()
        val workoutCount = try {
            flexRepository.getWorkoutCount().first()
        } catch (_: Exception) {
            0
        }

        if (!hasApiKey) {
            return ContextSnapshot(
                text = promptBuilder.noApiKeyContext(),
                hasWorkoutData = false,
                hasApiKey = false,
                workoutCount = 0
            )
        }

        if (workoutCount == 0) {
            return ContextSnapshot(
                text = promptBuilder.zeroWorkoutContext(),
                hasWorkoutData = false,
                hasApiKey = true,
                workoutCount = 0
            )
        }

        val profile = PromptBuilder.ProfileInput(
            displayName = userPreferencesManager.getDisplayName() ?: "Athlete",
            weeklyGoalSessions = userPreferencesManager.getWeeklyGoal(),
            units = userPreferencesManager.getUnits(),
            memberSinceMillis = try {
                flexRepository.getProfileInfo().memberSince
            } catch (_: Exception) {
                null
            }
        )

        // Mirrors the original single try-block: pieces captured before a failure stay.
        val trainingSummary = try {
            val stats = flexRepository.calculateStats()
            val trend = flexRepository.calculateVolumeTrend(weeks = 4)
            val goalProgress = flexRepository.getWeeklyGoalProgress()
            PromptBuilder.TrainingSummary(
                base = PromptBuilder.BaseSummary(
                    totalVolumeKg = stats.totalVolume,
                    averageSessionVolumeKg = stats.averageVolume,
                    currentStreakDays = stats.currentStreak,
                    longestStreakDays = stats.longestStreak
                ),
                fourWeekTrendPctChange = trend.percentageChange,
                weeklyGoal = PromptBuilder.WeeklyGoalLine(
                    completed = goalProgress.completed,
                    target = goalProgress.target,
                    status = goalProgress.status
                )
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed aggregate stats", e)
            PromptBuilder.TrainingSummary(base = null, fourWeekTrendPctChange = null, weeklyGoal = null)
        }

        val recentWorkouts = try {
            workoutRepository.getRecentWorkouts(RECENT_WORKOUT_LIMIT).first().map { workout ->
                PromptBuilder.RecentWorkout(
                    startTimeMillis = workout.startTime,
                    name = workout.name,
                    exercises = workoutRepository.getExercisesByWorkoutId(workout.id).map { exercise ->
                        PromptBuilder.ExerciseSets(
                            name = exercise.name,
                            sets = workoutRepository.getSetsByExerciseId(exercise.id).map { set ->
                                PromptBuilder.SetData(weightKg = set.weight, reps = set.reps, rpe = set.rpe)
                            }
                        )
                    }
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed recent workouts", e)
            emptyList()
        }

        val consistencySessions = try {
            statsRepository.getConsistencyData(days = 14).count { it.hasWorkout }
        } catch (_: Exception) {
            null
        }

        val personalRecords = try {
            statsRepository.getPRsWithDetails(limit = 15)
        } catch (_: Exception) {
            emptyList()
        }.map { pr ->
            PromptBuilder.PersonalRecord(
                exerciseName = pr.exerciseName,
                muscleGroup = pr.muscleGroup,
                weightKg = pr.weight,
                dateMillis = pr.date
            )
        }

        val highVolumeMuscles = try {
            statsRepository.getMuscleGroupProgress(weeks = 1)
        } catch (_: Exception) {
            emptyList()
        }.filter { it.intensity == "HI" }.map { it.muscleGroup }

        val recoveringMuscles = try {
            flexRepository.getMuscleRecoveryStatus()
        } catch (_: Exception) {
            emptyMap()
        }.mapNotNull { (group, pct) ->
            if (pct < 0.5f) PromptBuilder.RecoveringMuscle(group.name, pct) else null
        }

        val volumeBalance = try {
            flexRepository.getVolumeBalance(weeks = 4)
        } catch (_: Exception) {
            null
        }?.let { PromptBuilder.VolumeBalanceSplit(it.push, it.pull, it.legs) }

        val routines = try {
            routineRepository.getRoutines().first()
        } catch (_: Exception) {
            emptyList()
        }.map { routine ->
            PromptBuilder.RoutineSection(
                name = routine.name,
                exerciseNames = routine.exercises?.map { it.name ?: "?" }
            )
        }

        val plannedToday = try {
            statsRepository.getPlannedWorkoutsForDay(System.currentTimeMillis())
        } catch (_: Exception) {
            emptyList()
        }.map { p -> PromptBuilder.PlannedWorkoutSection(name = p.name, intensity = p.intensity) }

        val latestSession = try {
            workoutRepository.getRecentWorkouts(1).first().firstOrNull()
        } catch (_: Exception) {
            null
        }?.let { PromptBuilder.LatestSession(startTimeMillis = it.startTime, name = it.name) }

        val historyLookups = userQuery?.let { query ->
            resolveExerciseHistoryLookups(query)
        } ?: emptyList()

        val healthConnect = if (!userPreferencesManager.getHealthConnectEnabled()) {
            PromptBuilder.HealthConnectInput.Disabled
        } else {
            healthConnectRepository.readSnapshot().let { health ->
                when {
                    !health.isAvailable -> PromptBuilder.HealthConnectInput.Unavailable
                    !health.isPermissionGranted -> PromptBuilder.HealthConnectInput.PermissionMissing
                    else -> PromptBuilder.HealthConnectInput.Snapshot(
                        sleepHoursLastNight = health.sleepHoursLastNight,
                        restingHeartRateBpm = health.restingHeartRateBpm,
                        stepsToday = health.stepsToday,
                        activeCaloriesToday = health.activeCaloriesToday,
                        cardioSessionsThisWeek = health.cardioSessionsThisWeek
                    )
                }
            }
        }

        val output = promptBuilder.buildTrainingContext(
            PromptBuilder.TrainingContextInput(
                workoutCount = workoutCount,
                profile = profile,
                trainingSummary = trainingSummary,
                recentWorkouts = recentWorkouts,
                consistencySessionsLast14Days = consistencySessions,
                personalRecords = personalRecords,
                highVolumeMuscles = highVolumeMuscles,
                recoveringMuscles = recoveringMuscles,
                volumeBalance = volumeBalance,
                routines = routines,
                plannedToday = plannedToday,
                latestSession = latestSession,
                exerciseHistoryLookups = historyLookups,
                healthConnect = healthConnect
            )
        )

        return ContextSnapshot(
            text = output.text,
            hasWorkoutData = true,
            usesLiveExerciseHistory = output.usesLiveExerciseHistory,
            hasApiKey = true,
            workoutCount = workoutCount
        )
    }

    /**
     * Live Hevy API tool: fetches exercise history when the user asks about a specific lift.
     */
    private suspend fun resolveExerciseHistoryLookups(query: String): List<PromptBuilder.ExerciseHistoryLookup> {
        val candidates = try {
            exerciseRepository.getAllExercises().first()
        } catch (_: Exception) {
            emptyList()
        }.filter { it.exerciseTemplateId != null }
            .map { PromptBuilder.ExerciseCandidate(name = it.name, templateId = it.exerciseTemplateId!!) }

        val matches = promptBuilder.matchExerciseTemplates(query, candidates)

        return matches.map { match ->
            val outcome = when (val result = flexRepository.getExerciseHistory(match.templateId)) {
                is Result.Success -> PromptBuilder.ExerciseHistoryOutcome.Success(
                    result.data.history.map { entry ->
                        PromptBuilder.HistoryEntryData(
                            dateIso = entry.date,
                            sets = entry.sets?.map { set ->
                                PromptBuilder.HistorySetData(weightKg = set.weightKg, reps = set.reps)
                            },
                            oneRepMaxKg = entry.oneRepMax
                        )
                    }
                )
                is Result.Error -> PromptBuilder.ExerciseHistoryOutcome.Failure(reason = result.error.message)
            }
            PromptBuilder.ExerciseHistoryLookup(exerciseName = match.name, outcome = outcome)
        }
    }

    companion object {
        private const val TAG = "HevyAiDataAccessor"
        private const val RECENT_WORKOUT_LIMIT = 7
    }
}
