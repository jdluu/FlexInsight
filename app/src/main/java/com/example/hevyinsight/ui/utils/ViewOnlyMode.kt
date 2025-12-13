package com.example.hevyinsight.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.hevyinsight.HevyInsightApplication
import com.example.hevyinsight.data.preferences.UserPreferencesManager

/**
 * Get view only mode state from UserPreferencesManager
 */
@Composable
fun rememberViewOnlyMode(): Boolean {
    val context = LocalContext.current
    val preferencesManager = remember { UserPreferencesManager(context) }
    val viewOnlyMode by preferencesManager.viewOnlyModeFlow.collectAsState(initial = false)
    return viewOnlyMode
}

