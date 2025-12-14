package com.example.flexinsight.ui.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexinsight.core.errors.ErrorHandler
import com.example.flexinsight.ui.common.UiError
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Extension function to safely launch a coroutine in ViewModel scope
 * with automatic error handling.
 *
 * @param onError Callback to handle the error, typically updating the UI state.
 * @param block The suspend function to execute.
 */
fun ViewModel.safeLaunch(
    onError: (UiError) -> Unit,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val apiError = ErrorHandler.handleError(throwable)
        onError(UiError.fromApiError(apiError))
    }
    
    return viewModelScope.launch(exceptionHandler) {
        block()
    }
}
