package com.loadpredictor.presentation.promo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.DarkOutline
import com.loadpredictor.presentation.theme.DarkOutlineVariant
import com.loadpredictor.presentation.theme.MintGlow
import com.loadpredictor.presentation.theme.MintOnPrimary
import com.loadpredictor.presentation.theme.MintOnPrimaryContainer
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.MintPrimaryContainer
import com.loadpredictor.presentation.theme.PaceCritical
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.SurfaceLayer2
import com.loadpredictor.presentation.theme.SurfaceRecessed
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextLowEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis

enum class DataUnit(val multiplier: Long, val label: String) {
    GB(1024L * 1024L * 1024L, "GB"),
    MB(1024L * 1024L, "MB")
}

/**
 * Material 3 Dialog for creating or customizing a mobile data promo,
 * matching Panel 4 of the reference design:
 * - 2x2 FlowRow quick preset grid
 * - Dark filled input containers with floating labels
 * - Segmented GB/MB toggle pills
 * - No Expiration card
 * - Side-by-side SIM slot selector cards with SIM phone icons
 * - Mint CTA button with ambient glow shadow
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromoEntryDialog(
    onDismissRequest: () -> Unit,
    onSavePromo: (
        name: String,
        allowanceBytes: Long,
        startTimestamp: Long,
        expirationTimestamp: Long?,
        initialUsageOffsetBytes: Long,
        simSlot: SimSlot,
        isActive: Boolean
    ) -> Unit
) {
    var selectedPresetTitle by remember { mutableStateOf<String?>(null) }
    var promoName by remember { mutableStateOf("") }
    var allowanceValueStr by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(DataUnit.GB) }
    var remainingValueStr by remember { mutableStateOf("") }
    var remainingUnit by remember { mutableStateOf(DataUnit.GB) }
    var isNoExpiry by remember { mutableStateOf(false) }
    var durationDaysStr by remember { mutableStateOf("7") }
    var selectedSimSlot by remember { mutableStateOf(SimSlot.SIM_1) }
    var setAsActive by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun applyPreset(preset: PromoPreset) {
        selectedPresetTitle = preset.title
        promoName = preset.title
        val gb = preset.allowanceBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        allowanceValueStr = if (gb >= 1.0) gb.toInt().toString() else (preset.allowanceBytes / (1024 * 1024)).toString()
        selectedUnit = if (gb >= 1.0) DataUnit.GB else DataUnit.MB
        isNoExpiry = (preset.durationDays == null)
        durationDaysStr = preset.durationDays?.toString() ?: ""
        selectedSimSlot = preset.defaultSimSlot
        validationError = null
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MintPrimary,
        unfocusedBorderColor = DarkOutline,
        focusedLabelColor = MintPrimary,
        unfocusedLabelColor = TextMediumEmphasis,
        focusedTextColor = TextHighEmphasis,
        unfocusedTextColor = TextHighEmphasis,
        focusedContainerColor = SurfaceRecessed,
        unfocusedContainerColor = SurfaceRecessed,
        cursorColor = MintPrimary
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLayer1),
            border = BorderStroke(1.dp, BorderHighlight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure Promo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis
                )

                Text(
                    text = "Select a common Smart preset or enter custom promo details:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMediumEmphasis
                )

                // Quick Presets 2x2 Grid (FlowRow auto-reflow)
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMediumEmphasis
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    PromoPresets.ALL_PRESETS.forEach { preset ->
                        val isSelected = (selectedPresetTitle == preset.title)
                        Surface(
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .clickable { applyPreset(preset) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MintPrimaryContainer else SurfaceLayer2,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MintPrimary else DarkOutline
                            )
                        ) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MintOnPrimaryContainer else TextHighEmphasis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                            )
                        }
                    }
                }

                // Promo Name Field
                OutlinedTextField(
                    value = promoName,
                    onValueChange = {
                        promoName = it
                        selectedPresetTitle = null
                        validationError = null
                    },
                    label = { Text("Promo Name") },
                    placeholder = { Text("e.g. Smart Magic Data 399", color = TextLowEmphasis) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // Allowance input + Segmented unit switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = allowanceValueStr,
                        onValueChange = {
                            allowanceValueStr = it.filter { ch -> ch.isDigit() || ch == '.' }
                            validationError = null
                        },
                        label = { Text("Total Allowance") },
                        placeholder = { Text("24", color = TextLowEmphasis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f)
                    )

                    UnitSegmentToggle(
                        selectedUnit = selectedUnit,
                        onUnitSelected = { selectedUnit = it }
                    )
                }

                // Current remaining balance input (to derive starting usage offset)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = remainingValueStr,
                            onValueChange = {
                                remainingValueStr = it.filter { ch -> ch.isDigit() || ch == '.' }
                                validationError = null
                            },
                            label = { Text("Current remaining balance (optional)") },
                            placeholder = { Text("e.g. 20", color = TextLowEmphasis) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            modifier = Modifier.weight(1f)
                        )

                        UnitSegmentToggle(
                            selectedUnit = remainingUnit,
                            onUnitSelected = { remainingUnit = it }
                        )
                    }
                    Text(
                        text = "Check your current data balance via the Smart app or *123# and enter it here — we'll calculate how much you've already used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMediumEmphasis,
                        lineHeight = 18.sp
                    )
                }

                // No-expiry switch Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceLayer2,
                    border = BorderStroke(1.dp, DarkOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No Expiration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextHighEmphasis
                            )
                            Text(
                                text = "For data-cap-only promos (e.g. Magic Data)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMediumEmphasis
                            )
                        }
                        Switch(
                            checked = isNoExpiry,
                            onCheckedChange = {
                                isNoExpiry = it
                                validationError = null
                            },
                            thumbContent = if (isNoExpiry) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MintOnPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MintOnPrimary,
                                checkedTrackColor = MintPrimary,
                                uncheckedThumbColor = TextMediumEmphasis,
                                uncheckedTrackColor = SurfaceRecessed
                            )
                        )
                    }
                }

                // Duration Days (if expiring)
                if (!isNoExpiry) {
                    OutlinedTextField(
                        value = durationDaysStr,
                        onValueChange = {
                            durationDaysStr = it.filter { ch -> ch.isDigit() }
                            validationError = null
                        },
                        label = { Text("Validity Duration (Days)") },
                        placeholder = { Text("7", color = TextLowEmphasis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // SIM Slot Selector Cards (Side-by-side cards with SIM Phone Icons)
                Text(
                    text = "Assign to SIM Slot:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextHighEmphasis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SimSlotCard(
                        title = "SIM 1",
                        isSelected = (selectedSimSlot == SimSlot.SIM_1),
                        onClick = { selectedSimSlot = SimSlot.SIM_1 },
                        modifier = Modifier.weight(1f)
                    )
                    SimSlotCard(
                        title = "SIM 2",
                        isSelected = (selectedSimSlot == SimSlot.SIM_2),
                        onClick = { selectedSimSlot = SimSlot.SIM_2 },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Validation error
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = PaceCritical,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel", color = TextMediumEmphasis)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (promoName.isBlank()) {
                                validationError = "Promo name cannot be blank."
                                return@Button
                            }
                            val numAllowance = allowanceValueStr.toDoubleOrNull()
                            if (numAllowance == null || numAllowance <= 0.0) {
                                validationError = "Please enter a valid positive allowance."
                                return@Button
                            }
                            val allowanceBytes = (numAllowance * selectedUnit.multiplier).toLong()

                            // Compute offset from entered remaining balance
                            val initialOffsetBytes = if (remainingValueStr.isNotBlank()) {
                                val remainingNum = remainingValueStr.toDoubleOrNull()
                                if (remainingNum == null || remainingNum < 0.0) {
                                    validationError = "Current remaining balance cannot be negative."
                                    return@Button
                                }
                                val remainingBytes = (remainingNum * remainingUnit.multiplier).toLong()
                                if (remainingBytes > allowanceBytes) {
                                    validationError = "Remaining balance cannot exceed total allowance."
                                    return@Button
                                }
                                allowanceBytes - remainingBytes
                            } else {
                                0L
                            }

                            val now = System.currentTimeMillis()
                            val startTimestamp = if (initialOffsetBytes > 0L) {
                                now - (4L * 24L * 60L * 60L * 1000L)
                            } else {
                                now
                            }
                            val expirationTimestamp: Long? = if (!isNoExpiry) {
                                val days = durationDaysStr.toIntOrNull()
                                if (days == null || days <= 0) {
                                    validationError = "Please enter valid validity days (e.g. 7)."
                                    return@Button
                                }
                                startTimestamp + (days * 24L * 3_600_000L)
                            } else {
                                null
                            }

                            onSavePromo(
                                promoName.trim(),
                                allowanceBytes,
                                startTimestamp,
                                expirationTimestamp,
                                initialOffsetBytes,
                                selectedSimSlot,
                                setAsActive
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = MintGlow,
                            ambientColor = MintGlow
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintPrimary,
                            contentColor = MintOnPrimary
                        )
                    ) {
                        Text("Save Promo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Segmented toggle pills for GB/MB unit selection.
 */
