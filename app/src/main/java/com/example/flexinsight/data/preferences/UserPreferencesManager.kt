package com.example.flexinsight.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

        private const val DEFAULT_WEEKLY_GOAL = 5
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
     * Get view only mode preference
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
}

