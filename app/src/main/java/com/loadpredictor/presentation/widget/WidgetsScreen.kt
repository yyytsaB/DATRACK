package com.loadpredictor.presentation.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loadpredictor.presentation.common.ForecastCopy
import com.loadpredictor.util.DataFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(viewModel: WidgetsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("On your home screen") }) }) { padding ->
        Column(modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("Your data, at a glance.", style = MaterialTheme.typography.headlineMedium)
            Text("See what's left without opening DATRACK. Tap the widget to open your dashboard; use Refresh to request a new reading.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val forecast = state.activeForecast
            listOf("Compact" to false, "Expanded" to true).forEach { (title, wide) ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (forecast == null) "Add a promo" else if (forecast.pace == com.loadpredictor.domain.model.BurnPace.EXPIRED) "Promo expired"
                                else "${DataFormatter.formatBytes(forecast.dataRemainingBytes)} left", style = MaterialTheme.typography.headlineSmall)
                            Text(forecast?.let { ForecastCopy.widgetSummary(it.pace, it.estimatedDepletionTimestamp,
                                it.promo.isNoExpiry, System.currentTimeMillis(), wide) } ?: "Open DATRACK to start tracking")
                            if (wide && forecast != null) Text(ForecastCopy.updated(forecast.observedAt, System.currentTimeMillis()))
                            Text("Refresh", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Text("The launcher chooses the initial size. Long-press the placed widget and resize it for the expanded layout. Previews illustrate the content; exact spacing depends on your launcher.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { message = if (viewModel.requestPinWidget(context)) "Confirm placement on your home screen." else "Long-press your home screen, choose Widgets, then DATRACK." },
                modifier = Modifier.fillMaxWidth()) { Text("Add home-screen widget") }
            message?.let { Text(it) }
            Text("Background checks run about hourly and may be delayed by Android battery settings. Android's mobile-usage records may also lag. Refresh does not contact your carrier.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
