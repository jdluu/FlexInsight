package com.example.flexinsight.domain.usecase

import com.example.flexinsight.data.preferences.UserPreferencesManager
import com.example.flexinsight.data.repository.StatsRepository
import com.example.flexinsight.data.repository.WorkoutRepository
import com.example.flexinsight.data.repository.RoutineRepository
import com.example.flexinsight.data.model.Workout
import com.example.flexinsight.data.model.Exercise
import com.example.flexinsight.data.model.Set as WorkoutSet
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Use case to build the comprehensive AI context string for coaching.
 */
class BuildAiContextUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val userPreferencesManager: UserPreferencesManager
) {

    suspend operator fun invoke(): String {
        val sb = StringBuilder()
        sb.append("System Context - User Data:\n")

        // 1. User Profile
        val units = userPreferencesManager.getUnits()
        val goal = userPreferencesManager.getWeeklyGoal()
        val displayName = userPreferencesManager.getDisplayName() ?: "Athlete"

        sb.append("- Name: $displayName\n")
        sb.append("- Preferred Units: $units\n")
        sb.append("- Weekly Frequency Goal: $goal sessions\n")

        // 2. Workout History (Deep Context)
        val recentWorkouts: List<Workout> = try {
            workoutRepository.getRecentWorkouts(limit = 7).first()
        } catch (e: Exception) {
            android.util.Log.e("BuildAiContextUseCase", "Failed to fetch recent workouts", e)
            emptyList()
        }
        
        if (recentWorkouts.isNotEmpty()) {
            sb.append("\nRecently Completed Workouts (Last 7 Sessions):\n")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
                recentWorkouts.forEachIndexed { index, workout ->
                val dateStr = dateFormat.format(Date(workout.startTime))
                sb.append("${index + 1}. $dateStr: ${workout.name ?: "Untitled Workout"}\n")
                
                // Fetch details for this workout
                val exercises = workoutRepository.getExercisesByWorkoutId(workout.id)
                exercises.forEach { exercise ->
                    sb.append("   * ${exercise.name}: ")
                    val sets = workoutRepository.getSetsByExerciseId(exercise.id)
                    val setSummaries = sets.map { set: WorkoutSet ->
                        val weightStr = if (set.weight != null) "${set.weight}${units[0].lowercase()}" else ""
                        val repsStr = if (set.reps != null) "${set.reps}r" else ""
                        val rpeStr = if (set.rpe != null) " @RPE${set.rpe}" else ""
                        "$weightStr $repsStr$rpeStr".trim()
                    }.filter { it.isNotEmpty() }
                    
                    sb.append(setSummaries.joinToString(" | "))
                    sb.append("\n")
                }
                sb.append("\n")
            }
        } else {
            sb.append("- Activity History: No workouts recorded yet.\n")
        }

        // 3. Overall Progress Hint
        val consistencyData: List<com.example.flexinsight.data.model.DayInfo> = try {
             statsRepository.getConsistencyData(14)
        } catch (e: Exception) {
             android.util.Log.e("BuildAiContextUseCase", "Failed to get consistency data", e)
             emptyList()
        }
        val sessionsLast14Days = consistencyData.count { it.hasWorkout }
        sb.append("Training Consistency: $sessionsLast14Days workouts in the last 14 days.\n")

        // 4. Personal Records (Bench Press, etc.)
        val prs = try {
            statsRepository.getPRsWithDetails(limit = 20)
        } catch (e: Exception) {
            android.util.Log.e("BuildAiContextUseCase", "Failed to fetch PRs", e)
            emptyList()
        }

        if (prs.isNotEmpty()) {
            sb.append("\nPersonal Best Lifts (PRs):\n")
            prs.forEach { pr ->
                val weightStr = "${pr.weight}${units[0].lowercase()}"
                sb.append("- ${pr.exerciseName}: $weightStr\n")
            }
        }

        // 5. Advanced Context: Muscle Fatigue (Last 72 Hours)
        val fatigueData = try {
            statsRepository.getMuscleGroupProgress(weeks = 1)
        } catch (e: Exception) {
            android.util.Log.e("BuildAiContextUseCase", "Failed to fetch fatigue data", e)
            emptyList()
        }
        val highFatigueMuscles = fatigueData.filter { it.intensity == "HI" }.map { it.muscleGroup }
        if (highFatigueMuscles.isNotEmpty()) {
            sb.append("\nMuscle Fatigue (High): ${highFatigueMuscles.joinToString(", ")}\n")
            sb.append("Note: These muscles were trained recently with high volume. Suggest recovery or secondary focus if training them again.\n")
        }

        // 6. Advanced Context: Routines & Planned
        val routines = try { routineRepository.getRoutines().first() } catch (e: Exception) { 
            android.util.Log.e("BuildAiContextUseCase", "Failed to fetch routines", e)
            emptyList() 
        }
        val plannedToday = try { statsRepository.getPlannedWorkoutsForDay(System.currentTimeMillis()) } catch (e: Exception) { 
            android.util.Log.e("BuildAiContextUseCase", "Failed to fetch planned workouts", e)
            emptyList() 
        }
        
        if (routines.isNotEmpty()) {
            sb.append("\nYour Saved Routines:\n")
            routines.take(5).forEach { routine ->
                val exerciseList = routine.exercises?.joinToString { it.name ?: "Unknown Exercise" } ?: "No exercises"
                sb.append("- ${routine.name}: $exerciseList\n")
            }
        }

        if (plannedToday.isNotEmpty()) {
            sb.append("\nToday's Planned Content:\n")
            plannedToday.forEach { planned ->
                sb.append("- ${planned.name} (${planned.intensity})\n")
            }
        }

        // 7. Progressive Overload Prompts
        if (recentWorkouts.isNotEmpty()) {
            sb.append("\nTarget Progressive Overload (Coach's Note):\n")
            val latestWorkout = recentWorkouts.first()
            sb.append("Based on the workout '${latestWorkout.name}' from ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(latestWorkout.startTime))}:\n")
            sb.append("- Instruct the user to aim for either +2.5% weight or +1 rep on their main lifts from that session to ensure progress.\n")
        }

        sb.append("\nInstruction: Use this comprehensive data to provide expert-level coaching. Be aware of fatigue, reference saved routines when relevant, and always push for progressive overload based on the provided targets.")

        return sb.toString()
    }
}
