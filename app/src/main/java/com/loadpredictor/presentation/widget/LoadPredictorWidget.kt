package com.loadpredictor.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.loadpredictor.MainActivity
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.repository.PromoRepositoryImpl
import com.loadpredictor.data.repository.UsageRepositoryImpl
import com.loadpredictor.data.stats.NetworkStatsDataSource
import com.loadpredictor.data.stats.UsageAccessHelper
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.presentation.theme.LoadPredictorGlanceColorScheme
import com.loadpredictor.util.DataFormatter

/** Readable compact/wide layouts with explicit reading age and independent refresh action. */
class LoadPredictorWidget : GlanceAppWidget() {

    companion object {
        private val COMPACT_SIZE = DpSize(150.dp, 180.dp)
        private val WIDE_SIZE = DpSize(280.dp, 180.dp)
    }

    override var sizeMode: SizeMode = SizeMode.Responsive(
        setOf(COMPACT_SIZE, WIDE_SIZE)
    )

    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val promo = PromoRepositoryImpl(AppDatabase.getInstance(context).promoDao())
        val usage = UsageRepositoryImpl(UsageAccessHelper(context), NetworkStatsDataSource(context))
        WidgetStatePreferences.saveState(context, id, WidgetSyncHelper.computeWidgetState(promo, usage))

        provideContent {
            GlanceTheme(colors = LoadPredictorGlanceColorScheme) {
                val preferences = currentState<Preferences>()
                val state = WidgetStatePreferences.readState(preferences)
                val size = LocalSize.current
                val isWide = size.width >= 260.dp

                WidgetRoot(state = state, isWide = isWide)
            }
        }
    }
}

@Composable
private fun WidgetRoot(state: WidgetState, isWide: Boolean) {
    Column(GlanceModifier.fillMaxSize().appWidgetBackground()
        .background(ColorProvider(com.loadpredictor.presentation.theme.SurfaceLayer1)).cornerRadius(16.dp)
        .padding(12.dp).clickable(actionStartActivity<MainActivity>())) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(if (isWide && state is WidgetState.Success) state.promoName else "DATRACK",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = ColorProvider(com.loadpredictor.presentation.theme.TextMediumEmphasis), fontSize = 12.sp), maxLines = 2)
            Image(provider = ImageProvider(android.R.drawable.ic_popup_sync), contentDescription = "Refresh usage",
                modifier = GlanceModifier.size(48.dp).clickable(actionRunCallback<RefreshWidgetCallback>()).padding(12.dp))
        }
        when (state) {
            is WidgetState.Success -> {
                val expired = state.pace == BurnPace.EXPIRED
                Text(if (expired) "Expired" else DataFormatter.formatBytes(state.remainingBytes),
                    style = TextStyle(color = ColorProvider(com.loadpredictor.presentation.theme.MintPrimary), fontSize = 24.sp, fontWeight = FontWeight.Bold), maxLines = 2)
                Text(if (expired) "Validity ended" else "remaining",
                    style = TextStyle(color = ColorProvider(com.loadpredictor.presentation.theme.TextMediumEmphasis), fontSize = 12.sp))
                Spacer(GlanceModifier.height(8.dp))
                LinearProgressIndicator(progress = if (expired) 0f else (state.remainingBytes.toFloat() / state.totalAllowanceBytes.coerceAtLeast(1)).coerceIn(0f,1f),
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp), color = ColorProvider(com.loadpredictor.presentation.theme.MintPrimary),
                    backgroundColor = ColorProvider(com.loadpredictor.presentation.theme.DarkOutline))
                Spacer(GlanceModifier.height(8.dp))
                Text(com.loadpredictor.presentation.common.ForecastCopy.widgetSummary(state.pace, state.estimatedDepletionTimestamp, state.isNoExpiry, System.currentTimeMillis(), isWide),
                    style = TextStyle(color = ColorProvider(com.loadpredictor.presentation.theme.TextHighEmphasis), fontSize = 12.sp), maxLines = 2)
                if (isWide) Text(com.loadpredictor.presentation.common.ForecastCopy.updated(state.lastUpdatedMillis, System.currentTimeMillis()),
                    style = TextStyle(color = ColorProvider(com.loadpredictor.presentation.theme.TextMediumEmphasis), fontSize = 11.sp), maxLines = 2)
            }
            else -> {
                val title = when (state) {
                    is WidgetState.NoActivePromo -> "Choose a promo"
                    is WidgetState.PermissionRequired -> "Allow usage access"
                    else -> "Reading unavailable"
                }
                val detail = when (state) {
                    is WidgetState.NoActivePromo -> "Open DATRACK to start tracking."
                    is WidgetState.PermissionRequired -> "Open DATRACK to connect mobile data usage."
                    is WidgetState.Error -> if (state.lastSuccessfulAt > 0) com.loadpredictor.presentation.common.ForecastCopy.updated(state.lastSuccessfulAt, System.currentTimeMillis()) + ". Tap refresh to retry." else "Tap refresh to try again."
                    else -> ""
                }
                Text(title, style = TextStyle(color = ColorProvider(com.loadpredictor.presentation.theme.TextHighEmphasis), fontSize = 18.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(8.dp))
                Text(detail, style = TextStyle(color = ColorProvider(com.loadpredictor.presentation.theme.TextMediumEmphasis), fontSize = 12.sp))
            }
        }
    }
}
