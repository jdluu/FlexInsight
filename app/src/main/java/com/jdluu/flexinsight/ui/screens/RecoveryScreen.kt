package com.jdluu.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.data.model.MuscleGroup
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.ErrorBanner
import com.jdluu.flexinsight.ui.components.MuscleHeatmap
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.components.RecoverySkeleton
import com.jdluu.flexinsight.ui.components.recoveryColorFor
import com.jdluu.flexinsight.ui.components.recoveryContainerFor
import com.jdluu.flexinsight.ui.screens.dashboard.parts.TrainingLoadCard
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.theme.PlateNumeralLarge
import com.jdluu.flexinsight.ui.theme.PlateNumeralSmall
import com.jdluu.flexinsight.ui.viewmodel.RecoveryViewModel
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecoveryScreen(
    viewModel: RecoveryViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.recovery_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.action_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                TextButton(onClick = onNavigateToPlanner) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.nav_planner))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)

        if (uiState.isLoading && uiState.muscleRecovery.isEmpty()) {
            RecoverySkeleton()
            return@Column
        }

        val error = uiState.error
        if (error != null && uiState.muscleRecovery.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ErrorBanner(error = error)
                    Button(onClick = { viewModel.refresh() }) {
                        Text(text = stringResource(id = R.string.action_retry))
                    }
                }
            }
            return@Column
        }

        val fraction = (uiState.overallRecoveryScore / 100f).coerceIn(0f, 1f)
        val statusColor = recoveryColorFor(fraction)
        val (pillBg, pillFg) = recoveryContainerFor(fraction)
        val statusLabel = when {
            fraction < 0.35f -> "Fatigued"
            fraction < 0.70f -> "Moderate"
            else -> "Ready"
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.deloadAlert?.shouldDeload == true) {
                item {
                    PlateCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        containerColor = FlexTheme.colors.moderateContainer,
                        borderColor = FlexTheme.colors.moderateFatigue.copy(alpha = 0.45f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = FlexTheme.colors.moderateFatigue
                            )
                            Text(
                                text = uiState.deloadAlert?.message.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = FlexTheme.colors.onModerateContainer
                            )
                        }
                    }
                }
            }

            // 1. Composite Readiness Hero
            item {
                PlateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(id = R.string.recovery_composite_readiness),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "72-hour rolling tissue recovery window",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${uiState.overallRecoveryScore}%",
                                style = PlateNumeralLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            PlateStatusPill(
                                label = statusLabel,
                                containerColor = pillBg,
                                contentColor = pillFg
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CalibratedPlateBar(
                        progress = fraction,
                        segments = 10,
                        activeColor = statusColor,
                        barHeight = 10.dp
                    )
                }
            }

            // 2. Anatomical Muscle Heatmap + Interactive Muscle Group Breakdown
            item {
                PlateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.recovery_muscle_status_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
                    Spacer(modifier = Modifier.height(12.dp))

                    MuscleHeatmap(
                        recoveryMap = uiState.muscleRecovery,
                        figureHeight = 220.dp,
                        selectedGroup = uiState.selectedMuscle,
                        showLegend = true,
                        onGroupSelected = { viewModel.selectMuscle(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
                    Spacer(modifier = Modifier.height(10.dp))

                    val displayGroups = MuscleGroup.entries.filter { it != MuscleGroup.CARDIO }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayGroups.forEach { group ->
                            val value = uiState.muscleRecovery[group] ?: 1.0f
                            val pct = (value * 100f).roundToInt().coerceIn(0, 100)
                            val isSelected = uiState.selectedMuscle == group
                            val groupColor = recoveryColorFor(value)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectMuscle(group) },
                                shape = MaterialTheme.shapes.small,
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(groupColor, MaterialTheme.shapes.extraSmall)
                                    )
                                },
                                label = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = group.displayName,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "$pct%",
                                            style = MonoMetricSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = FlexTheme.colors.hairline,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.5.dp
                                )
                            )
                        }
                    }
                }
            }

            // 3. Health Connect Biometrics (when enabled / available)
            item {
                val health = uiState.healthSnapshot
                PlateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = if (!uiState.healthConnectEnabled) onNavigateToSettings else null,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.recovery_biometrics_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.healthConnectEnabled && health != null && health.hasData) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BiometricTile(
                                icon = Icons.Outlined.Bedtime,
                                label = stringResource(id = R.string.recovery_sleep_label),
                                value = health.sleepHoursLastNight?.let {
                                    String.format(Locale.US, "%.1fh", it)
                                } ?: "—",
                                modifier = Modifier.weight(1f)
                            )
                            BiometricTile(
                                icon = Icons.Outlined.FavoriteBorder,
                                label = stringResource(id = R.string.recovery_rhr_label),
                                value = health.restingHeartRateBpm?.let { "$it bpm" } ?: "—",
                                modifier = Modifier.weight(1f)
                            )
                            BiometricTile(
                                icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                                label = stringResource(id = R.string.recovery_steps_label),
                                value = health.stepsToday?.toString() ?: "—",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(id = R.string.recovery_biometrics_off),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. Subjective Check-In (persisted mood & soreness notes)
            item {
                PlateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.recovery_mood_log_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(id = R.string.recovery_mood_log_question),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f / 10", uiState.moodRating),
                            style = PlateNumeralSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = uiState.moodRating,
                        onValueChange = { viewModel.updateMood(it) },
                        valueRange = 0f..10f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = FlexTheme.colors.chalkTrack
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.recovery_mood_drained),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(id = R.string.recovery_mood_unstoppable),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.sorenessNotes,
                        onValueChange = { viewModel.updateNotes(it) },
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.recovery_mood_notes_placeholder),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }

            // 5. Training Load Breakdown (when available)
            if (uiState.trainingLoad != null) {
                item {
                    TrainingLoadCard(score = uiState.trainingLoad)
                }
            }

            // 6. On-Device AI Coach Note
            if (uiState.aiInsight != null || uiState.isGeneratingInsight) {
                item {
                    PlateCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.recovery_ai_insight),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.isGeneratingInsight) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = stringResource(id = R.string.recovery_smart_analysis_running),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else {
                            Text(
                                text = uiState.aiInsight.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BiometricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MonoMetricMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
