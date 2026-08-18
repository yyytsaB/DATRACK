package com.loadpredictor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.loadpredictor.data.stats.UsageAccessHelper
import com.loadpredictor.domain.model.BurnForecast
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.presentation.MainUiState
import com.loadpredictor.presentation.MainViewModel
import com.loadpredictor.presentation.common.UsagePermissionRequiredCard
import com.loadpredictor.presentation.dashboard.BurnAlertsOptInCard
import com.loadpredictor.presentation.dashboard.DailyUsageChartCard
import com.loadpredictor.presentation.promo.PromoManagementScreen
import com.loadpredictor.presentation.promo.PromoViewModel
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.DarkBackground
import com.loadpredictor.presentation.theme.DarkOutline
import com.loadpredictor.presentation.theme.DarkOutlineVariant
import com.loadpredictor.presentation.theme.DarkSurfaceVariant
import com.loadpredictor.presentation.theme.LoadPredictorTheme
import com.loadpredictor.presentation.theme.MintOnPrimary
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.MintPrimaryContainer
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
import com.loadpredictor.presentation.theme.SurfaceLayer2
import com.loadpredictor.presentation.theme.SurfaceRecessed
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextLowEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis
import com.loadpredictor.worker.WorkManagerScheduler

enum class AppScreen {
    DASHBOARD,
    PROMO_MANAGEMENT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule periodic background usage sync & threshold alerts
        WorkManagerScheduler.schedulePeriodicSync(this)

        val usageAccessHelper = UsageAccessHelper(this)

        setContent {
            LoadPredictorTheme {
                val context = LocalContext.current
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModel.provideFactory(context)
                )
                val promoViewModel: PromoViewModel = viewModel(
                    factory = PromoViewModel.provideFactory(context)
                )
                val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

                // Lifecycle-aware foreground ticker: refreshes immediately on entry (and on resume from Settings)
                // and every 30 seconds while in the foreground, automatically cancelling when backgrounded.
                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (isActive) {
                            mainViewModel.refresh()
                            delay(30_000L)
                        }
                    }
                }

                when (currentScreen) {
                    AppScreen.PROMO_MANAGEMENT -> {
                        PromoManagementScreen(
                            viewModel = promoViewModel,
                            onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
                        )
                    }
                    AppScreen.DASHBOARD -> {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = DarkBackground
                        ) { innerPadding ->
                            MainScreenContent(
                                uiState = uiState,
                                onGrantPermissionClick = {
                                    startActivity(usageAccessHelper.createUsageAccessSettingsIntent())
                                },
                                onNavigateToPromoManagement = {
                                    currentScreen = AppScreen.PROMO_MANAGEMENT
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onGrantPermissionClick: () -> Unit,
    onNavigateToPromoManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MintPrimary)
            }
        }
        !uiState.isUsagePermissionGranted -> {
            UsagePermissionRequiredCard(
                onGrantPermissionClick = onGrantPermissionClick,
                modifier = modifier
            )
        }
        else -> {
            DashboardView(
                uiState = uiState,
                onNavigateToPromoManagement = onNavigateToPromoManagement,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardView(
    uiState: MainUiState,
    onNavigateToPromoManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = SurfaceLayer1,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = MintPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Load Predictor",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).sp,
                        color = TextHighEmphasis
                    )
                }
            },
            actions = {
                IconButton(onClick = onNavigateToPromoManagement) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = SurfaceLayer1,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Manage Promos",
                            tint = TextHighEmphasis,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground,
                titleContentColor = TextHighEmphasis
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notification Permission Opt-In Card
            BurnAlertsOptInCard()

            when (val forecastResult = uiState.forecastResult) {
                is BurnForecastResult.NoActivePromo -> {
                    NoActivePromoCard(onConfigureClick = onNavigateToPromoManagement)
                }
                is BurnForecastResult.Success -> {
                    LiveForecastHeroCard(
                        forecast = forecastResult.forecast,
                        onManageClick = onNavigateToPromoManagement
                    )

                    // Daily Usage Breakdown Chart
                    DailyUsageChartCard(
                        buckets = uiState.dailyUsageBreakdown
                    )
                }
                is BurnForecastResult.PermissionRequired -> {
                    // Handled upstream in MainScreenContent
                }
                is BurnForecastResult.Error -> {
                    ErrorForecastCard(
                        message = forecastResult.message,
                        onManageClick = onNavigateToPromoManagement
                    )
                }
            }
        }
    }
}

