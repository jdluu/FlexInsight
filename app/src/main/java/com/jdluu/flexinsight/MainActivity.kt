package com.jdluu.flexinsight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.data.preferences.SyncPreferencesManager
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.data.sync.SyncManager
import com.jdluu.flexinsight.ui.common.LocalSnackbarHostState
import com.jdluu.flexinsight.ui.components.FlexBottomNavigation
import com.jdluu.flexinsight.ui.navigation.FlexAppNavigation
import com.jdluu.flexinsight.ui.navigation.Screen
import com.jdluu.flexinsight.ui.theme.FlexInsightTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    @Inject
    lateinit var apiKeyManager: ApiKeyManager

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var syncPreferencesManager: SyncPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreference by userPreferencesManager.themeFlow.collectAsState(initial = "System")

            val darkTheme = when (themePreference) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme() // "System" or any other value
            }

            FlexInsightTheme(darkTheme = darkTheme) {
                MainScreen(
                    apiKeyManager = apiKeyManager,
                    syncManager = syncManager,
                    syncPreferencesManager = syncPreferencesManager
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    apiKeyManager: ApiKeyManager,
    syncManager: SyncManager,
    syncPreferencesManager: SyncPreferencesManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val showBottomNav = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.History.route,
        Screen.Planner.route,
        Screen.Recovery.route,
        Screen.Settings.route
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch {
                syncPreferencesManager.pendingNewWorkoutsFlow.collect { pending ->
                    if (pending > 0) {
                        snackbarHostState.showSnackbar(
                            message = context.getString(
                                R.string.sync_snackbar_new_workouts,
                                pending
                            ),
                            actionLabel = context.getString(R.string.done)
                        )
                        syncPreferencesManager.clearPendingNewWorkouts()
                    }
                }
            }
            launch {
                syncPreferencesManager.pendingDeletedWorkoutsFlow.collect { deleted ->
                    if (deleted > 0) {
                        snackbarHostState.showSnackbar(
                            message = context.getString(
                                R.string.sync_snackbar_deleted_workouts,
                                deleted
                            ),
                            actionLabel = context.getString(R.string.done)
                        )
                        syncPreferencesManager.clearPendingDeletedWorkouts()
                    }
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomNav) {
                    FlexBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            // Always navigate to the selected tab, clearing any screens on top
                            navController.navigate(route) {
                                // Pop up to the start destination, but don't pop the start destination itself
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = false
                                    inclusive = false
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            FlexAppNavigation(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
