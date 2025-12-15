package com.example.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.data.model.PRDetails
import com.example.flexinsight.data.preferences.UserPreferencesManager
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.common.UiError
import com.example.flexinsight.ui.utils.safeLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PRListUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val prs: List<PRDetails> = emptyList(),
    val error: UiError? = null,
    val units: String = "Imperial"
)

@HiltViewModel
class PRListViewModel @Inject constructor(
    private val repository: FlexRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PRListUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<PRListUiState> = _uiState.asStateFlow()

    init {
        // Collect units preference
        viewModelScope.launch {
            userPreferencesManager.unitsFlow.collect { units ->
                _uiState.value = _uiState.value.copy(units = units)
            }
        }
        loadPRs()
    }

    fun loadPRs() {
        safeLaunch(onError = { apiError ->
             _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Error(apiError),
                error = UiError.fromApiError(apiError)
            )
        }) {
            _uiState.value = _uiState.value.copy(loadingState = LoadingState.Loading, error = null)
            
            // Fetch all PRs (up to 100 for now)
            val prs = repository.getAllPRsWithDetails()
            
            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Success,
                prs = prs,
                error = null
            )
        }
    }
}
