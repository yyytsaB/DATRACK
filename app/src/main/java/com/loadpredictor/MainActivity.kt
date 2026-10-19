package com.loadpredictor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loadpredictor.data.stats.UsageAccessHelper
import com.loadpredictor.domain.model.*
import com.loadpredictor.presentation.*
import com.loadpredictor.presentation.alerts.*
import com.loadpredictor.presentation.common.ForecastCopy
import com.loadpredictor.presentation.history.*
import com.loadpredictor.presentation.navigation.*
import com.loadpredictor.presentation.promo.*
import com.loadpredictor.presentation.theme.LoadPredictorTheme
import com.loadpredictor.presentation.widget.*
import com.loadpredictor.util.DataFormatter
import com.loadpredictor.worker.WorkManagerScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WorkManagerScheduler.schedulePeriodicSync(this)
        setContent {
            LoadPredictorTheme {
                val context = LocalContext.current
                val main: MainViewModel = viewModel(factory = MainViewModel.provideFactory(context))
                val promos: PromoViewModel = viewModel(factory = PromoViewModel.provideFactory(context))
                val history: HistoryViewModel = viewModel(factory = HistoryViewModel.provideFactory(context))
                val alerts: AlertsViewModel = viewModel(factory = AlertsViewModel.provideFactory(context))
                val widgets: WidgetsViewModel = viewModel(factory = WidgetsViewModel.provideFactory(context))
                val state by main.uiState.collectAsStateWithLifecycle()
                var destination by rememberSaveable { mutableStateOf(NavDestination.HOME) }
                val lifecycle = LocalLifecycleOwner.current
                LaunchedEffect(lifecycle, destination) {
                    lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (isActive) {
                            when (destination) {
                                NavDestination.HOME -> main.refresh()
                                NavDestination.HISTORY -> history.refresh()
                                NavDestination.ALERTS -> alerts.refreshPermissionState()
                                NavDestination.WIDGETS -> widgets.refresh()
                                NavDestination.PROMOS -> Unit
                            }
                            delay(60_000L)
                        }
                    }
                }
                BackHandler(destination != NavDestination.HOME) { destination = NavDestination.HOME }
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val expanded = maxWidth >= 600.dp
                    Scaffold(bottomBar = { if (!expanded) LoadPredictorBottomBar(destination, { destination = it }) }) { padding ->
                        Row(Modifier.fillMaxSize().padding(padding)) {
                            if (expanded) NavigationRail {
                                NavDestination.entries.forEach { item -> NavigationRailItem(selected = item == destination,
                                    onClick = { destination = item }, icon = { Icon(item.icon, item.contentDescription) }, label = { Text(item.label) }) }
                            }
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                                Box(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                                    when (destination) {
                                        NavDestination.HOME -> Dashboard(state, { main.refresh() }, { destination = NavDestination.PROMOS },
                                            { destination = NavDestination.HISTORY }, { startActivity(UsageAccessHelper(context).createUsageAccessSettingsIntent()) })
                                        NavDestination.PROMOS -> PromoManagementScreen(promos)
                                        NavDestination.HISTORY -> HistoryScreen(history)
                                        NavDestination.ALERTS -> AlertsScreen(alerts)
                                        NavDestination.WIDGETS -> WidgetsScreen(widgets)
                                    }
                                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dashboard(state: MainUiState, refresh: () -> Unit, promos: () -> Unit, history: () -> Unit, permission: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("DATRACK", fontWeight = FontWeight.Bold) }, actions = {
        IconButton(onClick = refresh) { Icon(Icons.Default.Refresh, "Refresh usage") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            when {
                !state.isUsagePermissionGranted -> {
                    Text("Understand your data usage", style = MaterialTheme.typography.headlineMedium)
                    Text("Allow Usage Access so DATRACK can read mobile-data totals on this device. Your data stays on your phone.")
                    Button(onClick = permission) { Text("Allow Usage Access") }
                }
                state.forecastResult is BurnForecastResult.NoActivePromo -> {
                    Text("Will your data last?", style = MaterialTheme.typography.headlineLarge)
                    Text("Add your current promo and we'll estimate how far your remaining data can take you.")
                    Button(onClick = promos) { Text("Add a promo") }
                }
                state.forecastResult is BurnForecastResult.Error -> {
                    Text("Couldn't read usage", style = MaterialTheme.typography.headlineMedium)
                    Text((state.forecastResult as BurnForecastResult.Error).message)
                    state.lastSuccessfulForecast?.let { previous ->
                        Text("Last known: ${DataFormatter.formatBytes(previous.dataRemainingBytes)} for ${previous.promo.name}")
                        Text("Stale reading · ${ForecastCopy.updated(previous.observedAt, System.currentTimeMillis())}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = refresh) { Text("Try again") }
                }
                state.forecastResult is BurnForecastResult.Success -> {
                    val forecast = (state.forecastResult as BurnForecastResult.Success).forecast
                    val now = System.currentTimeMillis()
                    val expired = forecast.pace == BurnPace.EXPIRED
                    Text(forecast.promo.name, style = MaterialTheme.typography.titleLarge)
                    Text(if (expired) "Promo expired" else "${DataFormatter.formatBytes(forecast.dataRemainingBytes)} left",
                        style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold)
                    if (expired) Text("${DataFormatter.formatBytes(forecast.dataRemainingBytes)} unused when validity ended. This data is no longer available.")
                    else {
                        Text(ForecastCopy.status(forecast.pace, forecast.promo.isNoExpiry), style = MaterialTheme.typography.headlineSmall)
                        LinearProgressIndicator(progress = { forecast.dataRemainingBytes.toFloat() / forecast.promo.totalAllowanceBytes },
                            modifier = Modifier.fillMaxWidth().height(8.dp))
                        Text("Of ${DataFormatter.formatBytes(forecast.promo.totalAllowanceBytes)} total", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(ForecastCopy.runway(forecast.pace, forecast.estimatedDepletionTimestamp, now), style = MaterialTheme.typography.titleLarge)
                            Text(if (forecast.promo.isNoExpiry) "No expiry · data allowance only" else "Promo ends ${DataFormatter.formatDepletionDateTime(forecast.promo.expirationTimestamp!!, now)}")
                            if (!expired && forecast.pace != BurnPace.INSUFFICIENT_DATA && forecast.pace != BurnPace.DEPLETED)
                                Text("${DataFormatter.formatBytes((forecast.burnRateBytesPerHour * 24).toLong())} daily estimate", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    var explain by rememberSaveable { mutableStateOf(false) }
                    TextButton(onClick = { explain = !explain }) { Text(if (explain) "Hide estimate details" else "How is this estimated?") }
                    if (explain) {
                        Text("Based on ${forecast.modelSource.lowercase()}. Usage Access reports device-total mobile traffic across SIMs. Carrier bonus and unlimited-app data are not separated. This is an estimate, not a carrier balance.")
                        if (forecast.depletionEarlyTimestamp != null && forecast.depletionLateTimestamp != null)
                            Text("Estimated range: ${ForecastCopy.date(forecast.depletionEarlyTimestamp)} to ${ForecastCopy.date(forecast.depletionLateTimestamp)}. Actual usage may vary.")
                    }
                    Text("${ForecastCopy.updated(forecast.observedAt, now)} · ${forecast.promo.simSlot.name.replace('_', ' ')} manual context",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Android's usage records may arrive later than the traffic itself.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    Text("Your recent usage", style = MaterialTheme.typography.titleLarge)
                    state.dailyUsageBreakdown.takeLast(3).reversed().forEach { bucket ->
                        Text("${DataFormatter.formatDate(bucket.startTimestamp)} · ${DataFormatter.formatBytes(bucket.totalBytes)}")
                    }
                    OutlinedButton(onClick = history) { Text("Explore usage history") }
                    TextButton(onClick = promos) { Text("Manage promo") }
                }
                else -> CircularProgressIndicator()
            }
        }
    }
}
