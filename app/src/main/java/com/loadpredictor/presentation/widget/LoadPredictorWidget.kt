package com.loadpredictor.presentation.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.layout.fillMaxHeight
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
import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.presentation.theme.LoadPredictorGlanceColorScheme
import com.loadpredictor.presentation.theme.getDataProgressColor
import com.loadpredictor.presentation.theme.getDataProgressIntColor
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.flow.first

/**
 * Material 3 Jetpack Glance home screen widget for Datrack.
 *
 * Implements single-row 2x1 (compact) and 4x1 (wide) horizontal layouts:
 * - 2x1: Circular progress ring + remaining balance + SIM/Pace label
 * - 4x1: Left column (Brand/SIM + Remaining balance) + Right column (Linear bar + ETA/Pace)
 */
class LoadPredictorWidget : GlanceAppWidget() {

    companion object {
        private val SIZE_2X1 = DpSize(110.dp, 40.dp)
        private val SIZE_4X1 = DpSize(220.dp, 40.dp)
    }

    override var sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SIZE_2X1, SIZE_4X1)
    )

    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
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
                val getDailyUsageBreakdownUseCase = com.loadpredictor.domain.usecase.GetDailyUsageBreakdownUseCase(usageRepo)
                val getActiveBurnForecastUseCase = com.loadpredictor.domain.usecase.GetActiveBurnForecastUseCase(
                    promoRepository = promoRepo,
                    usageRepository = usageRepo,
                    getDailyUsageBreakdownUseCase = getDailyUsageBreakdownUseCase
                )
                val result = getActiveBurnForecastUseCase.execute(activePromo)
                if (result is com.loadpredictor.domain.model.BurnForecastResult.Success) {
                    val forecast = result.forecast
                    val now = System.currentTimeMillis()
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
                        prefs[WidgetStatePreferences.KEY_ESTIMATED_DEPLETION_TIMESTAMP] = forecast.estimatedDepletionTimestamp ?: -1L
                    }
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
                val isWide = size.width >= 210.dp

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
            .background(ColorProvider(Color(0xFF131722)))
            .cornerRadius(20.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is WidgetState.Success -> {
                if (isWide) Success4x1Layout(state = state) else Success2x1Layout(state = state)
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
// 2x1 and 4x1 Success Layouts
// ---------------------------------------------------------------------------

@Composable
private fun Success2x1Layout(state: WidgetState.Success) {
    val remainingFraction = if (state.totalAllowanceBytes > 0L) {
        (state.remainingBytes.toFloat() / state.totalAllowanceBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val ringBitmap = renderGlanceRingBitmap(progress = remainingFraction, remainingRatio = remainingFraction)
    val dataPair = DataFormatter.formatDataPair(
        remainingBytes = state.remainingBytes,
        totalAllowanceBytes = state.totalAllowanceBytes
    )
    val paceColorProvider = getGlancePaceColor(state.pace)
    val paceLabel = getGlancePaceLabel(state.pace, state.isNoExpiry)
    val simLabel = if (state.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2"

    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // Left: Circular Ring Bitmap
        Image(
            provider = ImageProvider(ringBitmap),
            contentDescription = null,
            modifier = GlanceModifier.size(40.dp)
        )

        Spacer(modifier = GlanceModifier.width(10.dp))

        // Right: Amount + SIM/Pace label
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = dataPair.remainingFormatted,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFFFFF)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(1.dp))
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = "$simLabel • ",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8E99A8)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = paceLabel,
                    style = TextStyle(
                        color = paceColorProvider,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun Success4x1Layout(state: WidgetState.Success) {
    val remainingFraction = if (state.totalAllowanceBytes > 0L) {
        (state.remainingBytes.toFloat() / state.totalAllowanceBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val dataPair = DataFormatter.formatDataPair(
        remainingBytes = state.remainingBytes,
        totalAllowanceBytes = state.totalAllowanceBytes
    )
    val paceColorProvider = getGlancePaceColor(state.pace)
    val paceLabel = getGlancePaceLabel(state.pace, state.isNoExpiry)
    val simLabel = if (state.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2"
    val progressColor = getDataProgressColor(remainingFraction)

    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // Left Column: Brand/SIM + Remaining balance
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = "DATRACK",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF00F5D4)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = " • $simLabel",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8E99A8)),
                        fontSize = 10.sp
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(1.dp))
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = dataPair.remainingFormatted,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFFFFFFF)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = " / ${dataPair.totalFormatted}",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8E99A8)),
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Right Column: Linear Progress Bar + ETA / Pace Status
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = remainingFraction,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = ColorProvider(progressColor),
                backgroundColor = ColorProvider(Color(0xFF252D3D))
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                val etaSummary = formatWidgetDepletionEta(state, paceLabel)
                Text(
                    text = etaSummary,
                    style = TextStyle(
                        color = paceColorProvider,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

private fun formatWidgetDepletionEta(state: WidgetState.Success, paceLabel: String): String {
    if (state.pace == BurnPace.DEPLETED) return "Depleted"
    val depletionTime = state.estimatedDepletionTimestamp
    if (depletionTime != null && depletionTime > System.currentTimeMillis()) {
        return "Runs out ${DataFormatter.formatDepletionDateTime(depletionTime)}"
    }
    return if (state.pace == BurnPace.INSUFFICIENT_DATA) {
        "Calibrating pace"
    } else {
        paceLabel
    }
}

// ---------------------------------------------------------------------------
// Other Widget States (NoActivePromo, PermissionRequired, Error)
// ---------------------------------------------------------------------------

@Composable
private fun NoActivePromoCompactLayout() {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "No Active Promo",
            style = TextStyle(
                color = ColorProvider(Color(0xFFFFFFFF)),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            text = "Set up →",
            style = TextStyle(
                color = ColorProvider(Color(0xFF00F5D4)),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun NoActivePromoWideLayout() {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "No Active Promo Tracked",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFFFFF)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            Text(
                text = "Tap to configure a Smart promo",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF8E99A8)),
                    fontSize = 11.sp
                )
            )
        }
        Text(
            text = "Set up →",
            style = TextStyle(
                color = ColorProvider(Color(0xFF00F5D4)),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun PermissionRequiredCompactLayout() {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "Permission Needed",
            style = TextStyle(
                color = ColorProvider(Color(0xFFFACC15)),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            text = "Grant →",
            style = TextStyle(
                color = ColorProvider(Color(0xFF00F5D4)),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun PermissionRequiredWideLayout() {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "Usage Access Required",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFACC15)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )
            Text(
                text = "Tap to grant Usage Access in Settings",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF8E99A8)),
                    fontSize = 11.sp
                )
            )
        }
        Text(
            text = "Grant →",
            style = TextStyle(
                color = ColorProvider(Color(0xFF00F5D4)),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun ErrorCompactLayout() {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "Forecast Unavailable",
            style = TextStyle(
                color = ColorProvider(Color(0xFFFF4D4D)),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun ErrorWideLayout(message: String) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "Forecast Error",
            style = TextStyle(
                color = ColorProvider(Color(0xFFFF4D4D)),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        )
        Text(
            text = message,
            style = TextStyle(
                color = ColorProvider(Color(0xFF8E99A8)),
                fontSize = 11.sp
            ),
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers: Bitmap Ring & Pace Colors
// ---------------------------------------------------------------------------

private fun renderGlanceRingBitmap(progress: Float, remainingRatio: Float): Bitmap {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val strokeWidth = 10f
    val pad = strokeWidth / 2f + 4f
    val rect = RectF(pad, pad, size - pad, size - pad)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        color = 0xFF232B3D.toInt()
    }
    canvas.drawCircle(size / 2f, size / 2f, (size - strokeWidth) / 2f - 4f, trackPaint)

    val progressColor = getDataProgressIntColor(remainingRatio)
    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.ROUND
        color = progressColor
    }
    if (progress > 0f) {
        canvas.drawArc(rect, -90f, progress * 360f, false, progressPaint)
    }
    return bitmap
}

private fun getGlancePaceColor(pace: BurnPace): ColorProvider {
    return when (pace) {
        BurnPace.ON_TRACK, BurnPace.CONSERVATIVE -> ColorProvider(Color(0xFF00F5D4))
        BurnPace.BURNING_FAST -> ColorProvider(Color(0xFFFACC15))
        BurnPace.DEPLETED -> ColorProvider(Color(0xFFFF4D4D))
        BurnPace.INSUFFICIENT_DATA -> ColorProvider(Color(0xFF94A3B8))
    }
}

private fun getGlancePaceLabel(pace: BurnPace, isNoExpiry: Boolean): String {
    return if (isNoExpiry) {
        when (pace) {
            BurnPace.ON_TRACK -> "Pace Normal"
            BurnPace.CONSERVATIVE -> "Low Burn"
            BurnPace.BURNING_FAST -> "Fast Burn"
            BurnPace.DEPLETED -> "Data Depleted"
            BurnPace.INSUFFICIENT_DATA -> "Calibrating"
        }
    } else {
        when (pace) {
            BurnPace.ON_TRACK -> "On Track"
            BurnPace.CONSERVATIVE -> "Under Pace"
            BurnPace.BURNING_FAST -> "Burning Fast"
            BurnPace.DEPLETED -> "Depleted"
            BurnPace.INSUFFICIENT_DATA -> "Calibrating"
        }
    }
}
