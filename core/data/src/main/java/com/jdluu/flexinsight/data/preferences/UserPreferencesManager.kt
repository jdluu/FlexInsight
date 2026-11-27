package com.jdluu.flexinsight.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesManager(private val context: Context) {
    companion object {
        private val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        private val THEME = stringPreferencesKey("theme")
        private val UNITS = stringPreferencesKey("units")
        private val VIEW_ONLY_MODE = booleanPreferencesKey("view_only_mode")
        private val HEALTH_CONNECT_ENABLED = booleanPreferencesKey("health_connect_enabled")
        private val HEALTH_CONNECT_WRITE = booleanPreferencesKey("health_connect_write_enabled")
        private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val ONBOARDING_STEP = intPreferencesKey("onboarding_step")
        private val RECOVERY_MOOD = floatPreferencesKey("recovery_mood")
        private val RECOVERY_NOTES = stringPreferencesKey("recovery_notes")

        private const val DEFAULT_WEEKLY_GOAL = 5
        private const val DEFAULT_RECOVERY_MOOD = 7.5f
        private const val DEFAULT_THEME = "System"
        private const val DEFAULT_UNITS = "Imperial"
        private const val DEFAULT_VIEW_ONLY_MODE = true
    }

    /**
     * Get weekly goal preference
     */
    val weeklyGoalFlow: Flow<Int> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[WEEKLY_GOAL] ?: DEFAULT_WEEKLY_GOAL
    }

    suspend fun getWeeklyGoal(): Int {
        return weeklyGoalFlow.first()
    }

    suspend fun setWeeklyGoal(goal: Int) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[WEEKLY_GOAL] = goal
        }
    }

    /**
     * Get theme preference
     */
    val themeFlow: Flow<String> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[THEME] ?: DEFAULT_THEME
    }

    suspend fun getTheme(): String {
        return themeFlow.first()
    }

    suspend fun setTheme(theme: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[THEME] = theme
        }
    }

    /**
     * Get units preference
     */
    val unitsFlow: Flow<String> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[UNITS] ?: DEFAULT_UNITS
    }

    suspend fun getUnits(): String {
        return unitsFlow.first()
    }

    suspend fun setUnits(units: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[UNITS] = units
        }
    }

    /**
     * View-only mode: when true (default), FlexInsight does not write to Hevy (complete, reschedule, save routine).
     * Users should edit in the Hevy app unless they explicitly disable this setting.
     */
    val viewOnlyModeFlow: Flow<Boolean> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[VIEW_ONLY_MODE] ?: DEFAULT_VIEW_ONLY_MODE
    }

    suspend fun getViewOnlyMode(): Boolean {
        return viewOnlyModeFlow.first()
    }

    suspend fun setViewOnlyMode(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[VIEW_ONLY_MODE] = enabled
        }
    }

    /**
     * Get display name preference
     */
    private val DISPLAY_NAME = stringPreferencesKey("display_name")

    val displayNameFlow: Flow<String?> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[DISPLAY_NAME]
    }

    suspend fun getDisplayName(): String? {
        return displayNameFlow.first()
    }

    suspend fun setDisplayName(name: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[DISPLAY_NAME] = name
        }
    }

    /**
     * Get force AI enable preference (Developer Option)
     */
    private val FORCE_AI_ENABLE = booleanPreferencesKey("force_ai_enable")

    val forceAiEnableFlow: Flow<Boolean> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[FORCE_AI_ENABLE] ?: false
    }

    suspend fun getForceAiEnable(): Boolean {
        return forceAiEnableFlow.first()
    }

    suspend fun setForceAiEnable(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[FORCE_AI_ENABLE] = enabled
        }
    }

    /**
     * Get notifications enable preference
     */
    private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

    val notificationsEnabledFlow: Flow<Boolean> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun getNotificationsEnabled(): Boolean {
        return notificationsEnabledFlow.first()
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val healthConnectEnabledFlow: Flow<Boolean> = context.userPreferencesDataStore.data.map {
        it[HEALTH_CONNECT_ENABLED] ?: false
    }

    suspend fun getHealthConnectEnabled(): Boolean = healthConnectEnabledFlow.first()

    suspend fun setHealthConnectEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[HEALTH_CONNECT_ENABLED] = enabled }
    }

    suspend fun getHealthConnectWriteEnabled(): Boolean =
        context.userPreferencesDataStore.data.first()[HEALTH_CONNECT_WRITE] ?: false

    suspend fun setHealthConnectWriteEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[HEALTH_CONNECT_WRITE] = enabled }
    }

    val onboardingCompleteFlow: Flow<Boolean> = context.userPreferencesDataStore.data.map {
        it[ONBOARDING_COMPLETE] ?: false
    }

    suspend fun getOnboardingStep(): Int =
        context.userPreferencesDataStore.data.first()[ONBOARDING_STEP] ?: 0

    suspend fun setOnboardingStep(step: Int) {
        context.userPreferencesDataStore.edit { it[ONBOARDING_STEP] = step }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.userPreferencesDataStore.edit { it[ONBOARDING_COMPLETE] = complete }
    }

    val recoveryMoodFlow: Flow<Float> = context.userPreferencesDataStore.data.map {
        it[RECOVERY_MOOD] ?: DEFAULT_RECOVERY_MOOD
    }

    val recoveryNotesFlow: Flow<String> = context.userPreferencesDataStore.data.map {
        it[RECOVERY_NOTES] ?: ""
    }

    suspend fun setRecoveryMood(value: Float) {
        context.userPreferencesDataStore.edit {
            it[RECOVERY_MOOD] = value.coerceIn(0f, 10f)
        }
    }

    suspend fun setRecoveryNotes(notes: String) {
        context.userPreferencesDataStore.edit { it[RECOVERY_NOTES] = notes }
    }
}

