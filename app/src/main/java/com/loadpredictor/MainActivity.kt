package com.loadpredictor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loadpredictor.data.stats.UsageAccessHelper
import com.loadpredictor.domain.model.BurnForecast
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.presentation.MainUiState
import com.loadpredictor.presentation.MainViewModel
import com.loadpredictor.presentation.common.UsagePermissionRequiredCard
import com.loadpredictor.presentation.promo.PromoManagementScreen
import com.loadpredictor.presentation.promo.PromoViewModel
import com.loadpredictor.presentation.theme.LoadPredictorTheme
import java.util.Locale

enum class AppScreen {
    DASHBOARD,
    PROMO_MANAGEMENT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

                // Re-check permission automatically when user returns from Settings
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            mainViewModel.checkPermission()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
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
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                CircularProgressIndicator()
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
                forecastResult = uiState.forecastResult,
                onNavigateToPromoManagement = onNavigateToPromoManagement,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardView(
    forecastResult: BurnForecastResult,
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
                Text(
                    text = "Burn-Rate Predictor",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = onNavigateToPromoManagement) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Manage Promos"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (forecastResult) {
                is BurnForecastResult.NoActivePromo -> {
                    NoActivePromoCard(onConfigureClick = onNavigateToPromoManagement)
                }
                is BurnForecastResult.Success -> {
                    LiveForecastHeroCard(
                        forecast = forecastResult.forecast,
                        onManageClick = onNavigateToPromoManagement
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (forecast.promo.isNoExpiry) "Non-Expiring Promo" else "Expiring Promo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = if (forecast.promo.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Remaining Data Callout
            Column {
                Text(
                    text = "REMAINING DATA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatBytes(forecast.dataRemainingBytes),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "of ${formatBytes(forecast.promo.totalAllowanceBytes)} total allowance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { usedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when (forecast.pace) {
                    BurnPace.BURNING_FAST -> MaterialTheme.colorScheme.error
                    BurnPace.DEPLETED -> MaterialTheme.colorScheme.error
                    BurnPace.ON_TRACK -> MaterialTheme.colorScheme.primary
                    BurnPace.CONSERVATIVE -> MaterialTheme.colorScheme.secondary
                    BurnPace.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.outline
                },
                trackColor = MaterialTheme.colorScheme.surface
            )

            // Pace Pill
            PaceBadge(pace = forecast.pace)

            // Plain language advisory block
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = forecast.plainLanguageSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Footer actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onManageClick) {
                    Text("Switch / Edit Promo")
                }
            }
        }
    }
}

@Composable
fun PaceBadge(pace: BurnPace) {
    val (bgColor, textColor, label) = when (pace) {
        BurnPace.BURNING_FAST -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "🔥 Burning Fast"
        )
        BurnPace.ON_TRACK -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "⚡ Pace On Track"
        )
        BurnPace.CONSERVATIVE -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "🛡️ Conservative Pace"
        )
        BurnPace.DEPLETED -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            "⛔ Data Depleted"
        )
        BurnPace.INSUFFICIENT_DATA -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun NoActivePromoCard(onConfigureClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Active Promo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Configure your Philippine prepaid data promo (e.g. Smart Magic Data or GigaSurf) to begin tracking your data burn pace.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onConfigureClick) {
                Text("Configure Promo")
            }
        }
    }
}

@Composable
fun ErrorForecastCard(message: String, onManageClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
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
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Forecast Unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onManageClick) {
                Text("Check Promos")
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) {
        String.format(Locale.US, "%.1f GB", gb)
    } else {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        String.format(Locale.US, "%.0f MB", mb)
    }
}
