package com.jdluu.flexinsight.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jdluu.flexinsight.MainActivity

class FlexHomeWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> =
        PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val streak = prefs[WidgetKeys.STREAK] ?: 0
            val recovery = prefs[WidgetKeys.RECOVERY] ?: 0
            val nextWorkout = prefs[WidgetKeys.NEXT_WORKOUT] ?: "No plan"

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF0F172A)))
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "FlexInsight",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(modifier = GlanceModifier.padding(top = 8.dp)) {
                        WidgetStat("Streak", "$streak d")
                        WidgetStat("Recovery", "$recovery%")
                    }
                    Text(
                        text = nextWorkout,
                        modifier = GlanceModifier.padding(top = 8.dp),
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF94A3B8)),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetStat(label: String, value: String) {
    Column(modifier = GlanceModifier.padding(end = 16.dp)) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(Color(0xFF64748B)),
                fontSize = 10.sp
            )
        )
        Text(
            text = value,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

object WidgetKeys {
    val STREAK = intPreferencesKey("widget_streak")
    val RECOVERY = intPreferencesKey("widget_recovery")
    val NEXT_WORKOUT = androidx.datastore.preferences.core.stringPreferencesKey("widget_next")
    val LAST_UPDATED = longPreferencesKey("widget_updated")
}

class FlexHomeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlexHomeWidget()
}
