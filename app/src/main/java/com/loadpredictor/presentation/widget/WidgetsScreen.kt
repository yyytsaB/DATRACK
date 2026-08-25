package com.loadpredictor.presentation.widget

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.domain.model.BurnForecast
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.DarkBackground
import com.loadpredictor.presentation.theme.DarkLinearTrack
import com.loadpredictor.presentation.theme.DarkOutlineVariant
import com.loadpredictor.presentation.theme.DarkRingTrack
import com.loadpredictor.presentation.theme.LightCardOutline
import com.loadpredictor.presentation.theme.LightCardSurface
import com.loadpredictor.presentation.theme.LightLinearTrack
import com.loadpredictor.presentation.theme.LightRingTrack
import com.loadpredictor.presentation.theme.MintOnPrimary
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.PaceCalibrating
import com.loadpredictor.presentation.theme.PaceCalibratingContainer
import com.loadpredictor.presentation.theme.PaceCalibratingText
import com.loadpredictor.presentation.theme.PaceConservative
import com.loadpredictor.presentation.theme.PaceConservativeContainer
import com.loadpredictor.presentation.theme.PaceConservativeText
import com.loadpredictor.presentation.theme.PaceCritical
import com.loadpredictor.presentation.theme.PaceCriticalContainer
import com.loadpredictor.presentation.theme.PaceCriticalText
import com.loadpredictor.presentation.theme.PaceOnTrack
import com.loadpredictor.presentation.theme.PaceOnTrackContainer
import com.loadpredictor.presentation.theme.PaceOnTrackText
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextLightHigh
import com.loadpredictor.presentation.theme.TextLightLow
import com.loadpredictor.presentation.theme.TextLightMedium
import com.loadpredictor.presentation.theme.TextLowEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.launch

