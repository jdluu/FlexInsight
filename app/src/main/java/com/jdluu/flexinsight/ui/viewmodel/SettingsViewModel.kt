package com.jdluu.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.flexinsight.core.errors.ErrorHandler
import com.jdluu.flexinsight.data.model.ProfileInfo
import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.data.sync.SyncCoordinator
import com.jdluu.flexinsight.domain.usecase.ExportCoachReportUseCase
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.core.network.NetworkMonitor
import com.jdluu.flexinsight.core.network.NetworkState
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.jdluu.flexinsight.ui.utils.safeLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class SettingsUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val error: UiError? = null,
    val apiKey: String? = null,
    val apiKeyError: String? = null,
    val profileInfo: ProfileInfo? = null,
    val weeklyGoal: Int = 5,
    val theme: String = "System",
    val units: String = "Imperial",
    val syncState: LoadingState = LoadingState.Idle,
    val syncError: UiError? = null,
    val networkState: NetworkState = NetworkState.Unknown,
    val forceAiEnable: Boolean = false,
    val notificationsEnabled: Boolean = true,
    /** When true, Hevy write actions (complete, reschedule, save routine) are disabled. */
    val viewOnlyMode: Boolean = true,
    val healthConnectEnabled: Boolean = false,
    val healthConnectWriteEnabled: Boolean = false,
    val healthConnectAvailable: Boolean = false
) {
    // Backward compatibility helpers
    val isLoading: Boolean
        get() = loadingState.isLoading
    val isSyncing: Boolean
        get() = syncState.isLoading
}


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: FlexRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val apiKeyManager: com.jdluu.flexinsight.data.preferences.ApiKeyManager,
    private val networkMonitor: NetworkMonitor,
    private val healthConnectRepository: HealthConnectRepository,
    private val syncCoordinator: SyncCoordinator,
    private val exportCoachReportUseCase: ExportCoachReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.networkState.collect { state ->
                _uiState.value = _uiState.value.copy(networkState = state)
            }
        }

        viewModelScope.launch {
            delay(100) // Small delay to ensure database is initialized
            loadSettingsData()
            
            // Observe API key changes
            apiKeyManager.apiKeyFlow.collect { key ->
                _uiState.value = _uiState.value.copy(apiKey = key)
            }
        }
        
        viewModelScope.launch {
            userPreferencesManager.forceAiEnableFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(forceAiEnable = enabled)
            }
        }

        viewModelScope.launch {
            userPreferencesManager.viewOnlyModeFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(viewOnlyMode = enabled)
            }
        }

        viewModelScope.launch {
            userPreferencesManager.healthConnectEnabledFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(healthConnectEnabled = enabled)
            }
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
                    android.util.Log.e("SettingsViewModel", "Failed to load profile info", e)
                    null
                }

                // Load preferences
                val weeklyGoal = try {
                    userPreferencesManager.getWeeklyGoal()
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Failed to load weekly goal", e)
                    5
                }

                val theme = try {
                    userPreferencesManager.getTheme()
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Failed to load theme preference", e)
                    "System"
                }

                val units = try {
                    userPreferencesManager.getUnits()
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Failed to load units preference", e)
                    "Imperial"
                }
                
                val forceAi = try {
                    userPreferencesManager.getForceAiEnable()
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Failed to load force AI preference", e)
                    false
                }
                
                val notifications = try {
                    userPreferencesManager.getNotificationsEnabled()
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Failed to load notifications preference", e)
                    true
                }

                val viewOnlyMode = try {
                    userPreferencesManager.getViewOnlyMode()
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Failed to load view-only preference", e)
                    true
                }

                val healthEnabled = userPreferencesManager.getHealthConnectEnabled()
                val healthWrite = userPreferencesManager.getHealthConnectWriteEnabled()

                val displayName = try {
                    userPreferencesManager.getDisplayName()
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Failed to load display name", e)
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
                    forceAiEnable = forceAi,
                    notificationsEnabled = notifications,
                    viewOnlyMode = viewOnlyMode,
                    healthConnectEnabled = healthEnabled,
                    healthConnectWriteEnabled = healthWrite,
                    healthConnectAvailable = healthConnectRepository.isSdkAvailable(),
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
                when (val result = repository.syncAllData()) {
                    is com.jdluu.flexinsight.core.errors.Result.Success -> {
                        syncCoordinator.onSyncComplete()
                        _uiState.value = _uiState.value.copy(syncState = LoadingState.Success, syncError = null)
                    }
                    is com.jdluu.flexinsight.core.errors.Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            syncState = LoadingState.Error(result.error),
                            syncError = UiError.fromApiError(result.error)
                        )
                        return@launch
                    }
                }
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

    fun updateForceAiEnable(enabled: Boolean) {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(error = UiError.fromApiError(apiError))
        }) {
            userPreferencesManager.setForceAiEnable(enabled)
            _uiState.value = _uiState.value.copy(forceAiEnable = enabled)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(error = UiError.fromApiError(apiError))
        }) {
            userPreferencesManager.setNotificationsEnabled(enabled)
            _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        }
    }

    /**
     * @param viewOnly When true, disables Hevy write operations in FlexInsight (recommended).
     */
    fun updateViewOnlyMode(viewOnly: Boolean) {
        safeLaunch(onError = { apiError ->
            _uiState.value = _uiState.value.copy(error = UiError.fromApiError(apiError))
        }) {
            userPreferencesManager.setViewOnlyMode(viewOnly)
            _uiState.value = _uiState.value.copy(viewOnlyMode = viewOnly)
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun validateAndSaveApiKey(key: String, onSuccess: () -> Unit = {}) {
        if (apiKeyManager.isValidApiKeyFormat(key)) {
            viewModelScope.launch {
                try {
                    apiKeyManager.saveApiKey(key)
                    _uiState.value = _uiState.value.copy(apiKey = key, apiKeyError = null)
                    refresh()
                    onSuccess()
                } catch (e: Exception) {
                    val apiError = ErrorHandler.handleError(e)
                    _uiState.value = _uiState.value.copy(error = UiError.fromApiError(apiError))
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(apiKeyError = "API key must be at least 10 characters")
        }
    }

    fun clearApiKeyError() {
        _uiState.value = _uiState.value.copy(apiKeyError = null)
    }

    fun setHealthConnectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.setHealthConnectEnabled(enabled)
            _uiState.value = _uiState.value.copy(healthConnectEnabled = enabled)
        }
    }

    fun setHealthConnectWriteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.setHealthConnectWriteEnabled(enabled)
            _uiState.value = _uiState.value.copy(healthConnectWriteEnabled = enabled)
        }
    }

    fun getHealthConnectPermissions(): Set<String> = healthConnectRepository.requiredPermissions

    suspend fun exportCoachReport(): String = exportCoachReportUseCase()

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesManager.setOnboardingComplete(true)
            userPreferencesManager.setOnboardingStep(4)
        }
    }

    fun setOnboardingStep(step: Int) {
        viewModelScope.launch { userPreferencesManager.setOnboardingStep(step) }
    }

    suspend fun getOnboardingStep(): Int = userPreferencesManager.getOnboardingStep()

    suspend fun isOnboardingComplete(): Boolean =
        userPreferencesManager.onboardingCompleteFlow.first()
}
