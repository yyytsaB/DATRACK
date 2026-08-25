package com.loadpredictor.presentation.promo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.presentation.theme.AlertCardBorder
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.DarkBackground
import com.loadpredictor.presentation.theme.DarkOutline
import com.loadpredictor.presentation.theme.DarkOutlineVariant
import com.loadpredictor.presentation.theme.MintGlow
import com.loadpredictor.presentation.theme.MintOnPrimary
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.MintPrimaryContainer
import com.loadpredictor.presentation.theme.PaceCritical
import com.loadpredictor.presentation.theme.PurpleGradientBrush
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.SurfaceLayer2
import com.loadpredictor.presentation.theme.SurfaceRecessed
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis

/**
 * Screen for viewing, switching, adding, and deleting tracked promos.
 *
 * Matches Panel 3 of the reference design:
 * - Recessed top guidance callout
 * - Midnight purple gradient Active Tracking Context card with dual metrics
 * - Layered dark promo cards with mint radio selection
 * - Glowing mint Floating Action Button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoManagementScreen(
    viewModel: PromoViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
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
            TopAppBar(
                title = {
                    Text(
                        text = "Promos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextHighEmphasis
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextHighEmphasis
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextHighEmphasis
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MintPrimary,
                contentColor = MintOnPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = MintGlow,
                    ambientColor = MintGlow
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Promo",
                    tint = MintOnPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                            color = if (isSelected) MintPrimaryContainer else Color(0xFF1B2230),
                            border = if (isSelected) BorderStroke(1.dp, MintPrimary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            onClick = { selectedSimFilter = slot }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
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
                            text = "Presets are common sample configurations. Adjust allowance and duration to match your exact subscribed promo.",
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
                        onDelete = { viewModel.deletePromo(promo) }
                    )
                }
            }
        }

        if (showAddDialog) {
            PromoEntryDialog(
                onDismissRequest = { showAddDialog = false },
                onSavePromo = { name, allowanceBytes, startTimestamp, expirationTimestamp, initialUsageOffsetBytes, simSlot, isActive ->
                    viewModel.savePromo(
                        name = name,
                        allowanceBytes = allowanceBytes,
                        startTimestamp = startTimestamp,
                        expirationTimestamp = expirationTimestamp,
                        initialUsageOffsetBytes = initialUsageOffsetBytes,
                        simSlot = simSlot,
                        isActive = isActive,
                        onSuccess = { showAddDialog = false }
                    )
                }
            )
        }
    }
}

@Composable
fun PromoItemCard(
    promo: Promo,
    isActive: Boolean,
    activeForecast: com.loadpredictor.domain.model.BurnForecast? = null,
    onSelectActive: () -> Unit,
    onDelete: () -> Unit
) {
    val dateSubtitle = formatPromoDateRange(promo)
    val totalBytes = promo.totalAllowanceBytes
    val remainingBytes = if (isActive && activeForecast != null) {
        activeForecast.dataRemainingBytes
    } else {
        val usedBytes = if (promo.lastSyncDataUsedBytes > 0L) {
            promo.lastSyncDataUsedBytes
        } else {
            promo.initialUsageOffsetBytes
        }
        (totalBytes - usedBytes).coerceIn(0L, totalBytes)
    }
    val remainingRatio = if (totalBytes > 0L) {
        (remainingBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val dataPair = com.loadpredictor.util.DataFormatter.formatDataPair(
        remainingBytes = remainingBytes,
        totalAllowanceBytes = totalBytes
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectActive),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) SurfaceLayer2 else SurfaceLayer1
        ),
        border = BorderStroke(
            1.dp,
            if (isActive) MintPrimary.copy(alpha = 0.5f) else BorderHighlight
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Promo Name & Dates on Left, Active / Set Active Badge + Delete on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = promo.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHighEmphasis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SurfaceRecessed,
                            border = BorderStroke(1.dp, DarkOutlineVariant)
                        ) {
                            Text(
                                text = if (promo.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMediumEmphasis,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMediumEmphasis,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0C2B1D)
                        ) {
                            Text(
                                text = "Active",
                                color = Color(0xFF05D686),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E2532),
                            onClick = onSelectActive
                        ) {
                            Text(
                                text = "Set Active",
                                color = Color(0xFF8E9AA8),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Promo",
                            tint = PaceCritical.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar and Metric Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Horizontal linear bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(Color(0xFF252D3D), RoundedCornerShape(3.dp))
                ) {
                    if (remainingRatio > 0f) {
                        val barColor = if (isActive) {
                            com.loadpredictor.presentation.theme.getDataProgressColor(remainingRatio)
                        } else {
                            Color(0xFF64748B)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(remainingRatio)
                                .background(barColor, RoundedCornerShape(3.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Metric text
                Text(
                    text = "${dataPair.remainingFormatted} / ${dataPair.totalFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumEmphasis,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isActive) {
                Spacer(modifier = Modifier.height(6.dp))
                val snapshotText = when {
                    promo.lastSyncTimestamp > 0L -> {
                        "Snapshot as of ${com.loadpredictor.util.DataFormatter.formatDate(promo.lastSyncTimestamp)}"
                    }
                    promo.initialUsageOffsetBytes > 0L -> "Initial baseline snapshot"
                    else -> "Not yet tracked"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = snapshotText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMediumEmphasis.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

private fun formatPromoDateRange(promo: Promo): String {
    return com.loadpredictor.util.DataFormatter.formatDateRange(promo.startTimestamp, promo.expirationTimestamp)
}