/**
 * Preview and pinning gallery for Glance Home Screen widgets.
 *
 * Provides pixel-accurate preview cards for 2x2 (Compact) and 4x2 (Wide) sizes
 * in both Dark and Light visual variants matching the reference UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(
    viewModel: WidgetsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Widgets",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextHighEmphasis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextHighEmphasis
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Intro Callout Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLayer1),
                border = BorderStroke(1.dp, BorderHighlight)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Glance Home Screen Widgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHighEmphasis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keep track of your Philippine prepaid load at a glance. Preview live widget layouts below and request direct placement to your home screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMediumEmphasis,
                        lineHeight = 18.sp
                    )
                }
            }

            // Effective forecast to display (live active forecast or sample demo fallback)
            val displayForecast = uiState.activeForecast ?: createSampleDemoForecast()

            // -----------------------------------------------------------------------
            // SECTION 1: 2x1 COMPACT WIDGET (Dark & Light Variants)
            // -----------------------------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2×1 COMPACT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumEmphasis,
                    letterSpacing = 1.2.sp
                )

                // Side-by-side Dark & Light 2x1 Compact Cards (True 2-column width)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dark",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLowEmphasis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompactWidgetPreviewCard(forecast = displayForecast, isLight = false)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Light",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLowEmphasis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompactWidgetPreviewCard(forecast = displayForecast, isLight = true)
                    }
                }

                // Pin Action Button
                Button(
                    onClick = {
                        val dispatched = viewModel.requestPinWidget(context)
                        coroutineScope.launch {
                            if (dispatched) {
                                snackbarHostState.showSnackbar("Check your home screen to confirm widget placement.")
                            } else {
                                snackbarHostState.showSnackbar("Widget pinning is not supported by your launcher. Add manually from your home screen.")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintPrimary,
                        contentColor = MintOnPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pin 2×1 Widget",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // -----------------------------------------------------------------------
            // SECTION 2: 4x1 WIDE WIDGET (Dark & Light Variants)
            // -----------------------------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "4×1 WIDE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumEmphasis,
                    letterSpacing = 1.2.sp
                )

                // Dark Wide Card
                Text(
                    text = "Dark",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLowEmphasis
                )
                WideWidgetPreviewCard(forecast = displayForecast, isLight = false)

                Spacer(modifier = Modifier.height(4.dp))

                // Light Wide Card
                Text(
                    text = "Light",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLowEmphasis
                )
                WideWidgetPreviewCard(forecast = displayForecast, isLight = true)

                // Pin Action Button
                Button(
                    onClick = {
                        val dispatched = viewModel.requestPinWidget(context)
                        coroutineScope.launch {
                            if (dispatched) {
                                snackbarHostState.showSnackbar("Check your home screen to confirm widget placement.")
                            } else {
                                snackbarHostState.showSnackbar("Widget pinning is not supported by your launcher. Add manually from your home screen.")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintPrimary,
                        contentColor = MintOnPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pin 4×1 Widget",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // -----------------------------------------------------------------------
            // Launcher Instructions & Fallback Footer
            // -----------------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLayer1.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, BorderHighlight.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextLowEmphasis,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (uiState.isPinSupported) {
                            "Tapping 'Pin Widget' requests your launcher to place the widget. You will be prompted by your launcher to confirm placement."
                        } else {
                            "Automatic pinning is not supported by your launcher. You can add the widget manually by touching and holding an empty spot on your home screen, tapping 'Widgets', and selecting Datrack."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMediumEmphasis,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Native Jetpack Compose Lookalike Preview Cards (2x1 and 4x1)
// ---------------------------------------------------------------------------

@Composable
private fun CompactWidgetPreviewCard(
    forecast: BurnForecast,
    isLight: Boolean
) {
    val dataPair = DataFormatter.formatDataPair(
        remainingBytes = forecast.dataRemainingBytes,
        totalAllowanceBytes = forecast.promo.totalAllowanceBytes
    )
    val paceColor = getPaceColor(forecast.pace)
    val paceLabel = getPaceLabel(forecast.pace, forecast.promo.isNoExpiry)
    val remainingFraction = if (forecast.promo.totalAllowanceBytes > 0L) {
        (forecast.dataRemainingBytes.toFloat() / forecast.promo.totalAllowanceBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val cardBg = if (isLight) LightCardSurface else SurfaceLayer1
    val cardBorder = if (isLight) LightCardOutline else BorderHighlight
    val heroTextColor = if (isLight) TextLightHigh else TextHighEmphasis
    val subTextColor = if (isLight) TextLightMedium else TextMediumEmphasis
    val trackColor = if (isLight) LightRingTrack else DarkRingTrack
    val simLabel = if (forecast.promo.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Progress Ring
            Canvas(modifier = Modifier.size(34.dp)) {
                val strokeWidth = 4.dp.toPx()
                val progressColor = com.loadpredictor.presentation.theme.getDataProgressColor(remainingFraction)
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = strokeWidth)
                )
                if (remainingFraction > 0f) {
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = remainingFraction * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Balance + SIM / Pace
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dataPair.remainingFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = heroTextColor,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$simLabel • ",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = subTextColor
                    )
                    Text(
                        text = paceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = paceColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun WideWidgetPreviewCard(
    forecast: BurnForecast,
    isLight: Boolean
) {
    val dataPair = DataFormatter.formatDataPair(
        remainingBytes = forecast.dataRemainingBytes,
        totalAllowanceBytes = forecast.promo.totalAllowanceBytes
    )
    val paceColor = getPaceColor(forecast.pace)
    val paceLabel = getPaceLabel(forecast.pace, forecast.promo.isNoExpiry)
    val remainingFraction = if (forecast.promo.totalAllowanceBytes > 0L) {
        (forecast.dataRemainingBytes.toFloat() / forecast.promo.totalAllowanceBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val cardBg = if (isLight) LightCardSurface else SurfaceLayer1
    val cardBorder = if (isLight) LightCardOutline else BorderHighlight
    val titleColor = if (isLight) TextLightHigh else TextHighEmphasis
    val secondaryColor = if (isLight) TextLightMedium else TextMediumEmphasis
    val linearTrackColor = if (isLight) LightLinearTrack else DarkLinearTrack
    val simLabel = if (forecast.promo.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2"
    val etaText = formatDepletionEta(forecast)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Brand/SIM + Remaining balance
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DATRACK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MintPrimary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = " • $simLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryColor,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dataPair.remainingFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = titleColor
                    )
                    Text(
                        text = " / ${dataPair.totalFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Column: Progress bar + ETA
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                val progressColor = com.loadpredictor.presentation.theme.getDataProgressColor(remainingFraction)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(linearTrackColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(remainingFraction)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(progressColor)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = etaText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = paceColor,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WidePacePill(
    pace: BurnPace,
    isLight: Boolean,
    isNoExpiry: Boolean
) {
    val paceColor = getPaceColor(pace)
    val paceLabel = getPaceLabel(pace, isNoExpiry)

    val (darkBg, lightBg, darkText, lightText) = when (pace) {
        BurnPace.BURNING_FAST, BurnPace.DEPLETED -> Quadruple(
            PaceCriticalContainer,
            Color(0xFFFEEBEB),
            PaceCriticalText,
            PaceCritical
        )
        BurnPace.ON_TRACK -> Quadruple(
            PaceOnTrackContainer,
            Color(0xFFE6F9F0),
            PaceOnTrackText,
            PaceOnTrack
        )
        BurnPace.CONSERVATIVE -> Quadruple(
            PaceConservativeContainer,
            Color(0xFFE8F6FD),
            PaceConservativeText,
            PaceConservative
        )
        BurnPace.INSUFFICIENT_DATA -> Quadruple(
            PaceCalibratingContainer,
            Color(0xFFFEF3C7),
            PaceCalibratingText,
            PaceCalibrating
        )
    }

    val pillBg = if (isLight) lightBg else darkBg
    val pillTextColor = if (isLight) lightText else darkText

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = pillBg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(paceColor, CircleShape)
            )
            Text(
                text = paceLabel,
                color = pillTextColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun getPaceLabel(pace: BurnPace, isNoExpiry: Boolean): String = when (pace) {
    BurnPace.BURNING_FAST -> "Cutting It Close"
    BurnPace.ON_TRACK -> if (isNoExpiry) "Steady" else "On Track"
    BurnPace.CONSERVATIVE -> if (isNoExpiry) "Light Usage" else "Safe & Steady"
    BurnPace.DEPLETED -> "Depleted"
    BurnPace.INSUFFICIENT_DATA -> "Calibrating"
}

private fun getPaceColor(pace: BurnPace): Color = when (pace) {
    BurnPace.BURNING_FAST -> PaceCritical
    BurnPace.ON_TRACK -> PaceOnTrack
    BurnPace.CONSERVATIVE -> PaceConservative
    BurnPace.DEPLETED -> PaceCritical
    BurnPace.INSUFFICIENT_DATA -> PaceCalibrating
}

private fun formatDepletionEta(forecast: BurnForecast): String {
    if (forecast.isDepleted) return "Depleted"
    val depletionTime = forecast.estimatedDepletionTimestamp
    if (depletionTime != null && depletionTime > System.currentTimeMillis()) {
        return "Runs out ${DataFormatter.formatDepletionDateTime(depletionTime)}"
    }
    return forecast.plainLanguageSummary.ifBlank { "Calibrating pace" }
}

/**
 * Creates a clean sample demo forecast when no active promo is currently configured.
 */
private fun createSampleDemoForecast(): BurnForecast {
    val sampleTotal = 8L * 1024L * 1024L * 1024L // 8 GB
    val sampleRemaining = (2.8 * 1024.0 * 1024.0 * 1024.0).toLong() // 2.8 GB
    val sampleUsed = sampleTotal - sampleRemaining
    val promo = Promo(
        id = 999L,
        name = "Smart GigaSurf 99",
        totalAllowanceBytes = sampleTotal,
        startTimestamp = System.currentTimeMillis() - (86400000L * 2),
        expirationTimestamp = System.currentTimeMillis() + (86400000L * 2),
        simSlot = SimSlot.SIM_1
    )
    return BurnForecast(
        promo = promo,
        dataUsedBytes = sampleUsed,
        dataRemainingBytes = sampleRemaining,
        burnRateBytesPerHour = (sampleUsed / 48.0),
        estimatedDepletionTimestamp = System.currentTimeMillis() + (86400000L * 1),
        burnStatusIndex = 1.35,
        pace = BurnPace.BURNING_FAST,
        plainLanguageSummary = "Runs out Thu 3:15 PM",
        isDepleted = false,
        timeRemainingMillis = 86400000L * 2
    )
}
