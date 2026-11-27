package com.jdluu.flexinsight

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            val themePreference by userPreferencesManager.themeFlow
                .collectAsStateWithLifecycle(initialValue = "System")

            val darkTheme = when (themePreference) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
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
        Screen.AITrainer.route
    )

    val snackbarHostState = remember { SnackbarHostState() }

    val pendingNewWorkouts: Int? by syncPreferencesManager.pendingNewWorkoutsFlow
        .collectAsStateWithLifecycle(initialValue = null)
    val pendingDeletedWorkouts: Int? by syncPreferencesManager.pendingDeletedWorkoutsFlow
        .collectAsStateWithLifecycle(initialValue = null)

    val doneLabel = stringResource(R.string.done)
    val newWorkoutsMessage = pendingNewWorkouts?.takeIf { it > 0 }?.let {
        stringResource(R.string.sync_snackbar_new_workouts, it)
    }
    val deletedWorkoutsMessage = pendingDeletedWorkouts?.takeIf { it > 0 }?.let {
        stringResource(R.string.sync_snackbar_deleted_workouts, it)
    }

    LaunchedEffect(newWorkoutsMessage) {
        newWorkoutsMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, actionLabel = doneLabel)
            syncPreferencesManager.clearPendingNewWorkouts()
        }
    }

    LaunchedEffect(deletedWorkoutsMessage) {
        deletedWorkoutsMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, actionLabel = doneLabel)
            syncPreferencesManager.clearPendingDeletedWorkouts()
        }
    }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomNav) {
                    FlexBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = false
                                    inclusive = false
                                }
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
