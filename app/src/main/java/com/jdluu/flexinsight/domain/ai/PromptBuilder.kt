package com.jdluu.flexinsight.domain.ai

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Assembles the AI Trainer system prompt from pre-fetched training data.
 *
 * Pure domain class: no Android dependencies. All data is supplied via
 * [TrainingContextInput]; fetching, caching, and failure isolation live in
 * the [com.jdluu.flexinsight.data.ai.HevyAiDataAccessor], which delegates
 * formatting here so prompt structure is unit-testable in isolation.
 */
class PromptBuilder @Inject constructor() {

    // region Input models

    data class ExerciseCandidate(val name: String, val templateId: String)

    data class ProfileInput(
        val displayName: String,
        val weeklyGoalSessions: Int,
        /** "Metric" or anything else for imperial; mirrors UserPreferencesManager values. */
        val units: String,
        val memberSinceMillis: Long?
    )

    data class BaseSummary(
        val totalVolumeKg: Double,
        val averageSessionVolumeKg: Double,
        val currentStreakDays: Int,
        val longestStreakDays: Int
    )

    data class WeeklyGoalLine(
        val completed: Int,
        val target: Int,
        val status: String
    )

    /**
     * Mirrors the original single try-block around stats, trend, and weekly goal:
     * pieces captured before a failure stay, later ones are absent.
     */
    data class TrainingSummary(
        val base: BaseSummary?,
        val fourWeekTrendPctChange: Double?,
        val weeklyGoal: WeeklyGoalLine?
    )

    data class SetData(val weightKg: Double?, val reps: Int?, val rpe: Double?)

    data class ExerciseSets(val name: String, val sets: List<SetData>)

    data class RecentWorkout(
        val startTimeMillis: Long,
        val name: String?,
        val exercises: List<ExerciseSets>
    )

    data class PersonalRecord(
        val exerciseName: String,
        val muscleGroup: String,
        val weightKg: Double,
        val dateMillis: Long
    )

    data class RecoveringMuscle(val groupName: String, val recoveryPct: Float)

    data class VolumeBalanceSplit(val push: Float, val pull: Float, val legs: Float)

    data class RoutineSection(val name: String, /** Null when routine has no exercise list; empty when all names missing. */ val exerciseNames: List<String>?)

    data class PlannedWorkoutSection(val name: String, val intensity: String?)

    data class LatestSession(val startTimeMillis: Long, val name: String?)

    data class HistorySetData(val weightKg: Double?, val reps: Int?)

    data class HistoryEntryData(
        val dateIso: String,
        val sets: List<HistorySetData>?,
        val oneRepMaxKg: Double?
    )

    sealed interface ExerciseHistoryOutcome {
        data class Success(val entries: List<HistoryEntryData>) : ExerciseHistoryOutcome
        data class Failure(val reason: String?) : ExerciseHistoryOutcome
    }

    data class ExerciseHistoryLookup(
        val exerciseName: String,
        val outcome: ExerciseHistoryOutcome
    )

    sealed interface HealthConnectInput {
        data object Disabled : HealthConnectInput
        data object Unavailable : HealthConnectInput
        data object PermissionMissing : HealthConnectInput
        data class Snapshot(
            val sleepHoursLastNight: Double?,
            val restingHeartRateBpm: Long?,
            val stepsToday: Long?,
            val activeCaloriesToday: Double?,
            val cardioSessionsThisWeek: Int
        ) : HealthConnectInput
    }

