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
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.loadpredictor.MainActivity
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.repository.PromoRepositoryImpl
import com.loadpredictor.data.repository.UsageRepositoryImpl
import com.loadpredictor.data.stats.NetworkStatsDataSource
import com.loadpredictor.data.stats.UsageAccessHelper
import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.presentation.theme.LoadPredictorGlanceColorScheme
import kotlinx.coroutines.flow.first

/**
 * Material 3 Jetpack Glance home screen widget for LoadPredictor.
 *
 * Implements elevated dark responsive layouts for 2x2 (compact) and 4x2 (wide) dimensions:
 * - SurfaceLayer1 (#131722) elevated card surface
 * - Mint SIM pill and dominant crisp white hero typography
 * - Recessed depletion advisory callout container (#0C0F16)
 * - Semantic pace badges
 */
class LoadPredictorWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_SQUARE = DpSize(130.dp, 100.dp)
        private val HORIZONTAL_RECTANGLE = DpSize(250.dp, 100.dp)
    }

    override var sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_SQUARE, HORIZONTAL_RECTANGLE)
    )

    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Guarantee immediate state population upon widget placement / inflation
        try {
            val usageHelper = UsageAccessHelper(context)
            val database = AppDatabase.getInstance(context)
            val promoRepo = PromoRepositoryImpl(database.promoDao())
            val activePromo = promoRepo.getActivePromo().first()

            if (!usageHelper.hasUsageAccessPermission()) {
                updateAppWidgetState(context, id) { prefs ->
                    prefs[WidgetStatePreferences.KEY_STATE_TYPE] = WidgetStatePreferences.TYPE_PERMISSION_REQUIRED
                }
            } else if (activePromo == null) {
                updateAppWidgetState(context, id) { prefs ->
                    prefs[WidgetStatePreferences.KEY_STATE_TYPE] = WidgetStatePreferences.TYPE_NO_ACTIVE_PROMO
                }
            } else {
                val networkStatsDataSource = NetworkStatsDataSource(context)
                val usageRepo = UsageRepositoryImpl(usageHelper, networkStatsDataSource)
                val burnRateEngine = BurnRateEngine()
                val now = System.currentTimeMillis()
                val rawUsageBytes = try {
                    usageRepo.queryMobileUsageBytes(activePromo.startTimestamp, now)
                } catch (e: Exception) {
                    0L
                }
                val forecast = burnRateEngine.calculateForecast(activePromo, rawUsageBytes, now)
                updateAppWidgetState(context, id) { prefs ->
                    prefs[WidgetStatePreferences.KEY_STATE_TYPE] = WidgetStatePreferences.TYPE_SUCCESS
                    prefs[WidgetStatePreferences.KEY_PROMO_NAME] = forecast.promo.name
                    prefs[WidgetStatePreferences.KEY_SIM_SLOT] = forecast.promo.simSlot.name
                    prefs[WidgetStatePreferences.KEY_REMAINING_BYTES] = forecast.dataRemainingBytes
                    prefs[WidgetStatePreferences.KEY_TOTAL_ALLOWANCE_BYTES] = forecast.promo.totalAllowanceBytes
                    prefs[WidgetStatePreferences.KEY_PACE] = forecast.pace.name
                    prefs[WidgetStatePreferences.KEY_SUMMARY] = forecast.plainLanguageSummary
                    prefs[WidgetStatePreferences.KEY_IS_NO_EXPIRY] = forecast.promo.isNoExpiry
                    prefs[WidgetStatePreferences.KEY_LAST_UPDATED] = now
                }
            }
        } catch (e: Exception) {
            updateAppWidgetState(context, id) { prefs ->
                prefs[WidgetStatePreferences.KEY_STATE_TYPE] = WidgetStatePreferences.TYPE_ERROR
                prefs[WidgetStatePreferences.KEY_ERROR_MESSAGE] = e.localizedMessage ?: "Forecast error"
            }
        }

        provideContent {
            GlanceTheme(colors = LoadPredictorGlanceColorScheme) {
                val preferences = currentState<Preferences>()
                val state = WidgetStatePreferences.readState(preferences)
                val size = LocalSize.current
                val isWide = size.width >= 230.dp

                WidgetRoot(state = state, isWide = isWide)
            }
        }
    }
}

