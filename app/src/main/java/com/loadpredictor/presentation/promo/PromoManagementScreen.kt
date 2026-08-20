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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
                        text = "Manage Promos",
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
            // SIM 1 / SIM 2 Filter Segmented Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceLayer1, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf(
                        Pair(null, "All SIMs"),
                        Pair(SimSlot.SIM_1, "SIM 1"),
                        Pair(SimSlot.SIM_2, "SIM 2")
                    )

                    filters.forEach { (slot, title) ->
                        val isSelected = selectedSimFilter == slot
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MintPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
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
                                    color = if (isSelected) MintOnPrimary else TextMediumEmphasis
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

            // Active promo context card
            if (uiState.activePromo != null) {
                item {
                    Text(
                        text = "ACTIVE TRACKING CONTEXT",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MintPrimary
                    )
                }
                item {
                    ActivePromoCard(promo = uiState.activePromo!!)
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
                    PromoItemCard(
                        promo = promo,
                        isActive = (uiState.activePromo?.id == promo.id),
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
fun ActivePromoCard(promo: Promo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, AlertCardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PurpleGradientBrush)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = promo.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHighEmphasis
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MintPrimary
                    ) {
                        Text(
                            text = if (promo.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2",
                            color = MintOnPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Allowance",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMediumEmphasis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = com.loadpredictor.util.DataFormatter.formatBytes(promo.totalAllowanceBytes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHighEmphasis
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Validity",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMediumEmphasis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (promo.isNoExpiry) "No Expiration" else "Expiring",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHighEmphasis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PromoItemCard(
    promo: Promo,
    isActive: Boolean,
    onSelectActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectActive),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) SurfaceLayer2 else SurfaceLayer1
        ),
        border = BorderStroke(
            1.dp,
            if (isActive) MintPrimary.copy(alpha = 0.6f) else BorderHighlight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isActive,
                onClick = onSelectActive,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MintPrimary,
                    unselectedColor = Color(0xFF6B7280)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = promo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextHighEmphasis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceRecessed,
                        border = BorderStroke(1.dp, DarkOutlineVariant)
                    ) {
                        Text(
                            text = if (promo.simSlot == SimSlot.SIM_1) "SIM 1" else "SIM 2",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMediumEmphasis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${com.loadpredictor.util.DataFormatter.formatBytes(promo.totalAllowanceBytes)} • ${if (promo.isNoExpiry) "No Expiry" else "Expiring"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMediumEmphasis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Promo",
                    tint = PaceCritical
                )
            }
        }
    }
}
