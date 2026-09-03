package com.loadpredictor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
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
import com.loadpredictor.presentation.alerts.AlertsScreen
import com.loadpredictor.presentation.common.UsagePermissionRequiredCard
import com.loadpredictor.presentation.dashboard.BurnAlertsOptInCard
import com.loadpredictor.presentation.dashboard.DailyUsageTeaserCard
import com.loadpredictor.presentation.dashboard.DashboardStatChips
import com.loadpredictor.presentation.dashboard.RadialPaceRing
import com.loadpredictor.presentation.history.HistoryScreen
import com.loadpredictor.presentation.history.HistoryViewModel
import com.loadpredictor.presentation.navigation.LoadPredictorBottomBar
import com.loadpredictor.presentation.navigation.NavDestination
import com.loadpredictor.presentation.promo.PromoManagementScreen
import com.loadpredictor.presentation.promo.PromoViewModel
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.DarkBackground
import com.loadpredictor.presentation.theme.DarkOutlineVariant
import com.loadpredictor.presentation.theme.LoadPredictorTheme
import com.loadpredictor.presentation.theme.MintOnPrimary
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.MintPrimaryContainer
import com.loadpredictor.presentation.theme.PaceCalibrating
import com.loadpredictor.presentation.theme.PaceCalibratingContainer
import com.loadpredictor.presentation.theme.PaceCalibratingText
import com.loadpredictor.presentation.theme.PaceConservative
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
import com.loadpredictor.presentation.theme.TextMediumEmphasis
import com.loadpredictor.presentation.widget.WidgetsScreen
import com.loadpredictor.util.DataFormatter
import com.loadpredictor.worker.WorkManagerScheduler

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
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.provideFactory(context)
                )
                val alertsViewModel: com.loadpredictor.presentation.alerts.AlertsViewModel = viewModel(
                    factory = com.loadpredictor.presentation.alerts.AlertsViewModel.provideFactory(context)
                )
                val widgetsViewModel: com.loadpredictor.presentation.widget.WidgetsViewModel = viewModel(
                    factory = com.loadpredictor.presentation.widget.WidgetsViewModel.provideFactory(context)
                )
                val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

                var currentDestination by remember { mutableStateOf(NavDestination.HOME) }

                // Lifecycle-aware foreground ticker: refreshes immediately on entry (and on resume from Settings)
                // and every 30 seconds while in the foreground, automatically cancelling when backgrounded.
                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (isActive) {
                            mainViewModel.refresh()
                            historyViewModel.refresh()
                            alertsViewModel.refreshPermissionState()
                            widgetsViewModel.refresh()
                            delay(30_000L)
                        }
                    }
                }

                androidx.activity.compose.BackHandler(enabled = currentDestination != NavDestination.HOME) {
                    currentDestination = NavDestination.HOME
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    bottomBar = {
                        LoadPredictorBottomBar(
                            currentDestination = currentDestination,
                            onNavigateToDestination = { currentDestination = it }
                        )
                    }
                ) { innerPadding ->
                    when (currentDestination) {
                        NavDestination.HOME -> {
                            MainScreenContent(
                                uiState = uiState,
                                onGrantPermissionClick = {
                                    startActivity(usageAccessHelper.createUsageAccessSettingsIntent())
                                },
                                onNavigateToPromos = {
                                    currentDestination = NavDestination.PROMOS
                                },
                                onNavigateToHistory = {
                                    currentDestination = NavDestination.HISTORY
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        NavDestination.PROMOS -> {
                            Box(modifier = Modifier.padding(innerPadding)) {
                                PromoManagementScreen(
                                    viewModel = promoViewModel,
                                    onNavigateBack = null
                                )
                            }
                        }
                        NavDestination.HISTORY -> {
                            Box(modifier = Modifier.padding(innerPadding)) {
                                HistoryScreen(
                                    viewModel = historyViewModel
                                )
                            }
                        }
                        NavDestination.ALERTS -> {
                            Box(modifier = Modifier.padding(innerPadding)) {
                                AlertsScreen(
                                    viewModel = alertsViewModel
                                )
                            }
                        }
                        NavDestination.WIDGETS -> {
                            Box(modifier = Modifier.padding(innerPadding)) {
                                WidgetsScreen(
                                    viewModel = widgetsViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        WorkManagerScheduler.enqueueImmediateSync(this)
    }
}

@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onGrantPermissionClick: () -> Unit,
    onNavigateToPromos: () -> Unit,
    onNavigateToHistory: () -> Unit,
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
        uiState.forecastResult is BurnForecastResult.NoActivePromo -> {
            com.loadpredictor.presentation.common.NoActivePromoScreen(
                onConfigureClick = onNavigateToPromos,
                modifier = modifier
            )
        }
        else -> {
            DashboardView(
                uiState = uiState,
                onNavigateToPromos = onNavigateToPromos,
                onNavigateToHistory = onNavigateToHistory,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardView(
    uiState: MainUiState,
    onNavigateToPromos: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "DATRACK",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 1.sp,
                    color = TextHighEmphasis
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground,
                titleContentColor = TextHighEmphasis
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            when (val forecastResult = uiState.forecastResult) {
                is BurnForecastResult.NoActivePromo -> {
                    NoActivePromoCard(onConfigureClick = onNavigateToPromos)
                }
                is BurnForecastResult.Success -> {
                    LiveForecastHeroSection(
                        forecast = forecastResult.forecast,
                        dailyAvgBytes = uiState.dailyUsageBreakdown.let { list ->
                            if (list.isEmpty()) 0L else list.map { it.totalBytes }.average().toLong()
                        }
                    )

                    // Daily Usage Breakdown Teaser Strip
                    DailyUsageTeaserCard(
                        buckets = uiState.dailyUsageBreakdown,
                        onViewHistoryClick = onNavigateToHistory
                    )
                }
                is BurnForecastResult.PermissionRequired -> {
                    // Handled upstream in MainScreenContent
                }
                is BurnForecastResult.Error -> {
                    ErrorForecastCard(
                        message = forecastResult.message,
                        onManageClick = onNavigateToPromos
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LiveForecastHeroSection(
    forecast: BurnForecast,
    dailyAvgBytes: Long
) {
    val remainingRatio = (forecast.dataRemainingBytes.toFloat() / forecast.promo.totalAllowanceBytes.toFloat()).coerceIn(0f, 1f)
    val dataPair = DataFormatter.formatDataPair(
        remainingBytes = forecast.dataRemainingBytes,
        totalAllowanceBytes = forecast.promo.totalAllowanceBytes
    )
    val simSlotTitle = if (forecast.promo.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2"
    val depletionHighlight = formatDepletionHeadline(forecast)
    val promoExpiryHeadline = formatPromoExpiryHeadline(forecast)

    val paceHighlightColor = when (forecast.pace) {
        BurnPace.BURNING_FAST -> Color(0xFFFF9F43)
        BurnPace.ON_TRACK -> MintPrimary
        BurnPace.CONSERVATIVE -> PaceConservative
        BurnPace.INSUFFICIENT_DATA -> PaceCalibrating
        BurnPace.DEPLETED -> Color(0xFFFF5252)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Context Row: Promo name & sync status on Left, SIM chip on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "${forecast.promo.name} • $simSlotTitle",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E9AA8),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Synced just now",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5A6678),
                    fontSize = 12.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF142426),
                border = BorderStroke(1.dp, Color(0xFF1B3D3B))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF05D686), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = simSlotTitle,
                        color = Color(0xFF05D686),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2. Headline Prediction Section
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "At current pace, you'll run out",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 22.sp
            )
            Text(
                text = depletionHighlight,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = paceHighlightColor,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = promoExpiryHeadline,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E9AA8),
                fontSize = 13.sp
            )
        }

        // 3. Pace Status Badge Pill
        PaceBadge(pace = forecast.pace, isNoExpiry = forecast.promo.isNoExpiry)

        // 4. Hero Radial Ring Gauge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            RadialPaceRing(
                remainingRatio = remainingRatio,
                remainingFormatted = dataPair.remainingFormatted,
                totalAllowanceFormatted = dataPair.totalFormatted,
                pace = forecast.pace,
                size = 230.dp,
                strokeWidth = 15.dp
            )
        }

        // 5. 3 Stat Chips (Daily avg, Days left, At this pace)
        DashboardStatChips(
            forecast = forecast,
            dailyAvgBytes = dailyAvgBytes
        )
    }
}

@Composable
fun PaceBadge(pace: BurnPace, isNoExpiry: Boolean = false) {
    val (bgColor, dotColor, textColor, label) = when (pace) {
        BurnPace.BURNING_FAST -> Quadruple(
            Color(0xFF2E2016),
            Color(0xFFFF9F43),
            Color(0xFFFF9F43),
            if (isNoExpiry) "Burning Fast" else "Cutting It Close"
        )
        BurnPace.ON_TRACK -> Quadruple(
            Color(0xFF0C2B1D),
            Color(0xFF00F5D4),
            Color(0xFF00F5D4),
            if (isNoExpiry) "Steady Pace" else "Safe Pace"
        )
        BurnPace.CONSERVATIVE -> Quadruple(
            Color(0xFF0F2438),
            Color(0xFF38BDF8),
            Color(0xFF38BDF8),
            if (isNoExpiry) "Light Pace" else "Conservative Pace"
        )
        BurnPace.DEPLETED -> Quadruple(
            Color(0xFF2E1414),
            Color(0xFFFF5252),
            Color(0xFFFF5252),
            "Data Depleted"
        )
        BurnPace.INSUFFICIENT_DATA -> Quadruple(
            Color(0xFF2E2414),
            Color(0xFFFBBF24),
            Color(0xFFFBBF24),
            "Calibrating"
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatDepletionHeadline(forecast: BurnForecast): String {
    val now = System.currentTimeMillis()
    val est = forecast.estimatedDepletionTimestamp
    return when {
        forecast.pace == BurnPace.DEPLETED -> "Data already depleted"
        forecast.pace == BurnPace.INSUFFICIENT_DATA -> "Calculating burn pace..."
        est == null -> "Will last until promo ends"
        est <= now -> "Depleting very soon"
        else -> com.loadpredictor.util.DataFormatter.formatDepletionDateTime(est, now)
    }
}

private fun formatPromoExpiryHeadline(forecast: BurnForecast): String {
    val promo = forecast.promo
    val now = System.currentTimeMillis()
    return if (promo.isNoExpiry || promo.expirationTimestamp == null) {
        "No expiration • Data cap only"
    } else {
        val expDateStr = com.loadpredictor.util.DataFormatter.formatDate(promo.expirationTimestamp, now)
        val daysLeft = ((promo.expirationTimestamp - now).coerceAtLeast(0L) / (1000 * 60 * 60 * 24)).toInt()
        val daysText = if (daysLeft == 0) "< 1 day" else "$daysLeft ${if (daysLeft == 1) "day" else "days"}"
        "Promo ends $expDateStr • $daysText left"
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
