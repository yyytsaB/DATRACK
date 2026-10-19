package com.loadpredictor.presentation.widget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.presentation.common.ScreenHeader
import com.loadpredictor.presentation.common.ForecastCopy
import com.loadpredictor.presentation.theme.*
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.launch

@Composable
fun WidgetsScreen(viewModel: WidgetsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(containerColor = DarkBackground, topBar = { ScreenHeader("Widgets", "Your balance, on your home screen.") },
        snackbarHost = { SnackbarHost(snack) }) { padding ->
        Column(modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("Check your data without opening the app.", style = MaterialTheme.typography.headlineSmall, color = TextHighEmphasis)
            val forecast = state.activeForecast
            Text(if (forecast != null) "Preview with your active promo" else "Illustrative preview · add an active promo for live values",
                style = MaterialTheme.typography.bodyMedium, color = TextMediumEmphasis)
            Surface(color = SurfaceLayer1, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(forecast?.promo?.name ?: "Example promo", style = MaterialTheme.typography.titleMedium, color = TextHighEmphasis)
                    Text(if (forecast?.pace == BurnPace.EXPIRED) "Expired" else "${forecast?.let { DataFormatter.formatBytes(it.dataRemainingBytes) } ?: "8.0 GB"} left",
                        style = MaterialTheme.typography.headlineLarge, color = MintPrimary)
                    LinearProgressIndicator(progress = { forecast?.let { (it.dataRemainingBytes.toFloat() / it.promo.totalAllowanceBytes.coerceAtLeast(1)).coerceIn(0f,1f) } ?: 0.66f }, modifier = Modifier.fillMaxWidth(), color = MintPrimary, trackColor = DarkOutline)
                    Text(forecast?.let { ForecastCopy.widgetSummary(it.pace, it.estimatedDepletionTimestamp, it.promo.isNoExpiry, System.currentTimeMillis(), true) } ?: "Steady usage",
                        style = MaterialTheme.typography.bodyMedium, color = TextMediumEmphasis)
                }
            }
            Button(onClick = {
                val sent = viewModel.requestPinWidget(context)
                scope.launch { snack.showSnackbar(if (sent) "Confirm placement on your home screen." else "Open your home screen’s widget picker and choose DATRACK Preview.") }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Add home screen widget") }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Make it yours", style = MaterialTheme.typography.titleLarge, color = TextHighEmphasis)
                Text("Touch and hold the widget to resize it. A wider layout shows the promo name and more detail. Your launcher controls the final size.", color = TextMediumEmphasis)
                HorizontalDivider(color = DarkOutlineVariant)
                Text("Tap the balance to open DATRACK Preview. Tap the refresh icon for a new reading.", color = TextMediumEmphasis)
                HorizontalDivider(color = DarkOutlineVariant)
                Text("Background readings update about hourly. Android battery settings may delay them; the widget shows when data was last checked.", color = TextMediumEmphasis)
            }
        }
    }
}
