package com.jdluu.flexinsight.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jdluu.flexinsight.MainActivity

private val WidgetSurface = ColorProvider(
    day = Color(0xFFF3F2EE),
    night = Color(0xFF121316)
)

private val WidgetCardSurface = ColorProvider(
    day = Color(0xFFFFFFFF),
    night = Color(0xFF1B1D22)
)

private val WidgetInkPrimary = ColorProvider(
    day = Color(0xFF13151A),
    night = Color(0xFFF3F2EE)
)

private val WidgetInkSecondary = ColorProvider(
    day = Color(0xFF4B505C),
    night = Color(0xFFA8AFBD)
)

private val WidgetAccentCobalt = ColorProvider(
    day = Color(0xFF1E4EC7),
    night = Color(0xFF5B8BFF)
)

private val WidgetRecovered = ColorProvider(
    day = Color(0xFF19734B),
    night = Color(0xFF34D399)
)

private val WidgetModerate = ColorProvider(
    day = Color(0xFFB45309),
    night = Color(0xFFF59E0B)
)

private val WidgetStrained = ColorProvider(
    day = Color(0xFFC62828),
    night = Color(0xFFF87171)
)

class FlexHomeWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> =
        PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val streak = prefs[WidgetKeys.STREAK] ?: 0
            val recovery = prefs[WidgetKeys.RECOVERY] ?: 0
            val lastUpdated = prefs[WidgetKeys.LAST_UPDATED] ?: 0L
            val hasSynced = lastUpdated > 0L || streak > 0 || recovery > 0
            val nextWorkout = prefs[WidgetKeys.NEXT_WORKOUT] ?: "No session planned"
            val (statusLabel, statusColor, recoveryAccent) = when {
                !hasSynced -> Triple("Sync needed", WidgetAccentCobalt, WidgetInkSecondary)
                recovery >= 70 -> Triple("Ready", WidgetRecovered, WidgetRecovered)
                recovery >= 35 -> Triple("Moderate", WidgetModerate, WidgetModerate)
                else -> Triple("Fatigued", WidgetStrained, WidgetStrained)
            }

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetSurface)
                        .padding(14.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "FlexInsight",
                            style = TextStyle(
                                color = WidgetInkPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = statusLabel,
                            style = TextStyle(
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(WidgetCardSurface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        WidgetStat(
                            label = "Streak",
                            value = if (hasSynced) "${streak}d" else "—",
                            valueColor = WidgetInkPrimary,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        WidgetStat(
                            label = "Recovery",
                            value = if (hasSynced) "$recovery%" else "—",
                            valueColor = recoveryAccent,
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Text(
                        text = "Next: $nextWorkout",
                        maxLines = 1,
                        style = TextStyle(
                            color = WidgetInkSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetStat(
    label: String,
    value: String,
    valueColor: androidx.glance.unit.ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = TextStyle(
                color = WidgetInkSecondary,
                fontSize = 11.sp
            )
        )
        Text(
            text = value,
            style = TextStyle(
                color = valueColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

object WidgetKeys {
    val STREAK = intPreferencesKey("widget_streak")
    val RECOVERY = intPreferencesKey("widget_recovery")
    val NEXT_WORKOUT = stringPreferencesKey("widget_next")
    val LAST_UPDATED = longPreferencesKey("widget_updated")
}

class FlexHomeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlexHomeWidget()
}
