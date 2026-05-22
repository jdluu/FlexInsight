package com.jdluu.flexinsight.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.jdluu.flexinsight.data.model.Workout
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager
) {
    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    )

    fun getSdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    fun isSdkAvailable(): Boolean =
        getSdkStatus() == HealthConnectClient.SDK_AVAILABLE

    suspend fun getClientOrNull(): HealthConnectClient? = withContext(Dispatchers.IO) {
        if (!isSdkAvailable()) return@withContext null
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Health Connect client unavailable", e)
            null
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = getClientOrNull() ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            requiredPermissions.all { it in granted }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun readSnapshot(): HealthConnectSnapshot = withContext(Dispatchers.IO) {
        if (!userPreferencesManager.getHealthConnectEnabled()) {
            return@withContext HealthConnectSnapshot(isAvailable = isSdkAvailable())
        }
        val client = getClientOrNull()
            ?: return@withContext HealthConnectSnapshot(isAvailable = false)

        if (!hasAllPermissions()) {
            return@withContext HealthConnectSnapshot(
                isAvailable = true,
                isPermissionGranted = false
            )
        }

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val startOfToday = now.toLocalDate().atStartOfDay(zone).toInstant()
        val endNow = now.toInstant()
        val weekAgo = now.minusDays(7).toInstant()
        val sleepStart = now.minusDays(1).toLocalDate().atStartOfDay(zone).minusHours(12).toInstant()

        var sleepHours: Double? = null
        var restingHr: Long? = null
        var steps: Long? = null
        var calories: Double? = null
        var cardioSessions = 0

        try {
            val sleepResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(sleepStart, endNow)
                )
            )
            sleepHours = sleepResponse.records.maxByOrNull {
                java.time.Duration.between(it.startTime, it.endTime).toMillis()
            }?.let { session ->
                java.time.Duration.between(session.startTime, session.endTime).toMinutes() / 60.0
            }

            val hrResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(weekAgo, endNow)
                )
            )
            restingHr = hrResponse.records
                .flatMap { it.samples }
                .mapNotNull { it.beatsPerMinute }
                .filter { it in 40..100 }
                .minOrNull()

            val stepsResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfToday, endNow)
                )
            )
            steps = stepsResponse.records.sumOf { it.count }

            val calResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfToday, endNow)
                )
            )
            calories = calResponse.records.sumOf { it.energy.inKilocalories }

            val exerciseResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(weekAgo, endNow)
                )
            )
            cardioSessions = exerciseResponse.records.count { session ->
                session.exerciseType != ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING &&
                    session.exerciseType != ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed reading Health Connect data", e)
        }

        HealthConnectSnapshot(
            isAvailable = true,
            isPermissionGranted = true,
            sleepHoursLastNight = sleepHours,
            restingHeartRateBpm = restingHr,
            stepsToday = steps,
            activeCaloriesToday = calories,
            cardioSessionsThisWeek = cardioSessions
        )
    }

    /**
     * Writes completed Hevy strength sessions to Health Connect (when enabled and not view-only).
     */
    suspend fun writeWorkoutsToHealthConnect(workouts: List<Workout>): Int = withContext(Dispatchers.IO) {
        if (!userPreferencesManager.getHealthConnectEnabled()) return@withContext 0
        if (!userPreferencesManager.getHealthConnectWriteEnabled()) return@withContext 0
        if (userPreferencesManager.getViewOnlyMode()) return@withContext 0

        val client = getClientOrNull() ?: return@withContext 0
        if (!hasAllPermissions()) return@withContext 0

        var written = 0
        workouts.filter { !it.isDeleted && it.endTime != null }.take(5).forEach { workout ->
            try {
                val start = Instant.ofEpochMilli(workout.startTime)
                val end = Instant.ofEpochMilli(workout.endTime!!)
                val zone = ZoneId.systemDefault()
                val startOffset = zone.rules.getOffset(start)
                val endOffset = zone.rules.getOffset(end)
                val record = ExerciseSessionRecord(
                    startTime = start,
                    startZoneOffset = startOffset,
                    endTime = end,
                    endZoneOffset = endOffset,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                    title = workout.name ?: "FlexInsight Workout",
                    notes = "Synced from Hevy via FlexInsight"
                )
                client.insertRecords(listOf(record))
                written++
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to write workout ${workout.id} to Health Connect", e)
            }
        }
        written
    }

    companion object {
        private const val TAG = "HealthConnectRepository"
    }
}
