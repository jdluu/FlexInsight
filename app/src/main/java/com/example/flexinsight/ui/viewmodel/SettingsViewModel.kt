package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.core.errors.ErrorHandler
import com.example.flexinsight.data.model.ProfileInfo
import com.example.flexinsight.data.preferences.UserPreferencesManager
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.flexinsight.ui.utils.safeLaunch

data class SettingsUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val error: UiError? = null,
    val profileInfo: ProfileInfo? = null,
    val weeklyGoal: Int = 5,
    val theme: String = "System",
    val units: String = "Imperial",
    val syncState: LoadingState = LoadingState.Idle,
    val syncError: UiError? = null
) {
    // Backward compatibility helpers
    val isLoading: Boolean
        get() = loadingState.isLoading
    val isSyncing: Boolean
        get() = syncState.isLoading
}

class SettingsViewModel(
    private val repository: FlexRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(100) // Small delay to ensure database is initialized
            loadSettingsData()
        }
    }

    private fun loadSettingsData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                if (!isRefresh) {
                    _uiState.value = _uiState.value.copy(loadingState = LoadingState.Loading, error = null)
                }

                // Load profile info
                val profileInfo = try {
                    repository.getProfileInfo()
                } catch (e: Exception) {
                    null
                }

                // Load preferences
                val weeklyGoal = try {
                    userPreferencesManager.getWeeklyGoal()
                } catch (e: Exception) {
                    5
                }

                val theme = try {
                    userPreferencesManager.getTheme()
                } catch (e: Exception) {
                    "System"
                }

                val units = try {
                    userPreferencesManager.getUnits()
                } catch (e: Exception) {
                    "Imperial"
                }

                val displayName = try {
                    userPreferencesManager.getDisplayName()
                } catch (e: Exception) {
                    null
                }

                // Merge display name into profile info if exists
                val finalProfileInfo = if (displayName != null && profileInfo != null) {
                    profileInfo.copy(displayName = displayName)
                } else {
                    profileInfo
                }

                _uiState.value = _uiState.value.copy(
                    loadingState = LoadingState.Success,
                    profileInfo = finalProfileInfo,
                    weeklyGoal = weeklyGoal,
                    theme = theme,
                    units = units,
                    error = null
                )
            } catch (e: Exception) {
                val apiError = ErrorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    loadingState = LoadingState.Error(apiError),
                    error = UiError.fromApiError(apiError)
                )
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(syncState = LoadingState.Loading, syncError = null)
                repository.syncAllData()
                _uiState.value = _uiState.value.copy(syncState = LoadingState.Success, syncError = null)
                // Reload profile info after sync (silent refresh)
                loadSettingsData(isRefresh = true)
            } catch (e: Exception) {
                val apiError = ErrorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    syncState = LoadingState.Error(apiError),
                    syncError = UiError.fromApiError(apiError)
                )
            }
        }
    }

    fun updateWeeklyGoal(goal: Int) {
        viewModelScope.launch {
            try {
                userPreferencesManager.setWeeklyGoal(goal)
                _uiState.value = _uiState.value.copy(weeklyGoal = goal)
            } catch (e: Exception) {
                val apiError = ErrorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    error = UiError.fromApiError(apiError)
                )
            }
        }
    }

    fun updateTheme(theme: String) {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(error = UiError.fromApiError(apiError))
        }) {
            userPreferencesManager.setTheme(theme)
            _uiState.value = _uiState.value.copy(theme = theme)
        }
    }

    fun updateUnits(units: String) {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(error = UiError.fromApiError(apiError))
        }) {
            userPreferencesManager.setUnits(units)
            _uiState.value = _uiState.value.copy(units = units)
        }
    }



    fun clearCache() {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(error = UiError.fromApiError(apiError))
        }) {
            repository.clearCache()
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    fun updateDisplayName(name: String) {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(error = UiError.fromApiError(apiError))
        }) {
            userPreferencesManager.setDisplayName(name)
            // Update local state immediately for responsiveness
            val currentProfile = _uiState.value.profileInfo
            if (currentProfile != null) {
                _uiState.value = _uiState.value.copy(
                    profileInfo = currentProfile.copy(displayName = name)
                )
            }
        }
    }

    fun refresh() {
        loadSettingsData(isRefresh = true)
    }
}
