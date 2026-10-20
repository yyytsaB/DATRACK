package com.loadpredictor.presentation.promo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.DarkBackground
import com.loadpredictor.presentation.theme.DarkOutline
import com.loadpredictor.presentation.theme.DarkOutlineVariant
import com.loadpredictor.presentation.theme.MintOnPrimary
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.MintPrimaryContainer
import com.loadpredictor.presentation.theme.PaceCritical
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.SurfaceLayer2
import com.loadpredictor.presentation.theme.SurfaceRecessed
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis

/** Saved contexts keep their full names above a separate action row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoManagementScreen(
    viewModel: PromoViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Promo?>(null) }
    var showAddDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var promoToEdit by remember { mutableStateOf<Promo?>(null) }
    var selectedSimFilter by remember { mutableStateOf<SimSlot?>(null) } // null = All SIMs

    val filteredPromos = remember(uiState.promos, selectedSimFilter) {
        if (selectedSimFilter == null) {
            uiState.promos
        } else {
            uiState.promos.filter { it.simSlot == selectedSimFilter }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            com.loadpredictor.presentation.common.ScreenHeader("Promos", "Choose the data plan you’re tracking.")
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.errorMessage?.let { message -> item { Text(message, color = PaceCritical) } }
            item {
                Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add promo")
                }
            }
            // SIM 1 / SIM 2 Filter Segmented Buttons (Matching tab/pill style in preview (4))
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        Pair(null, "All SIMs"),
                        Pair(SimSlot.SIM_1, "SIM 1"),
                        Pair(SimSlot.SIM_2, "SIM 2")
                    )

                    filters.forEach { (slot, title) ->
                        val isSelected = selectedSimFilter == slot
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MintPrimaryContainer else Color(0xFF213034),
                            border = if (isSelected) BorderStroke(1.dp, MintPrimary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            onClick = { selectedSimFilter = slot }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MintPrimary else TextMediumEmphasis
                                )
                            }
                        }
                    }
                }
            }

            // Informative sample disclaimer callout
            item {
                Surface(
                    color = SurfaceRecessed,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DarkOutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = SurfaceLayer1,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MintPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Select a promo to track it. Usage covers all mobile data on this device; SIM labels are for your reference.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMediumEmphasis,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Saved Promos (${filteredPromos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis
                )
            }

            if (filteredPromos.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLayer1),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BorderHighlight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (selectedSimFilter != null) "No promos for this SIM slot." else "No promos configured yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextHighEmphasis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap the + button below to add your active mobile data promo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMediumEmphasis
                            )
                        }
                    }
                }
            } else {
                items(filteredPromos, key = { it.id }) { promo ->
                    val isActive = (uiState.activePromo?.id == promo.id)
                    PromoItemCard(
                        promo = promo,
                        isActive = isActive,
                        activeForecast = if (isActive) uiState.activeForecast else null,
                        onSelectActive = { viewModel.selectActivePromo(promo.id) },
                        onEdit = { promoToEdit = promo },
                        onDelete = { pendingDelete = promo }
                    )
                }
            }
        }

        pendingDelete?.let { promo ->
            androidx.compose.material3.AlertDialog(onDismissRequest = { pendingDelete = null },
                title = { Text("Delete ${promo.name}?") },
                text = { Text("This removes the saved promo${if (promo.isActive) " and active tracking context" else ""}.") },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { viewModel.deletePromo(promo); pendingDelete = null }) { Text("Delete") } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { pendingDelete = null }) { Text("Keep promo") } })
        }
        if (showAddDialog || promoToEdit != null) {
            PromoEntryDialog(
                promoToEdit = promoToEdit,
                saveError = uiState.errorMessage,
                isSaving = uiState.isSaving,
                onDismissRequest = {
                    showAddDialog = false
                    promoToEdit = null
                },
                onSavePromo = { id, name, allowanceBytes, startTimestamp, expirationTimestamp, initialUsageOffsetBytes, simSlot, isActive, existingPromo, carrier, priceCentavos ->
                    viewModel.savePromo(
                        id = id,
                        name = name,
                        allowanceBytes = allowanceBytes,
                        startTimestamp = startTimestamp,
                        expirationTimestamp = expirationTimestamp,
                        initialUsageOffsetBytes = initialUsageOffsetBytes,
                        simSlot = simSlot,
                        isActive = isActive,
                        existingPromo = existingPromo, carrier = carrier, priceCentavos = priceCentavos,
                        onSuccess = {
                            showAddDialog = false
                            promoToEdit = null
                        }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromoItemCard(
    promo: Promo, isActive: Boolean,
    activeForecast: com.loadpredictor.domain.model.BurnForecast? = null,
    onSelectActive: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val expired = promo.isExpired(System.currentTimeMillis())
    val remaining = if (isActive && activeForecast != null) activeForecast.dataRemainingBytes else promo.remainingBytes(promo.lastSyncDataUsedBytes)
    val fraction = (remaining.toFloat() / promo.totalAllowanceBytes.coerceAtLeast(1)).coerceIn(0f, 1f)
    Surface(color = if (isActive) SurfaceLayer2 else SurfaceLayer1,
        shape = RoundedCornerShape(16.dp),
        border = if (isActive) BorderStroke(1.dp, MintPrimary.copy(alpha = 0.65f)) else null) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(promo.name, style = MaterialTheme.typography.titleLarge, color = TextHighEmphasis)
            Text("${if (promo.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2"} · ${if (isActive) "Tracking now" else "Saved promo"}",
                style = MaterialTheme.typography.bodyMedium, color = if (isActive) MintPrimary else TextMediumEmphasis)
            Text(if (expired) "Expired · unused data is no longer available" else formatPromoDateRange(promo),
                style = MaterialTheme.typography.bodyMedium, color = TextMediumEmphasis)
            Text("${com.loadpredictor.util.DataFormatter.formatBytes(remaining)} ${if (expired) "unused" else "remaining"}",
                style = MaterialTheme.typography.titleMedium, color = TextHighEmphasis)
            LinearProgressIndicator(progress = { if (expired) 0f else fraction }, Modifier.fillMaxWidth().height(6.dp),
                color = if (isActive) MintPrimary else TextMediumEmphasis, trackColor = DarkOutline, drawStopIndicator = {})
            Text("of ${com.loadpredictor.util.DataFormatter.formatBytes(promo.totalAllowanceBytes)}${if (!isActive) " · saved snapshot" else ""}",
                style = MaterialTheme.typography.bodySmall, color = TextMediumEmphasis)
            HorizontalDivider(color = DarkOutlineVariant)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isActive) {
                    Text("Active promo", Modifier.padding(vertical = 14.dp, horizontal = 8.dp), style = MaterialTheme.typography.labelLarge, color = MintPrimary)
                } else {
                    FilledTonalButton(onClick = onSelectActive, modifier = Modifier.heightIn(min = 48.dp)) { Text("Track promo") }
                }
                TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit") }
                TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = PaceCritical)) { Text("Delete") }
            }
        }
    }
}

private fun formatPromoDateRange(promo: Promo): String {
    return com.loadpredictor.util.DataFormatter.formatDateRange(promo.startTimestamp, promo.expirationTimestamp)
}
