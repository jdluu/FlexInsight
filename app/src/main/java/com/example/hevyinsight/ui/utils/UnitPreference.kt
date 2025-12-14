package com.example.hevyinsight.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.hevyinsight.HevyInsightApplication
import kotlinx.coroutines.flow.flowOf

@Composable
fun rememberUnitPreference(): Boolean {
    val context = LocalContext.current
    val application = context.applicationContext as? HevyInsightApplication
    val preferencesManager = remember(application) {
        application?.userPreferencesManager
    }
    
    val unitsFlow = remember(preferencesManager) {
        preferencesManager?.unitsFlow ?: flowOf("Imperial")
    }
    
    val units by unitsFlow.collectAsState(initial = "Imperial")
    return units == "Metric"
}
