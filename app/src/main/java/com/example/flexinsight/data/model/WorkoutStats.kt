package com.example.flexinsight.data.model

import androidx.compose.runtime.Stable

/**
 * Aggregated statistics calculated from workouts
 */
@Stable
data class WorkoutStats(
    val totalWorkouts: Int,
    val totalVolume: Double, // in kg
    val averageVolume: Double,
    val totalSets: Int,
    val totalDuration: Long, // in minutes
    val averageDuration: Long, // in minutes
    val currentStreak: Int, // consecutive days
    val longestStreak: Int,
    val bestWeekVolume: Double,
    val bestWeekDate: Long? // timestamp
)

/**
 * Weekly progress data
 */
@Stable
data class WeeklyProgress(
    val weekStartDate: Long,
    val totalVolume: Double,
    val workoutCount: Int,
    val averageVolume: Double
)

/**
 * Muscle group progress
 */
@Stable
data class MuscleGroupProgress(
    val muscleGroup: String,
    val volume: Double,
    val sets: Int,
    val intensity: String // "HI", "MD", "LO"
)

/**
 * Personal record entry
 */
@Stable
data class PersonalRecord(
    val exerciseName: String,
    val exerciseTemplateId: String?,
    val recordType: String, // "weight", "reps", "volume"
    val value: Double,
    val date: Long,
    val workoutId: String
)

/**
 * Statistics for a single workout
 */
@Stable
data class SingleWorkoutStats(
    val durationMinutes: Long,
    val totalSets: Int,
    val totalVolume: Double
)

/**
 * Personal record details with exercise and workout information
 */
@Stable
data class PRDetails(
    val exerciseName: String,
    val date: Long, // timestamp
    val muscleGroup: String,
    val weight: Double,
    val workoutId: String,
    val setId: String
)

/**
 * Volume trend data comparing current and previous periods
 */
@Stable
data class VolumeTrend(
    val currentVolume: Double,
    val previousVolume: Double,
    val percentageChange: Double
)

/**
 * Weekly volume data for chart display
 */
@Stable
data class WeeklyVolumeData(
    val weekLabel: String, // "W1", "W2", etc.
    val volume: Double
)

/**
 * Daily duration data grouped by day of week
 */
@Stable
data class DailyDurationData(
    val dayOfWeek: String, // "M", "T", "W", etc.
    val averageDuration: Long // in minutes
)

/**
 * Weekly goal progress
 */
@Stable
data class WeeklyGoalProgress(
    val completed: Int,
    val target: Int,
    val status: String // "On Track", "Behind", "Ahead"
)

/**
 * Day information for week calendar
 */
@Stable
data class DayInfo(
    val name: String, // "Mon", "Tue", etc.
    val date: Int, // Day of month
    val timestamp: Long, // Full timestamp for the day
    val hasWorkout: Boolean,
    val isCompleted: Boolean,
    val workoutCount: Int
)

/**
 * Planned workout for a specific day
 */
@Stable
data class PlannedWorkout(
    val id: String?,
    val name: String,
    val duration: Long?, // in minutes
    val intensity: String?, // "High Intensity", "Aerobic", etc.
    val isCompleted: Boolean,
    val routineId: String?,
    val exerciseCount: Int
)

/**
 * Volume balance across different categories
 */
@Stable
data class VolumeBalance(
    val push: Float, // 0.0 to 1.0
    val pull: Float,
    val legs: Float,
    val cardio: Float
)

/**
 * Profile information calculated from workout data
 */
@Stable
data class ProfileInfo(
    val displayName: String?, // Can be derived or set by user
    val memberSince: Long?, // Timestamp of first workout
    val isProMember: Boolean, // Based on API key availability
    val totalWorkouts: Int,
    val accountAgeDays: Int
)

/**
 * User preferences stored locally
 */
@Stable
data class UserPreferences(
    val weeklyGoal: Int, // Default 5, can be customized
    val theme: String, // "Dark", "Light", "System"
    val units: String, // "Metric", "Imperial"
    val viewOnlyMode: Boolean // If true, hide all edit/create buttons
)