@Composable
fun LiveForecastHeroCard(
    forecast: BurnForecast,
    onManageClick: () -> Unit
) {
    val usedRatio = (forecast.dataUsedBytes.toFloat() / forecast.promo.totalAllowanceBytes.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLayer1),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header row: Promo name + SIM badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = forecast.promo.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHighEmphasis
                    )
                    Text(
                        text = if (forecast.promo.isNoExpiry) "Non-Expiring Promo" else "Expiring Promo",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMediumEmphasis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MintPrimary
                ) {
                    Text(
                        text = if (forecast.promo.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2",
                        color = MintOnPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Remaining Data Callout (Dominant Typography with split number + unit)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val dataPair = com.loadpredictor.util.DataFormatter.formatDataPair(
                    remainingBytes = forecast.dataRemainingBytes,
                    totalAllowanceBytes = forecast.promo.totalAllowanceBytes
                )

                Text(
                    text = "REMAINING DATA",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MintPrimary
                )

                // Split display number & unit
                val formattedRemaining = dataPair.remainingFormatted.trim()
                val parts = formattedRemaining.split(" ")
                val numberPart = parts.getOrNull(0) ?: formattedRemaining
                val unitPart = parts.getOrNull(1) ?: ""

                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = numberPart,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = TextHighEmphasis,
                        lineHeight = 50.sp
                    )
                    if (unitPart.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = unitPart,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextHighEmphasis,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                Text(
                    text = "of ${dataPair.totalFormatted} total allowance",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = TextMediumEmphasis
                )
            }

            // Glow Progress Bar with glowing indicator dot
            GlowingProgressBar(
                progress = usedRatio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            )

            // Pace Pill (Semantic colors)
            PaceBadge(pace = forecast.pace, isNoExpiry = forecast.promo.isNoExpiry)

            // Recessed Plain language advisory container
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceRecessed,
                border = BorderStroke(1.dp, DarkOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = SurfaceLayer1,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MintPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = forecast.plainLanguageSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextHighEmphasis
                    )
                }
            }

            // Footer action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onManageClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Switch / Edit Promo →",
                        color = MintPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 100% minSdk 26 compatible Canvas progress bar with ambient radial gradient indicator dot.
 */
@Composable
fun GlowingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val barColor = MintPrimary
    val trackColor = SurfaceLayer2

    Canvas(modifier = modifier) {
        val strokeH = 6.dp.toPx()
        val centerY = size.height / 2f
        val topY = centerY - (strokeH / 2f)
        val cornerRadius = CornerRadius(strokeH / 2f, strokeH / 2f)

        // Draw track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, topY),
            size = Size(size.width, strokeH),
            cornerRadius = cornerRadius
        )

        // Draw filled progress
        val progressWidth = (size.width * progress.coerceIn(0f, 1f)).coerceAtLeast(strokeH)
        drawRoundRect(
            color = barColor,
            topLeft = Offset(0f, topY),
            size = Size(progressWidth, strokeH),
            cornerRadius = cornerRadius
        )

        // Draw glowing indicator dot at progress endpoint
        val dotX = progressWidth
        val dotRadius = 4.dp.toPx()
        val glowRadius = 8.dp.toPx()

        // Outer ambient glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(barColor.copy(alpha = 0.8f), barColor.copy(alpha = 0.2f), Color.Transparent),
                center = Offset(dotX, centerY),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(dotX, centerY)
        )

        // Inner solid white dot
        drawCircle(
            color = Color.White,
            radius = dotRadius,
            center = Offset(dotX, centerY)
        )
    }
}

@Composable
fun PaceBadge(pace: BurnPace, isNoExpiry: Boolean = false) {
    val (bgColor, textColor, label) = when (pace) {
        BurnPace.BURNING_FAST -> Triple(
            PaceCriticalContainer,
            PaceCriticalText,
            "🔥 Burning Fast"
        )
        BurnPace.ON_TRACK -> Triple(
            PaceOnTrackContainer,
            PaceOnTrackText,
            if (isNoExpiry) "⚡ Steady Pace" else "⚡ Pace On Track"
        )
        BurnPace.CONSERVATIVE -> Triple(
            PaceConservativeContainer,
            PaceConservativeText,
            if (isNoExpiry) "🛡️ Light Pace" else "🛡️ Conservative Pace"
        )
        BurnPace.DEPLETED -> Triple(
            PaceCriticalContainer,
            PaceCriticalText,
            "⛔ Data Depleted"
        )
        BurnPace.INSUFFICIENT_DATA -> Triple(
            PaceCalibratingContainer,
            PaceCalibratingText,
            "⏳ Calibrating"
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun NoActivePromoCard(onConfigureClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLayer1),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = MintPrimaryContainer.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MintPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Active Promo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Configure your Philippine prepaid data promo (e.g. Smart Magic Data or GigaSurf) to begin tracking your data burn pace.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = TextMediumEmphasis
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onConfigureClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintPrimary,
                    contentColor = MintOnPrimary
                )
            ) {
                Text(
                    text = "Configure Promo",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ErrorForecastCard(message: String, onManageClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PaceCriticalContainer),
        border = BorderStroke(1.dp, PaceCritical.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = PaceCritical
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Forecast Unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PaceCriticalText
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = PaceCriticalText
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onManageClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Check Promos", color = TextHighEmphasis)
            }
        }
    }
}