@Composable
private fun UnitSegmentToggle(
    selectedUnit: DataUnit,
    onUnitSelected: (DataUnit) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceRecessed,
        border = BorderStroke(1.dp, DarkOutline)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DataUnit.entries.forEach { unit ->
                val isSelected = (selectedUnit == unit)
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) MintPrimary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onUnitSelected(unit) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unit.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) MintOnPrimary else TextMediumEmphasis
                    )
                }
            }
        }
    }
}

/**
 * Selectable card for SIM 1 / SIM 2 with SIM icon and custom radio indicator.
 */
@Composable
private fun SimSlotCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SurfaceLayer2 else SurfaceRecessed,
        border = BorderStroke(
            1.dp,
            if (isSelected) MintPrimary else DarkOutline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    tint = if (isSelected) MintPrimary else TextMediumEmphasis,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) TextHighEmphasis else TextMediumEmphasis
                )
            }

            // Custom radio circle
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        color = if (isSelected) MintPrimary else Color.Transparent,
                        shape = CircleShape
                    )
                    .then(
                        if (!isSelected) Modifier.background(
                            color = Color.Transparent,
                            shape = CircleShape
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MintOnPrimary, CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.5.dp, Color(0xFF6B7280)),
                        modifier = Modifier.size(18.dp)
                    ) {}
                }
            }
        }
    }
}
