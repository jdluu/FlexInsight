package com.jdluu.flexinsight.data.ai

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.model.ExerciseHistoryEntry
import com.jdluu.flexinsight.data.model.Set as WorkoutSet
import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.data.repository.ExerciseRepository
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.data.repository.RoutineRepository
import com.jdluu.flexinsight.data.repository.StatsRepository
import com.jdluu.flexinsight.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides Hevy workout data to the AI Trainer.
 *
 * Data path: Hevy API → Room (background sync) → this accessor → prompt context.
 * On-device Gemini Nano does not support MCP/function calling, so we pre-fetch and
 * inject structured context, with optional live Hevy API calls for exercise history.
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
    private val healthConnectRepository: HealthConnectRepository
) {

    data class ContextSnapshot(
        val text: String,
        val hasWorkoutData: Boolean,
        val hasApiKey: Boolean,
        val workoutCount: Int,
        /** True when live Hevy exercise_history API was queried for this turn. */
        val usesLiveExerciseHistory: Boolean = false
    )

    suspend fun buildContext(userQuery: String? = null): ContextSnapshot {
        val hasApiKey = apiKeyManager.hasApiKey()
        val workoutCount = try {
            flexRepository.getWorkoutCount().first()
        } catch (_: Exception) {
            0
        }

        if (!hasApiKey) {
            return ContextSnapshot(
                text = """
                    System Context - Hevy Data:
                    - API key: Not configured. The user must add a Hevy API key in Settings to sync workout history.
                    - Instruction: Explain that personalized coaching requires connecting Hevy, then offer general fitness guidance only.
                """.trimIndent(),
                hasWorkoutData = false,
                hasApiKey = false,
                workoutCount = 0
            )
        }

        if (workoutCount == 0) {
            return ContextSnapshot(
                text = """
                    System Context - Hevy Data:
                    - API key: Connected.
                    - Workouts synced: 0. Data may still be downloading — suggest the user pull to refresh or open Settings to sync.
                    - Instruction: Offer general coaching; mention that insights will improve once Hevy workouts sync.
                """.trimIndent(),
                hasWorkoutData = false,
                hasApiKey = true,
                workoutCount = 0
            )
        }

        val units = userPreferencesManager.getUnits()
        val weightUnit = if (units == "Metric") "kg" else "lbs"
        val sb = StringBuilder()
        sb.appendLine("System Context - Hevy Training Data (synced from Hevy API):")
        sb.appendLine("- Data source: Local cache of Hevy workouts; refreshed on app sync.")
        sb.appendLine("- Total logged workouts: $workoutCount")

        appendProfile(sb)
        appendAggregateStats(sb, weightUnit)
        appendRecentWorkouts(sb, weightUnit, limit = 7)
        appendConsistency(sb)
        appendPersonalRecords(sb, weightUnit)
        appendMuscleFatigue(sb)
        appendMuscleRecovery(sb)
        appendVolumeBalance(sb)
        appendRoutinesAndPlanned(sb)
        appendProgressiveOverloadNote(sb)

        val usesLiveApi = userQuery?.let { query ->
            appendQuerySpecificHistory(sb, query, weightUnit)
        } ?: false

        appendHealthConnect(sb)

        sb.appendLine()
        sb.appendLine(
            "Instruction: You are an expert strength coach. Reference the user's actual Hevy numbers, " +
                "routines, PRs, and recovery state. Be specific — use exercise names, weights, and dates from above. " +
                "If asked about data not listed, say what you do have and suggest they log it in Hevy."
        )

        val text = trimToTokenBudget(sb.toString(), maxChars = 12_000)

        return ContextSnapshot(
            text = text,
            hasWorkoutData = true,
            usesLiveExerciseHistory = usesLiveApi,
            hasApiKey = true,
            workoutCount = workoutCount
        )
    }

    private suspend fun appendProfile(sb: StringBuilder) {
        val displayName = userPreferencesManager.getDisplayName() ?: "Athlete"
        val goal = userPreferencesManager.getWeeklyGoal()
        val units = userPreferencesManager.getUnits()
        sb.appendLine("- Name: $displayName")
        sb.appendLine("- Preferred units: $units")
        sb.appendLine("- Weekly frequency goal: $goal sessions")
        try {
            val profile = flexRepository.getProfileInfo()
            profile.memberSince?.let {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sb.appendLine("- Member since: ${fmt.format(Date(it))}")
            }
        } catch (_: Exception) { }
    }

    private suspend fun appendAggregateStats(sb: StringBuilder, weightUnit: String) {
        try {
            val stats = flexRepository.calculateStats()
            sb.appendLine()
            sb.appendLine("Training summary:")
            sb.appendLine("- Total volume (all time): ${formatWeight(stats.totalVolume, weightUnit)}")
            sb.appendLine("- Average session volume: ${formatWeight(stats.averageVolume, weightUnit)}")
            sb.appendLine("- Current streak: ${stats.currentStreak} days")
            sb.appendLine("- Longest streak: ${stats.longestStreak} days")

            val trend = flexRepository.calculateVolumeTrend(weeks = 4)
            val change = "%.1f".format(trend.percentageChange)
            sb.appendLine("- 4-week volume trend: $change% vs prior period")

            val goalProgress = flexRepository.getWeeklyGoalProgress()
            sb.appendLine("- This week: ${goalProgress.completed}/${goalProgress.target} workouts (${goalProgress.status})")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed aggregate stats", e)
        }
    }

    private suspend fun appendRecentWorkouts(sb: StringBuilder, weightUnit: String, limit: Int) {
        val recent = try {
            workoutRepository.getRecentWorkouts(limit).first()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed recent workouts", e)
            emptyList()
        }

        if (recent.isEmpty()) return

        sb.appendLine()
        sb.appendLine("Recently completed workouts (last $limit sessions):")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        recent.forEachIndexed { index, workout ->
            sb.appendLine("${index + 1}. ${dateFormat.format(Date(workout.startTime))}: ${workout.name}")
            val exercises = workoutRepository.getExercisesByWorkoutId(workout.id)
            exercises.forEach { exercise ->
                sb.append("   * ${exercise.name}: ")
                val sets = workoutRepository.getSetsByExerciseId(exercise.id)
                sb.appendLine(formatSets(sets, weightUnit))
            }
        }
    }

    private suspend fun appendConsistency(sb: StringBuilder) {
        try {
            val days = statsRepository.getConsistencyData(14)
            val sessions = days.count { it.hasWorkout }
            sb.appendLine("- Consistency: $sessions workouts in the last 14 days")
        } catch (_: Exception) { }
    }

    private suspend fun appendPersonalRecords(sb: StringBuilder, weightUnit: String) {
        val prs = try {
            statsRepository.getPRsWithDetails(limit = 15)
        } catch (_: Exception) {
            emptyList()
        }
        if (prs.isEmpty()) return

        sb.appendLine()
        sb.appendLine("Personal records:")
        prs.forEach { pr ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(pr.date))
            sb.appendLine("- ${pr.exerciseName} (${pr.muscleGroup}): ${formatWeight(pr.weight, weightUnit)} on $date")
        }
    }

    private suspend fun appendMuscleFatigue(sb: StringBuilder) {
        val fatigue = try {
            statsRepository.getMuscleGroupProgress(weeks = 1)
        } catch (_: Exception) {
            emptyList()
        }
        val high = fatigue.filter { it.intensity == "HI" }.map { it.muscleGroup }
        if (high.isNotEmpty()) {
            sb.appendLine("- High recent volume muscles: ${high.joinToString(", ")}")
        }
    }

    private suspend fun appendMuscleRecovery(sb: StringBuilder) {
        try {
            val recovery = flexRepository.getMuscleRecoveryStatus()
            val notRecovered = recovery
                .filter { (_, pct) -> pct < 0.5f }
                .map { (group, pct) -> "${group.name} (${(pct * 100).toInt()}% recovered)" }
            if (notRecovered.isNotEmpty()) {
                sb.appendLine("- Muscles still recovering (<50%): ${notRecovered.joinToString(", ")}")
            }
        } catch (_: Exception) { }
    }

    private suspend fun appendVolumeBalance(sb: StringBuilder) {
        try {
            val balance = flexRepository.getVolumeBalance(weeks = 4)
            sb.appendLine(
                "- Push/Pull/Legs volume split (4 wks): " +
                    "Push ${pct(balance.push)}, Pull ${pct(balance.pull)}, Legs ${pct(balance.legs)}"
            )
        } catch (_: Exception) { }
    }

    private suspend fun appendRoutinesAndPlanned(sb: StringBuilder) {
        val routines = try {
            routineRepository.getRoutines().first()
        } catch (_: Exception) {
            emptyList()
        }
        if (routines.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Saved Hevy routines:")
            routines.take(8).forEach { routine ->
                val names = routine.exercises?.joinToString { it.name ?: "?" } ?: "—"
                sb.appendLine("- ${routine.name}: $names")
            }
        }

        val planned = try {
            statsRepository.getPlannedWorkoutsForDay(System.currentTimeMillis())
        } catch (_: Exception) {
            emptyList()
        }
        if (planned.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Planned for today:")
            planned.forEach { p ->
                sb.appendLine("- ${p.name} (${p.intensity ?: "planned"})")
            }
        }
    }

    private suspend fun appendProgressiveOverloadNote(sb: StringBuilder) {
        val latest = try {
            workoutRepository.getRecentWorkouts(1).first().firstOrNull()
        } catch (_: Exception) {
            null
        } ?: return

        val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
        sb.appendLine()
        sb.appendLine("Latest session (${fmt.format(Date(latest.startTime))} — ${latest.name}):")
        sb.appendLine("- Coach note: suggest +2.5% weight or +1 rep on main lifts vs that session when programming progress.")
    }

    /**
     * Live Hevy API tool: fetches exercise history when the user asks about a specific lift.
     */
    private suspend fun appendQuerySpecificHistory(sb: StringBuilder, query: String, weightUnit: String): Boolean {
        val templateIds = findRelevantTemplateIds(query)
        if (templateIds.isEmpty()) return false

        sb.appendLine()
        sb.appendLine("Exercise history for this question (from Hevy API):")

        for ((name, templateId) in templateIds) {
            when (val result = flexRepository.getExerciseHistory(templateId)) {
                is Result.Success -> {
                    val entries = result.data.history.take(5)
                    if (entries.isEmpty()) {
                        sb.appendLine("- $name: no history entries")
                        continue
                    }
                    sb.appendLine("- $name:")
                    entries.forEach { entry ->
                        sb.appendLine("  ${formatHistoryEntry(entry, weightUnit)}")
                    }
                }
                is Result.Error -> {
                    sb.appendLine("- $name: history unavailable (${result.error.message})")
                }
            }
        }
        return true
    }

    private suspend fun findRelevantTemplateIds(query: String): List<Pair<String, String>> {
        val queryLower = query.lowercase(Locale.getDefault())
        val tokens = queryLower.split(Regex("\\W+")).filter { it.length >= 4 }.toSet()

        val exercises = try {
            exerciseRepository.getAllExercises().first()
        } catch (_: Exception) {
            emptyList()
        }

        return exercises
            .filter { ex ->
                ex.exerciseTemplateId != null &&
                    tokens.any { token -> ex.name.lowercase(Locale.getDefault()).contains(token) }
            }
            .distinctBy { it.exerciseTemplateId }
            .take(2)
            .map { it.name to it.exerciseTemplateId!! }
    }

    private fun formatHistoryEntry(entry: ExerciseHistoryEntry, weightUnit: String): String {
        val date = entry.date.take(10)
        val best = entry.sets?.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
        val bestStr = best?.let {
            "${formatWeight(it.weightKg ?: 0.0, weightUnit)} x ${it.reps ?: 0} reps"
        } ?: "—"
        val e1rm = entry.oneRepMax?.let { "e1RM ${formatWeight(it, weightUnit)}" } ?: ""
        return "$date: best set $bestStr $e1rm".trim()
    }

    private fun formatSets(sets: List<WorkoutSet>, weightUnit: String): String =
        sets.mapNotNull { set ->
            val w = set.weight?.let { formatWeight(it, weightUnit) }
            val r = set.reps?.let { "${it}r" }
            val rpe = set.rpe?.let { "@RPE$it" }
            listOfNotNull(w, r, rpe).joinToString(" ").takeIf { it.isNotEmpty() }
        }.joinToString(" | ")

    private fun formatWeight(kg: Double, unit: String): String {
        val value = if (unit == "lbs") kg * 2.20462 else kg
        return "${value.toInt()} $unit"
    }

    private fun pct(value: Float) = "${(value * 100).toInt()}%"

    private suspend fun appendHealthConnect(sb: StringBuilder) {
        if (!userPreferencesManager.getHealthConnectEnabled()) return
        val health = healthConnectRepository.readSnapshot()
        if (!health.isAvailable) {
            sb.appendLine("- Health Connect: not available on this device")
            return
        }
        if (!health.isPermissionGranted) {
            sb.appendLine("- Health Connect: enabled but permissions not granted")
            return
        }
        sb.appendLine()
        sb.appendLine("Health Connect (last 24h / week):")
        health.sleepHoursLastNight?.let {
            sb.appendLine("- Sleep last night: ${"%.1f".format(it)} hours")
        }
        health.restingHeartRateBpm?.let {
            sb.appendLine("- Resting heart rate: $it bpm")
        }
        health.stepsToday?.let {
            sb.appendLine("- Steps today: $it")
        }
        health.activeCaloriesToday?.let {
            sb.appendLine("- Active calories today: ${it.toInt()} kcal")
        }
        if (health.cardioSessionsThisWeek > 0) {
            sb.appendLine("- Non-strength sessions this week: ${health.cardioSessionsThisWeek}")
        }
    }

    private fun trimToTokenBudget(text: String, maxChars: Int): String {
        if (text.length <= maxChars) return text
        return text.take(maxChars) + "\n…[context truncated for on-device token limit]"
    }

    companion object {
        private const val TAG = "HevyAiDataAccessor"
    }
}
