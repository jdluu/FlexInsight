package com.jdluu.flexinsight.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.jdluu.flexinsight.data.repository.FlexRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes dashboard metrics into the Glance home widget state.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val flexRepository: FlexRepository
) {
    suspend fun updateFromDashboard(
        streak: Int,
        recoveryScore: Int,
        nextWorkoutLabel: String?
    ) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(FlexHomeWidget::class.java)
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs: MutablePreferences ->
                prefs[WidgetKeys.STREAK] = streak
                prefs[WidgetKeys.RECOVERY] = recoveryScore
                prefs[WidgetKeys.NEXT_WORKOUT] = nextWorkoutLabel ?: "No plan"
                prefs[WidgetKeys.LAST_UPDATED] = System.currentTimeMillis()
            }
        }
        glanceIds.forEach { glanceId ->
            FlexHomeWidget().update(context, glanceId)
        }
    }

    suspend fun updateFromRepository() {
        val stats = runCatching { flexRepository.calculateStats() }.getOrNull() ?: return
        val recovery = runCatching {
            flexRepository.getMuscleRecoveryStatus().values.average()
        }.getOrNull()?.let { (it * 100).toInt() } ?: 0
        val next = runCatching {
            flexRepository.getPlannedWorkoutsForDay(System.currentTimeMillis()).firstOrNull()?.name
        }.getOrNull()
        updateFromDashboard(
            streak = stats.currentStreak,
            recoveryScore = recovery,
            nextWorkoutLabel = next
        )
    }
}
