package com.loadpredictor.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.loadpredictor.MainActivity
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.repository.*
import com.loadpredictor.data.stats.*
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.presentation.common.ForecastCopy
import com.loadpredictor.util.DataFormatter

/** Content-first, tall enough to wrap essential text at large font settings. */
class LoadPredictorWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = PromoRepositoryImpl(AppDatabase.getInstance(context).promoDao())
        val usage = UsageRepositoryImpl(UsageAccessHelper(context), NetworkStatsDataSource(context))
        // The helper handles every result, so a revoked permission cannot leave a success layout behind.
        val computed = WidgetSyncHelper.computeWidgetState(repo, usage)
        WidgetStatePreferences.saveState(context, id, computed)
        provideContent {
            val prefs = currentState<Preferences>()
            val state = WidgetStatePreferences.readState(prefs)
            WidgetContent(state, LocalSize.current.width >= 240.dp)
        }
    }
}

private val ink = ColorProvider(Color(0xFFF1F6F4))
private val secondary = ColorProvider(Color(0xFFB4C5BD))
private val mint = ColorProvider(Color(0xFF74F4CE))

@Composable
private fun WidgetContent(state: WidgetState, wide: Boolean) {
    Column(GlanceModifier.fillMaxSize().appWidgetBackground().background(ColorProvider(Color(0xFF10241E)))
        .cornerRadius(20.dp).padding(12.dp).clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically) {
        when (state) {
            is WidgetState.Success -> {
                Text(if (state.pace == BurnPace.EXPIRED) "Promo expired" else "${DataFormatter.formatBytes(state.remainingBytes)} left",
                    style = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(6.dp))
                Text(ForecastCopy.widgetSummary(state.pace, state.estimatedDepletionTimestamp, state.isNoExpiry,
                    System.currentTimeMillis(), wide), style = TextStyle(color = mint, fontSize = 14.sp))
                if (wide) {
                    Spacer(GlanceModifier.height(6.dp))
                    Text(ForecastCopy.updated(state.lastUpdatedMillis, System.currentTimeMillis()),
                        style = TextStyle(color = secondary, fontSize = 12.sp))
                }
            }
            WidgetState.NoActivePromo -> {
                Text("Add a promo", style = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold))

            }
            WidgetState.PermissionRequired -> {
                Text("Allow Usage Access", style = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold))

            }
            is WidgetState.Error -> {
                Text("Reading unavailable", style = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold))

            }
        }
        Spacer(GlanceModifier.height(8.dp))
        val action = if (state is WidgetState.NoActivePromo || state is WidgetState.PermissionRequired)
            actionStartActivity<MainActivity>() else actionRunCallback<RefreshWidgetCallback>()
        Box(GlanceModifier.fillMaxWidth().height(48.dp).clickable(action),
            contentAlignment = Alignment.CenterStart) {
            Text(if (state is WidgetState.NoActivePromo || state is WidgetState.PermissionRequired) "Open app" else "Refresh", style = TextStyle(color = mint, fontSize = 14.sp, fontWeight = FontWeight.Bold))
        }
    }
}