    data class TrainingContextInput(
        val workoutCount: Int,
        val profile: ProfileInput,
        val trainingSummary: TrainingSummary? = null,
        val recentWorkouts: List<RecentWorkout> = emptyList(),
        /** Sessions with a workout in the last 14 days; null skips the line. */
        val consistencySessionsLast14Days: Int? = null,
        val personalRecords: List<PersonalRecord> = emptyList(),
        /** Muscle group names with high recent volume. */
        val highVolumeMuscles: List<String> = emptyList(),
        val recoveringMuscles: List<RecoveringMuscle> = emptyList(),
        val volumeBalance: VolumeBalanceSplit? = null,
        val routines: List<RoutineSection> = emptyList(),
        val plannedToday: List<PlannedWorkoutSection> = emptyList(),
        val latestSession: LatestSession? = null,
        /** Present only when the user query matched exercises; drives live-history flag. */
        val exerciseHistoryLookups: List<ExerciseHistoryLookup> = emptyList(),
        val healthConnect: HealthConnectInput = HealthConnectInput.Disabled
    )

    data class TrainingContextOutput(
        val text: String,
        /** True when the user query triggered live Hevy exercise-history injection. */
        val usesLiveExerciseHistory: Boolean
    )

    // endregion

    fun noApiKeyContext(): String = """
        System Context - Hevy Data:
        - API key: Not configured. The user must add a Hevy API key in Settings to sync workout history.
        - Instruction: Explain that personalized coaching requires connecting Hevy, then offer general fitness guidance only.
    """.trimIndent()

    fun zeroWorkoutContext(): String = """
        System Context - Hevy Data:
        - API key: Connected.
        - Workouts synced: 0. Data may still be downloading — suggest the user pull to refresh or open Settings to sync.
        - Instruction: Offer general coaching; mention that insights will improve once Hevy workouts sync.
    """.trimIndent()

    /**
     * Matches user query tokens against known exercise templates.
     * Tokens shorter than 4 characters are ignored; results are deduplicated by
     * template id and capped at two exercises.
     */
    fun matchExerciseTemplates(
        query: String,
        candidates: List<ExerciseCandidate>
    ): List<ExerciseCandidate> {
        val queryLower = query.lowercase(Locale.getDefault())
        val tokens = queryLower.split(Regex("\\W+")).filter { it.length >= 4 }.toSet()

        return candidates
            .filter { candidate ->
                tokens.any { token -> candidate.name.lowercase(Locale.getDefault()).contains(token) }
            }
            .distinctBy { it.templateId }
            .take(2)
    }

    fun buildTrainingContext(input: TrainingContextInput): TrainingContextOutput {
        val weightUnit = if (input.profile.units == "Metric") "kg" else "lbs"
        val sb = StringBuilder()
        sb.appendLine("System Context - Hevy Training Data (synced from Hevy API):")
        sb.appendLine("- Data source: Local cache of Hevy workouts; refreshed on app sync.")
        sb.appendLine("- Total logged workouts: ${input.workoutCount}")

        appendProfile(sb, input.profile)
        appendAggregateStats(sb, input.trainingSummary, weightUnit)
        appendRecentWorkouts(sb, input.recentWorkouts, weightUnit)
        appendConsistency(sb, input.consistencySessionsLast14Days)
        appendPersonalRecords(sb, input.personalRecords, weightUnit)
        appendMuscleFatigue(sb, input.highVolumeMuscles)
        appendMuscleRecovery(sb, input.recoveringMuscles)
        appendVolumeBalance(sb, input.volumeBalance)
        appendRoutinesAndPlanned(sb, input.routines, input.plannedToday)
        appendProgressiveOverloadNote(sb, input.latestSession)

        val usesLiveApi = appendQuerySpecificHistory(sb, input.exerciseHistoryLookups, weightUnit)

        appendHealthConnect(sb, input.healthConnect)

        sb.appendLine()
        sb.appendLine(
            "Instruction: You are an expert strength coach. Reference the user's actual Hevy numbers, " +
                "routines, PRs, and recovery state. Be specific — use exercise names, weights, and dates from above. " +
                "If asked about data not listed, say what you do have and suggest they log it in Hevy."
        )

        val text = trimToTokenBudget(sb.toString(), maxChars = 12_000)

        return TrainingContextOutput(
            text = text,
            usesLiveExerciseHistory = usesLiveApi
        )
    }

