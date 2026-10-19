package com.loadpredictor.presentation.promo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.util.DataFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoManagementScreen(viewModel: PromoViewModel, onNavigateBack: (() -> Unit)? = null) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var add by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editing = state.promos.find { it.id == editingId }
    val deleting = state.promos.find { it.id == deleteId }
    Scaffold(topBar = { TopAppBar(title = { Text("Your promos") }) }, floatingActionButton = {
        ExtendedFloatingActionButton(onClick = { viewModel.clearErrorMessage(); add = true },
            icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add promo") })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("One manual tracking context at a time. Device mobile usage includes both SIMs.",
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            state.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (!state.isLoading && state.promos.isEmpty()) item { Text("Add your current promo to find out whether it will last.") }
            items(state.promos.sortedByDescending { it.id == state.activePromo?.id }, key = { it.id }) { promo ->
                val active = state.activePromo?.id == promo.id
                val forecast = state.activeForecast?.takeIf { it.promo.id == promo.id }
                val remaining = if (active && forecast != null) forecast.dataRemainingBytes else promo.remainingBytes(promo.lastSyncDataUsedBytes)
                val expired = promo.isExpired(System.currentTimeMillis())
                OutlinedCard(Modifier.fillMaxWidth().clickable { viewModel.selectActivePromo(promo.id) }) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(promo.name, style = MaterialTheme.typography.titleLarge)
                        Text("${promo.simSlot.name.replace('_', ' ')} · ${if (active) "Tracking" else "Saved snapshot"}",
                            color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Text(if (expired) "Expired · ${DataFormatter.formatBytes(remaining)} unused" else "${DataFormatter.formatBytes(remaining)} left",
                            style = MaterialTheme.typography.headlineSmall)
                        Text(if (promo.isNoExpiry) "No expiry" else "Ends ${DataFormatter.formatDepletionDateTime(promo.expirationTimestamp!!)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!active) Text(if (promo.lastSyncTimestamp > 0) "Last observed ${DataFormatter.formatDepletionDateTime(promo.lastSyncTimestamp)}" else "No live reading yet",
                            style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.clearErrorMessage(); editingId = promo.id }) { Text("Edit") }
                            if (!active) TextButton(onClick = { viewModel.selectActivePromo(promo.id) }) { Text("Track this promo") }
                            TextButton(onClick = { deleteId = promo.id }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
    if (deleting != null) AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Delete ${deleting.name}?") },
        text = { Text(if (deleting.isActive) "This removes your active tracking context. Your device's usage records are not deleted." else "This removes the saved promo and its tracking settings.") },
        confirmButton = { TextButton(onClick = { viewModel.deletePromo(deleting); deleteId = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Keep promo") } })
    if (add || editing != null) PromoEntryDialog(
        onDismissRequest = { add = false; editingId = null; viewModel.clearErrorMessage() }, promoToEdit = editing,
        saveError = state.errorMessage, isSaving = state.isSaving,
        onSavePromo = { id, name, allowance, start, expiry, offset, sim, active, existing ->
            viewModel.savePromo(name, allowance, start, expiry, offset, sim, active, id, existing,
                onSuccess = { add = false; editingId = null })
        })
}
