package com.jdluu.flexinsight

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.data.preferences.SyncPreferencesManager
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.data.sync.SyncManager
import com.jdluu.flexinsight.ui.common.LocalSnackbarHostState
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.FlexBottomNavigation
import com.jdluu.flexinsight.ui.navigation.FlexAppNavigation
import com.jdluu.flexinsight.ui.navigation.Screen
import com.jdluu.flexinsight.ui.theme.FlexInsightTheme
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricLarge
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.timer.RestTimerController
import com.jdluu.flexinsight.ui.timer.RestTimerState
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

    @Inject
    lateinit var restTimerController: RestTimerController

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
                    syncPreferencesManager = syncPreferencesManager,
                    restTimerController = restTimerController
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    apiKeyManager: ApiKeyManager,
    syncManager: SyncManager,
    syncPreferencesManager: SyncPreferencesManager,
    restTimerController: RestTimerController
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

    val timerState by restTimerController.state.collectAsStateWithLifecycle()
    val isOnPlateRack = currentRoute.startsWith(Screen.PlateRack.BASE_ROUTE)
    val showLiveTimerBanner = timerState.isVisibleInBanner &&
        !isOnPlateRack &&
        currentRoute != Screen.Onboarding.route

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
                if (showBottomNav || showLiveTimerBanner) {
                    Column {
                        if (showLiveTimerBanner) {
                            LiveRestTimerStrip(
                                state = timerState,
                                onOpenPlateRack = {
                                    navController.navigate(Screen.PlateRack.createRoute())
                                },
                                onTogglePause = restTimerController::togglePause,
                                onAdd15Seconds = { restTimerController.addSeconds(15) },
                                onDismiss = restTimerController::dismissBanner,
                                modifier = if (showBottomNav) Modifier else Modifier.navigationBarsPadding()
                            )
                        }
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

@Composable
private fun LiveRestTimerStrip(
    state: RestTimerState,
    onOpenPlateRack: () -> Unit,
    onTogglePause: () -> Unit,
    onAdd15Seconds: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (state.hasCompleted) {
        FlexTheme.colors.recoveredContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (state.hasCompleted) {
        FlexTheme.colors.onRecoveredContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val accentColor = if (state.hasCompleted) {
        FlexTheme.colors.recovered
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
            CalibratedPlateBar(
                progress = state.progress,
                segments = 12,
                activeColor = accentColor,
                barHeight = 4.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = stringResource(id = R.string.plate_rack_banner_open),
                        onClick = onOpenPlateRack
                    )
                    .padding(start = 16.dp, end = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.hasCompleted) {
                            stringResource(id = R.string.plate_rack_timer_complete)
                        } else {
                            state.formattedRemaining
                        },
                        style = MonoMetricLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = stringResource(id = R.string.plate_rack_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.hasCompleted) {
                            contentColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                TextButton(
                    onClick = onAdd15Seconds,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.plate_rack_timer_add_15),
                        style = MonoMetricMedium
                    )
                }

                IconButton(onClick = onTogglePause) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(
                            id = if (state.isRunning) R.string.plate_rack_timer_pause else R.string.plate_rack_timer_resume
                        ),
                        tint = accentColor
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.plate_rack_banner_dismiss),
                        tint = contentColor
                    )
                }
            }
        }
    }
}
