package com.example.flexinsight.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.flexinsight.FlexInsightApplication
import kotlinx.coroutines.flow.flowOf

@Composable
fun rememberUnitPreference(): Boolean {
    val context = LocalContext.current
    val application = context.applicationContext as? FlexInsightApplication
    val preferencesManager = remember(application) {
        application?.userPreferencesManager
    }

    val unitsFlow = remember(preferencesManager) {
        preferencesManager?.unitsFlow ?: flowOf("Imperial")
    }

    val units by unitsFlow.collectAsState(initial = "Imperial")
    return units == "Metric"
}
