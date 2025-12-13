package com.example.hevyinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hevyinsight.data.model.ProfileInfo
import com.example.hevyinsight.data.preferences.UserPreferencesManager
import com.example.hevyinsight.data.repository.HevyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val profileInfo: ProfileInfo? = null,
    val weeklyGoal: Int = 5,
    val theme: String = "Dark",
    val units: String = "Metric",
    val viewOnlyMode: Boolean = false,
    val isSyncing: Boolean = false,
    val syncError: String? = null
)

class SettingsViewModel(
    private val repository: HevyRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            delay(100) // Small delay to ensure database is initialized
            loadSettingsData()
        }
    }
    
    private fun loadSettingsData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
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
                    "Dark"
                }
                
                val units = try {
                    userPreferencesManager.getUnits()
                } catch (e: Exception) {
                    "Metric"
                }
                
                val viewOnlyMode = try {
                    userPreferencesManager.getViewOnlyMode()
                } catch (e: Exception) {
                    false
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profileInfo = profileInfo,
                    weeklyGoal = weeklyGoal,
                    theme = theme,
                    units = units,
                    viewOnlyMode = viewOnlyMode,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load settings data: ${e.message}"
                )
            }
        }
    }
    
    fun syncData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true, syncError = null)
                repository.syncAllData()
                _uiState.value = _uiState.value.copy(isSyncing = false, syncError = null)
                // Reload profile info after sync
                loadSettingsData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncError = "Failed to sync data: ${e.message}"
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
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update weekly goal: ${e.message}"
                )
            }
        }
    }
    
    fun updateTheme(theme: String) {
        viewModelScope.launch {
            try {
                userPreferencesManager.setTheme(theme)
                _uiState.value = _uiState.value.copy(theme = theme)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update theme: ${e.message}"
                )
            }
        }
    }
    
    fun updateUnits(units: String) {
        viewModelScope.launch {
            try {
                userPreferencesManager.setUnits(units)
                _uiState.value = _uiState.value.copy(units = units)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update units: ${e.message}"
                )
            }
        }
    }
    
    fun updateViewOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferencesManager.setViewOnlyMode(enabled)
                _uiState.value = _uiState.value.copy(viewOnlyMode = enabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update view only mode: ${e.message}"
                )
            }
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            try {
                repository.clearCache()
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to clear cache: ${e.message}"
                )
            }
        }
    }
    
    fun refresh() {
        loadSettingsData()
    }
}