    private fun appendProfile(sb: StringBuilder, profile: ProfileInput) {
        sb.appendLine("- Name: ${profile.displayName}")
        sb.appendLine("- Preferred units: ${profile.units}")
        sb.appendLine("- Weekly frequency goal: ${profile.weeklyGoalSessions} sessions")
        profile.memberSinceMillis?.let {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sb.appendLine("- Member since: ${fmt.format(Date(it))}")
        }
    }

    private fun appendAggregateStats(sb: StringBuilder, summary: TrainingSummary?, weightUnit: String) {
        val base = summary?.base ?: return
        sb.appendLine()
        sb.appendLine("Training summary:")
        sb.appendLine("- Total volume (all time): ${formatWeight(base.totalVolumeKg, weightUnit)}")
        sb.appendLine("- Average session volume: ${formatWeight(base.averageSessionVolumeKg, weightUnit)}")
        sb.appendLine("- Current streak: ${base.currentStreakDays} days")
        sb.appendLine("- Longest streak: ${base.longestStreakDays} days")

        summary.fourWeekTrendPctChange?.let { trend ->
            val change = "%.1f".format(trend)
            sb.appendLine("- 4-week volume trend: $change% vs prior period")
        }

        summary.weeklyGoal?.let { goal ->
            sb.appendLine("- This week: ${goal.completed}/${goal.target} workouts (${goal.status})")
        }
    }

    private fun appendRecentWorkouts(sb: StringBuilder, recent: List<RecentWorkout>, weightUnit: String) {
        if (recent.isEmpty()) return

        sb.appendLine()
        sb.appendLine("Recently completed workouts (last 7 sessions):")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        recent.forEachIndexed { index, workout ->
            sb.appendLine("${index + 1}. ${dateFormat.format(Date(workout.startTimeMillis))}: ${workout.name}")
            workout.exercises.forEach { exercise ->
                sb.append("   * ${exercise.name}: ")
                sb.appendLine(formatSets(exercise.sets, weightUnit))
            }
        }
    }

    private fun appendConsistency(sb: StringBuilder, sessionsLast14Days: Int?) {
        sessionsLast14Days?.let {
            sb.appendLine("- Consistency: $it workouts in the last 14 days")
        }
    }

