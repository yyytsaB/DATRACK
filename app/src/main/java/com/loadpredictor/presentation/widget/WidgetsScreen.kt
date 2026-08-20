package com.loadpredictor.presentation.widget

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.loadpredictor.presentation.theme.DarkOutlineVariant
import com.loadpredictor.presentation.theme.MintOnPrimary
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.PaceCalibratingContainer
import com.loadpredictor.presentation.theme.PaceCalibratingText
import com.loadpredictor.presentation.theme.PaceConservativeContainer
import com.loadpredictor.presentation.theme.PaceConservativeText
import com.loadpredictor.presentation.theme.PaceCritical
import com.loadpredictor.presentation.theme.PaceCriticalContainer
import com.loadpredictor.presentation.theme.PaceCriticalText
import com.loadpredictor.presentation.theme.PaceOnTrackContainer
import com.loadpredictor.presentation.theme.PaceOnTrackText
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.SurfaceRecessed
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextLowEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.launch

/**
 * Preview and pinning gallery for Glance Home Screen widgets.
 *
 * =========================================================================================
 * ARCHITECTURAL CONTRACT & ACCEPTED MAINTENANCE RISK:
 * =========================================================================================
 * Glance widget layouts (in [LoadPredictorWidget.kt]) compile down to Android RemoteViews
 * for hosting in the OS Home Screen process and cannot be directly hosted within a normal
 * Jetpack Compose UI tree.
 *
 * The preview cards in this screen are separate, hand-built native Jetpack Compose lookalikes
 * built with identical design tokens (SurfaceLayer1, SurfaceRecessed, Pace semantic badges,
 * and formatters). Any visual changes made to [LoadPredictorWidget.kt] must be mirrored here
 * to ensure previews stay accurate.
 * =========================================================================================
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E2638),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = MintPrimary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Text(
                            text = "Widgets Gallery",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextHighEmphasis
                        )
                    }
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
            // SECTION 1: 2x2 COMPACT WIDGET
            // -----------------------------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2×2 COMPACT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumEmphasis,
                    letterSpacing = 1.2.sp
                )

                // 2x2 Preview Card
                CompactWidgetPreviewCard(forecast = displayForecast)

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
                        text = "Pin 2×2 Widget",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // -----------------------------------------------------------------------
            // SECTION 2: 4x2 WIDE WIDGET
            // -----------------------------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "4×2 WIDE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumEmphasis,
                    letterSpacing = 1.2.sp
                )

                // 4x2 Wide Preview Card
                WideWidgetPreviewCard(forecast = displayForecast)

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
                        text = "Pin 4×2 Widget",
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
                            "Automatic pinning is not supported by your launcher. You can add the widget manually by touching and holding an empty spot on your home screen, tapping 'Widgets', and selecting Load Predictor."
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
// Native Jetpack Compose Lookalike Preview Cards
// ---------------------------------------------------------------------------

@Composable
private fun CompactWidgetPreviewCard(forecast: BurnForecast) {
    val dataPair = DataFormatter.formatDataPair(
        remainingBytes = forecast.dataRemainingBytes,
        totalAllowanceBytes = forecast.promo.totalAllowanceBytes
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLayer1),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Promo Name + SIM badge + Refresh Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = forecast.promo.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MintPrimary
                ) {
                    Text(
                        text = if (forecast.promo.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2",
                        color = MintOnPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E2638),
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MintPrimary,
                        modifier = Modifier.padding(3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "REMAINING",
                style = MaterialTheme.typography.labelSmall,
                color = MintPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Hero Remaining Number
            Text(
                text = dataPair.remainingFormatted,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis
            )

            Text(
                text = "of ${dataPair.totalFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMediumEmphasis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Semantic Pace Badge Pill
            WidgetPaceBadge(pace = forecast.pace, isNoExpiry = forecast.promo.isNoExpiry)
        }
    }
}

@Composable
private fun WideWidgetPreviewCard(forecast: BurnForecast) {
    val dataPair = DataFormatter.formatDataPair(
        remainingBytes = forecast.dataRemainingBytes,
        totalAllowanceBytes = forecast.promo.totalAllowanceBytes
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLayer1),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: App/Promo Title + SIM badge + Pace Pill + Refresh Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = forecast.promo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MintPrimary
                ) {
                    Text(
                        text = if (forecast.promo.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2",
                        color = MintOnPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                WidgetPaceBadge(pace = forecast.pace, isNoExpiry = forecast.promo.isNoExpiry)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E2638),
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MintPrimary,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hero Balance Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = dataPair.remainingFormatted,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "of ${dataPair.totalFormatted} allowance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMediumEmphasis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recessed Depletion Advisory Callout Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceRecessed,
                border = BorderStroke(1.dp, DarkOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ ${forecast.plainLanguageSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHighEmphasis,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPaceBadge(pace: BurnPace, isNoExpiry: Boolean) {
    val (bgColor, textColor, label) = when (pace) {
        BurnPace.BURNING_FAST -> Triple(
            PaceCriticalContainer,
            PaceCriticalText,
            "🔥 Fast"
        )
        BurnPace.ON_TRACK -> Triple(
            PaceOnTrackContainer,
            PaceOnTrackText,
            if (isNoExpiry) "⚡ Steady" else "⚡ On Track"
        )
        BurnPace.CONSERVATIVE -> Triple(
            PaceConservativeContainer,
            PaceConservativeText,
            if (isNoExpiry) "🛡️ Light" else "🛡️ Safe"
        )
        BurnPace.DEPLETED -> Triple(
            PaceCriticalContainer,
            PaceCriticalText,
            "⛔ Depleted"
        )
        BurnPace.INSUFFICIENT_DATA -> Triple(
            PaceCalibratingContainer,
            PaceCalibratingText,
            "⏳ Calibrating"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Creates a clean sample demo forecast when no active promo is currently configured.
 */
private fun createSampleDemoForecast(): BurnForecast {
    val sampleTotal = 24L * 1024L * 1024L * 1024L // 24 GB
    val sampleRemaining = (18.5 * 1024.0 * 1024.0 * 1024.0).toLong() // 18.5 GB
    val sampleUsed = sampleTotal - sampleRemaining
    val promo = Promo(
        id = 999L,
        name = "Smart Magic Data 399",
        totalAllowanceBytes = sampleTotal,
        startTimestamp = System.currentTimeMillis() - (86400000L * 3),
        expirationTimestamp = null,
        simSlot = SimSlot.SIM_1
    )
    return BurnForecast(
        promo = promo,
        dataUsedBytes = sampleUsed,
        dataRemainingBytes = sampleRemaining,
        burnRateBytesPerHour = (sampleUsed / 72.0),
        estimatedDepletionTimestamp = System.currentTimeMillis() + (86400000L * 14),
        burnStatusIndex = null,
        pace = BurnPace.ON_TRACK,
        plainLanguageSummary = "Demo Preview • 18.5 GB remaining",
        isDepleted = false,
        timeRemainingMillis = null
    )
}
