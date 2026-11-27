package com.jdluu.flexinsight.domain.model

data class WorkoutStats(
    val totalWorkouts: Int,
    val totalVolume: Double,
    val averageVolume: Double,
    val totalSets: Int,
    val totalDuration: Long,
    val averageDuration: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val bestWeekVolume: Double,
    val bestWeekDate: Long?
)

data class WeeklyProgress(
    val weekStartDate: Long,
    val totalVolume: Double,
    val workoutCount: Int,
    val averageVolume: Double
)

data class MuscleGroupProgress(
    val muscleGroup: String,
    val volume: Double,
    val sets: Int,
    val intensity: String
)

data class PersonalRecord(
    val exerciseName: String,
    val exerciseTemplateId: String?,
    val recordType: String,
    val value: Double,
    val date: Long,
    val workoutId: String
)

data class SingleWorkoutStats(
    val durationMinutes: Long,
    val totalSets: Int,
    val totalVolume: Double
)

data class PRDetails(
    val exerciseName: String,
    val date: Long,
    val muscleGroup: String,
    val weight: Double,
    val workoutId: String,
    val setId: String
)

data class PeriodComparison(
    val currentPeriodLabel: String,
    val previousPeriodLabel: String,
    val totalVolumeCurrent: Double,
    val totalVolumePrevious: Double,
    val totalWorkoutsCurrent: Int,
    val totalWorkoutsPrevious: Int,
    val avgDurationCurrent: Long,
    val avgDurationPrevious: Long
) {
    val volumeChangePercent: Double
        get() = if (totalVolumePrevious > 0) {
            ((totalVolumeCurrent - totalVolumePrevious) / totalVolumePrevious) * 100.0
        } else {
            0.0
        }

    val workoutCountDelta: Int
        get() = totalWorkoutsCurrent - totalWorkoutsPrevious

    val hasCurrentData: Boolean
        get() = totalWorkoutsCurrent > 0 || totalVolumeCurrent > 0.0
}

data class VolumeTrend(
    val currentVolume: Double,
    val previousVolume: Double,
    val percentageChange: Double
)

data class WeeklyVolumeData(
    val weekLabel: String,
    val volume: Double
)

data class DailyDurationData(
    val dayOfWeek: String,
    val averageDuration: Long
)

data class WeeklyGoalProgress(
    val completed: Int,
    val target: Int,
    val status: String
)

data class DayInfo(
    val name: String,
    val date: Int,
    val timestamp: Long,
    val hasWorkout: Boolean,
    val isCompleted: Boolean,
    val workoutCount: Int
)

data class PlannedWorkout(
    val id: String?,
    val name: String,
    val duration: Long?,
    val intensity: String?,
    val isCompleted: Boolean,
    val routineId: String?,
    val exerciseCount: Int
)

data class VolumeBalance(
    val push: Float,
    val pull: Float,
    val legs: Float,
    val cardio: Float
)

data class ProfileInfo(
    val displayName: String?,
    val memberSince: Long?,
    val isProMember: Boolean,
    val totalWorkouts: Int,
    val accountAgeDays: Int
)

data class UserPreferences(
    val weeklyGoal: Int,
    val theme: String,
    val units: String,
    val viewOnlyMode: Boolean
)