    private fun appendPersonalRecords(sb: StringBuilder, prs: List<PersonalRecord>, weightUnit: String) {
        if (prs.isEmpty()) return

        sb.appendLine()
        sb.appendLine("Personal records:")
        prs.forEach { pr ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(pr.dateMillis))
            sb.appendLine("- ${pr.exerciseName} (${pr.muscleGroup}): ${formatWeight(pr.weightKg, weightUnit)} on $date")
        }
    }

    private fun appendMuscleFatigue(sb: StringBuilder, highVolumeMuscles: List<String>) {
        if (highVolumeMuscles.isNotEmpty()) {
            sb.appendLine("- High recent volume muscles: ${highVolumeMuscles.joinToString(", ")}")
        }
    }

    private fun appendMuscleRecovery(sb: StringBuilder, recovering: List<RecoveringMuscle>) {
        val notRecovered = recovering
            .filter { it.recoveryPct < 0.5f }
            .map { "${it.groupName} (${(it.recoveryPct * 100).toInt()}% recovered)" }
        if (notRecovered.isNotEmpty()) {
            sb.appendLine("- Muscles still recovering (<50%): ${notRecovered.joinToString(", ")}")
        }
    }

    private fun appendVolumeBalance(sb: StringBuilder, balance: VolumeBalanceSplit?) {
        balance?.let {
            sb.appendLine(
                "- Push/Pull/Legs volume split (4 wks): " +
                    "Push ${pct(it.push)}, Pull ${pct(it.pull)}, Legs ${pct(it.legs)}"
            )
        }
    }

    private fun appendRoutinesAndPlanned(
        sb: StringBuilder,
        routines: List<RoutineSection>,
        planned: List<PlannedWorkoutSection>
    ) {
        if (routines.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Saved Hevy routines:")
            routines.take(8).forEach { routine ->
                val names = routine.exerciseNames?.joinToString { it } ?: "—"
                sb.appendLine("- ${routine.name}: $names")
            }
        }

        if (planned.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Planned for today:")
            planned.forEach { p ->
                sb.appendLine("- ${p.name} (${p.intensity ?: "planned"})")
            }
        }
    }

    private fun appendProgressiveOverloadNote(sb: StringBuilder, latest: LatestSession?) {
        latest ?: return

        val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
        sb.appendLine()
        sb.appendLine("Latest session (${fmt.format(Date(latest.startTimeMillis))} — ${latest.name}):")
        sb.appendLine("- Coach note: suggest +2.5% weight or +1 rep on main lifts vs that session when programming progress.")
    }

    /** Returns true when any lookup was rendered (mirrors the live-API flag). */
    private fun appendQuerySpecificHistory(
        sb: StringBuilder,
        lookups: List<ExerciseHistoryLookup>,
        weightUnit: String
    ): Boolean {
        if (lookups.isEmpty()) return false

        sb.appendLine()
        sb.appendLine("Exercise history for this question (from Hevy API):")

        for (lookup in lookups) {
            when (val outcome = lookup.outcome) {
                is ExerciseHistoryOutcome.Success -> {
                    val entries = outcome.entries.take(5)
                    if (entries.isEmpty()) {
                        sb.appendLine("- ${lookup.exerciseName}: no history entries")
                        continue
                    }
                    sb.appendLine("- ${lookup.exerciseName}:")
                    entries.forEach { entry ->
                        sb.appendLine("  ${formatHistoryEntry(entry, weightUnit)}")
                    }
                }
                is ExerciseHistoryOutcome.Failure -> {
                    sb.appendLine("- ${lookup.exerciseName}: history unavailable (${outcome.reason})")
                }
            }
        }
        return true
    }

    private fun formatHistoryEntry(entry: HistoryEntryData, weightUnit: String): String {
        val date = entry.dateIso.take(10)
        val best = entry.sets?.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
        val bestStr = best?.let {
            "${formatWeight(it.weightKg ?: 0.0, weightUnit)} x ${it.reps ?: 0} reps"
        } ?: "—"
        val e1rm = entry.oneRepMaxKg?.let { "e1RM ${formatWeight(it, weightUnit)}" } ?: ""
        return "$date: best set $bestStr $e1rm".trim()
    }

    private fun formatSets(sets: List<SetData>, weightUnit: String): String =
        sets.mapNotNull { set ->
            val w = set.weightKg?.let { formatWeight(it, weightUnit) }
            val r = set.reps?.let { "${it}r" }
            val rpe = set.rpe?.let { "@RPE$it" }
            listOfNotNull(w, r, rpe).joinToString(" ").takeIf { it.isNotEmpty() }
        }.joinToString(" | ")

    private fun formatWeight(kg: Double, unit: String): String {
        val value = if (unit == "lbs") kg * 2.20462 else kg
        return "${value.toInt()} $unit"
    }

    private fun pct(value: Float) = "${(value * 100).toInt()}%"

    private fun appendHealthConnect(sb: StringBuilder, health: HealthConnectInput) {
        when (health) {
            HealthConnectInput.Disabled -> return
            HealthConnectInput.Unavailable -> {
                sb.appendLine("- Health Connect: not available on this device")
                return
            }
            HealthConnectInput.PermissionMissing -> {
                sb.appendLine("- Health Connect: enabled but permissions not granted")
                return
            }
            is HealthConnectInput.Snapshot -> {
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
        }
    }

    private fun trimToTokenBudget(text: String, maxChars: Int): String {
        if (text.length <= maxChars) return text
        return text.take(maxChars) + "\n…[context truncated for on-device token limit]"
    }
}
