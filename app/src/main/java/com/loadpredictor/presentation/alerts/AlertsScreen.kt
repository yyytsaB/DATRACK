package com.loadpredictor.presentation.alerts

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(viewModel: AlertsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Scaffold(topBar = { TopAppBar(title = { Text("Stay ahead of your data") }) }) { padding ->
        Column(modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(if (state.hasNotificationPermission) "Alerts are enabled" else "Alerts need your attention", style = MaterialTheme.typography.headlineSmall)
            Text(if (state.hasNotificationPermission) "Choose the milestones that matter to you." else "Allow notifications and check the Data Burn & Threshold Alerts channel in Android settings.")
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) {
                Text("Open notification settings")
            }
            AlertToggle("Half used", "When 50% of your allowance is used", state.is50Enabled, viewModel::onToggle50Alert)
            AlertToggle("Getting low", "When 80% of your allowance is used", state.is80Enabled, viewModel::onToggle80Alert)
            AlertToggle("Almost out", "When 90% of your allowance is used", state.is90Enabled, viewModel::onToggle90Alert)
            AlertToggle("May run out early", "If the estimate is at least 12 hours before expiry", state.isPrematureEnabled, viewModel::onTogglePrematureAlert)
            Text("Background checks run about hourly. Android may delay alerts while conserving battery. Turning a milestone off consumes that milestone when crossed; turning it back on won't repeat it.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable
private fun AlertToggle(title: String, description: String, checked: Boolean, toggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = toggle, modifier = Modifier.semanticsLabel(title))
    }
}
private fun Modifier.semanticsLabel(label: String): Modifier = this.then(Modifier.semantics { contentDescription = label })