@Composable
private fun WidgetRoot(state: WidgetState, isWide: Boolean) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is WidgetState.Success -> {
                if (isWide) SuccessWideLayout(state = state) else SuccessCompactLayout(state = state)
            }
            is WidgetState.NoActivePromo -> {
                if (isWide) NoActivePromoWideLayout() else NoActivePromoCompactLayout()
            }
            is WidgetState.PermissionRequired -> {
                if (isWide) PermissionRequiredWideLayout() else PermissionRequiredCompactLayout()
            }
            is WidgetState.Error -> {
                if (isWide) ErrorWideLayout(message = state.message) else ErrorCompactLayout()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Success Layouts (2x2 vs 4x2)
// ---------------------------------------------------------------------------

@Composable
private fun SuccessCompactLayout(state: WidgetState.Success) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // Top Header: Promo Name + SIM badge + Refresh Button
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = state.promoName,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            SurfaceBadge(
                text = if (state.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2",
                bgColor = GlanceTheme.colors.primary,
                textColor = GlanceTheme.colors.onPrimary
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.surface)
                    .cornerRadius(6.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clickable(actionRunCallback<RefreshWidgetCallback>())
            ) {
                Text(
                    text = "🔄",
                    style = TextStyle(fontSize = 8.sp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        val dataPair = com.loadpredictor.util.DataFormatter.formatDataPair(
            remainingBytes = state.remainingBytes,
            totalAllowanceBytes = state.totalAllowanceBytes
        )

        Text(
            text = "REMAINING",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        )

        // Hero Remaining Number (Dominant Hierarchy - Crisp White)
        Text(
            text = dataPair.remainingFormatted,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            ),
            maxLines = 1
        )

        Text(
            text = "of ${dataPair.totalFormatted}",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp
            ),
            maxLines = 1
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Pace Badge at bottom
        PaceBadge(pace = state.pace, isNoExpiry = state.isNoExpiry)
    }
}

@Composable
private fun SuccessWideLayout(state: WidgetState.Success) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // Header Row: Promo Name + SIM badge + Wide Pace Pill + Manual Refresh
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = state.promoName,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            SurfaceBadge(
                text = if (state.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2",
                bgColor = GlanceTheme.colors.primary,
                textColor = GlanceTheme.colors.onPrimary
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            PaceBadge(pace = state.pace, isNoExpiry = state.isNoExpiry)
            Spacer(modifier = GlanceModifier.width(6.dp))
            // Dedicated Manual Refresh Trigger
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.surface)
                    .cornerRadius(6.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clickable(actionRunCallback<RefreshWidgetCallback>())
            ) {
                Text(
                    text = "🔄",
                    style = TextStyle(fontSize = 10.sp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        val dataPair = com.loadpredictor.util.DataFormatter.formatDataPair(
            remainingBytes = state.remainingBytes,
            totalAllowanceBytes = state.totalAllowanceBytes
        )

        // Hero Row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.Bottom
        ) {
            Text(
                text = dataPair.remainingFormatted,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "of ${dataPair.totalFormatted} allowance",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Recessed Depletion Advisory Callout Box
        val projectionText = state.plainLanguageSummary.ifBlank {
            "Calibrating pace • ${dataPair.remainingFormatted} remaining"
        }

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "⚡ $projectionText",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Other Widget States (NoActivePromo, PermissionRequired, Error)
// ---------------------------------------------------------------------------

@Composable
private fun NoActivePromoCompactLayout() {
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = "No Promo",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = "Tap to set up",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun NoActivePromoWideLayout() {
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        Text(
            text = "No Active Promo Configured",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = "Tap to track your Philippine prepaid mobile data",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun PermissionRequiredCompactLayout() {
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = "⚠️ Usage Access",
            style = TextStyle(
                color = GlanceTheme.colors.error,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = "Tap to grant",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun PermissionRequiredWideLayout() {
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        Text(
            text = "⚠️ Usage Access Required",
            style = TextStyle(
                color = GlanceTheme.colors.error,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = "Tap to enable system permission for mobile data tracking",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun ErrorCompactLayout() {
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = "Sync Error",
            style = TextStyle(
                color = GlanceTheme.colors.error,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = "Tap to retry",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun ErrorWideLayout(message: String) {
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        Text(
            text = "Forecast Unavailable",
            style = TextStyle(
                color = GlanceTheme.colors.error,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = message,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp
            ),
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// Helper Badges & Formatters
// ---------------------------------------------------------------------------

@Composable
private fun SurfaceBadge(
    text: String,
    bgColor: androidx.glance.unit.ColorProvider,
    textColor: androidx.glance.unit.ColorProvider
) {
    Box(
        modifier = GlanceModifier
            .background(bgColor)
            .cornerRadius(6.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun PaceBadge(pace: BurnPace, isNoExpiry: Boolean = false) {
    val (label, bg, fg) = when (pace) {
        BurnPace.BURNING_FAST -> Triple("🔥 Fast", GlanceTheme.colors.errorContainer, GlanceTheme.colors.onErrorContainer)
        BurnPace.ON_TRACK -> Triple(if (isNoExpiry) "⚡ Steady" else "⚡ On Track", GlanceTheme.colors.primaryContainer, GlanceTheme.colors.onPrimaryContainer)
        BurnPace.CONSERVATIVE -> Triple("🛡️ Safe", GlanceTheme.colors.secondaryContainer, GlanceTheme.colors.onSecondaryContainer)
        BurnPace.DEPLETED -> Triple("⛔ Depleted", GlanceTheme.colors.error, GlanceTheme.colors.onError)
        BurnPace.INSUFFICIENT_DATA -> Triple("⏳ Calibrating", GlanceTheme.colors.surface, GlanceTheme.colors.onSurfaceVariant)
    }
    SurfaceBadge(text = label, bgColor = bg, textColor = fg)
}
