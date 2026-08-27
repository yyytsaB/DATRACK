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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.MintOnPrimary
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
 * 1-to-1 exact replica of the Add/Configure Promo Modal Bottom Sheet matching preview (4).webp:
 * - Elevated Bottom Sheet with dark surface (#181F2C) & top rounded 28dp corners
 * - Drag handle indicator
 * - Quick presets pill grid (GigaSurf 99, GigaSurf 149, Giga Video 50, Magic Data, GoSURF 50, Custom)
 * - Allowance rounded card field
 * - Validity rounded card field (with No Expiration option)
 * - Already used (optional) rounded card field
 * - SIM Slot selector
 * - Electric Mint full-width "Save Promo" pill action button
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PromoEntryDialog(
    onDismissRequest: () -> Unit,
    promoToEdit: com.loadpredictor.domain.model.Promo? = null,
    onSavePromo: (
        id: Long,
        name: String,
        allowanceBytes: Long,
        startTimestamp: Long,
        expirationTimestamp: Long?,
        initialUsageOffsetBytes: Long,
        simSlot: SimSlot,
        isActive: Boolean,
        existingPromo: com.loadpredictor.domain.model.Promo?
    ) -> Unit
) {
    val isEditMode = promoToEdit != null
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val matchingPreset = remember(promoToEdit) {
        promoToEdit?.let { promo ->
            PromoPresets.ALL_PRESETS.firstOrNull { it.title.equals(promo.name, ignoreCase = true) }
        }
    }

    val initialAllowancePair = remember(promoToEdit) {
        if (promoToEdit != null) {
            val gb = promoToEdit.totalAllowanceBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            if (gb >= 1.0) {
                val str = if (gb == gb.toLong().toDouble()) gb.toLong().toString() else "%.2f".format(gb).trimEnd('0').trimEnd('.')
                Pair(str, DataUnit.GB)
            } else {
                val mb = promoToEdit.totalAllowanceBytes / (1024 * 1024)
                Pair(mb.toString(), DataUnit.MB)
            }
        } else {
            Pair("2", DataUnit.GB)
        }
    }

    val initialDurationDays = remember(promoToEdit) {
        if (promoToEdit?.expirationTimestamp != null) {
            val days = maxOf(1L, (promoToEdit.expirationTimestamp - promoToEdit.startTimestamp) / (24L * 3_600_000L))
            days.toString()
        } else {
            "7"
        }
    }

    var selectedPresetTitle by remember {
        mutableStateOf<String?>(if (isEditMode) matchingPreset?.title else "GigaSurf 99")
    }
    var promoName by remember {
        mutableStateOf(promoToEdit?.name ?: "GigaSurf 99")
    }
    var allowanceValueStr by remember { mutableStateOf(initialAllowancePair.first) }
    var selectedUnit by remember { mutableStateOf(initialAllowancePair.second) }
    var usedValueStr by remember { mutableStateOf("") }
    var usedUnit by remember { mutableStateOf(DataUnit.MB) }
    var isNoExpiry by remember {
        mutableStateOf(promoToEdit?.isNoExpiry ?: false)
    }
    var durationDaysStr by remember { mutableStateOf(initialDurationDays) }
    var selectedSimSlot by remember {
        mutableStateOf(promoToEdit?.simSlot ?: SimSlot.SIM_1)
    }
    var setAsActive by remember {
        mutableStateOf(promoToEdit?.isActive ?: true)
    }
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

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF161C28),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color(0xFF374151), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Title
            Text(
                text = if (isEditMode) "Edit Promo" else "Add Promo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis
            )

            // Section 1: Quick Presets
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick presets",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMediumEmphasis
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PromoPresets.ALL_PRESETS.forEach { preset ->
                        val isSelected = (selectedPresetTitle == preset.title)
                        PresetPillChip(
                            label = preset.title,
                            isSelected = isSelected,
                            onClick = { applyPreset(preset) }
                        )
                    }

                    // Custom preset chip
                    val isCustomSelected = (selectedPresetTitle == null)
                    PresetPillChip(
                        label = "Custom",
                        isSelected = isCustomSelected,
                        onClick = {
                            selectedPresetTitle = null
                            if (!isEditMode) promoName = ""
                            validationError = null
                        }
                    )
                }
            }

            // Custom Promo Name Field (if Custom selected or name differs from preset)
            if (selectedPresetTitle == null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Promo Name",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMediumEmphasis
                    )
                    StyledInputFieldBox(
                        value = promoName,
                        onValueChange = {
                            promoName = it
                            validationError = null
                        },
                        placeholder = "e.g. Smart Magic Data 399",
                        keyboardType = KeyboardType.Text
                    )
                }
            }

            // Section 2: Allowance
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Allowance",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMediumEmphasis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StyledInputFieldBox(
                            value = allowanceValueStr,
                            onValueChange = {
                                allowanceValueStr = it.filter { ch -> ch.isDigit() || ch == '.' }
                                validationError = null
                            },
                            placeholder = "8",
                            keyboardType = KeyboardType.Decimal
                        )
                    }

                    UnitSegmentPills(
                        selectedUnit = selectedUnit,
                        onUnitSelected = { selectedUnit = it }
                    )
                }
            }

            // Section 3: Validity
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Validity",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMediumEmphasis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No Expiration",
                            fontSize = 11.sp,
                            color = if (isNoExpiry) MintPrimary else TextLowEmphasis,
                            fontWeight = if (isNoExpiry) FontWeight.Bold else FontWeight.Normal
                        )
                        Switch(
                            checked = isNoExpiry,
                            onCheckedChange = {
                                isNoExpiry = it
                                validationError = null
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MintOnPrimary,
                                checkedTrackColor = MintPrimary,
                                uncheckedThumbColor = TextMediumEmphasis,
                                uncheckedTrackColor = SurfaceRecessed
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                if (!isNoExpiry) {
                    StyledInputFieldBox(
                        value = durationDaysStr,
                        onValueChange = {
                            durationDaysStr = it.filter { ch -> ch.isDigit() }
                            validationError = null
                        },
                        placeholder = "7",
                        suffix = if (isEditMode) "days total validity" else "days from today",
                        keyboardType = KeyboardType.Number
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E2636),
                        border = BorderStroke(1.dp, Color(0xFF2B364A))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Non-expiring / Data cap only",
                                fontSize = 14.sp,
                                color = MintPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Section 4: Already used (only for creation / mid-cycle setup)
            if (!isEditMode) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Already used (optional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMediumEmphasis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            StyledInputFieldBox(
                                value = usedValueStr,
                                onValueChange = {
                                    usedValueStr = it.filter { ch -> ch.isDigit() || ch == '.' }
                                    validationError = null
                                },
                                placeholder = "0 — for mid-cycle setup",
                                keyboardType = KeyboardType.Decimal
                            )
                        }

                        UnitSegmentPills(
                            selectedUnit = usedUnit,
                            onUnitSelected = { usedUnit = it }
                        )
                    }
                }
            }

            // Section 5: SIM Slot Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SIM Slot",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMediumEmphasis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SimSlotPillButton(
                        title = "SIM 1",
                        isSelected = (selectedSimSlot == SimSlot.SIM_1),
                        onClick = { selectedSimSlot = SimSlot.SIM_1 },
                        modifier = Modifier.weight(1f)
                    )
                    SimSlotPillButton(
                        title = "SIM 2",
                        isSelected = (selectedSimSlot == SimSlot.SIM_2),
                        onClick = { selectedSimSlot = SimSlot.SIM_2 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Validation Error Message
            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = PaceCritical,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Section 6: Save Promo Action Button
            Button(
                onClick = {
                    val finalPromoName = promoName.ifBlank { selectedPresetTitle ?: "My Promo" }
                    val numAllowance = allowanceValueStr.toDoubleOrNull()
                    if (numAllowance == null || numAllowance <= 0.0) {
                        validationError = "Please enter a valid positive allowance."
                        return@Button
                    }
                    val allowanceBytes = (numAllowance * selectedUnit.multiplier).toLong()

                    val usedBytes = if (!isEditMode) {
                        if (usedValueStr.isNotBlank()) {
                            val numUsed = usedValueStr.toDoubleOrNull()
                            if (numUsed == null || numUsed < 0.0) {
                                validationError = "Used data cannot be negative."
                                return@Button
                            }
                            val calcUsedBytes = (numUsed * usedUnit.multiplier).toLong()
                            if (calcUsedBytes >= allowanceBytes) {
                                validationError = "Used amount must be less than total allowance."
                                return@Button
                            }
                            calcUsedBytes
                        } else {
                            0L
                        }
                    } else {
                        promoToEdit!!.initialUsageOffsetBytes
                    }

                    if (isEditMode) {
                        val startTimestamp = promoToEdit!!.startTimestamp
                        val expirationTimestamp: Long? = if (!isNoExpiry) {
                            val days = durationDaysStr.toIntOrNull()
                            if (days == null || days <= 0) {
                                validationError = "Please enter valid duration days (e.g. 7)."
                                return@Button
                            }
                            startTimestamp + (days * 24L * 3_600_000L)
                        } else {
                            null
                        }

                        onSavePromo(
                            promoToEdit.id,
                            finalPromoName.trim(),
                            allowanceBytes,
                            startTimestamp,
                            expirationTimestamp,
                            usedBytes,
                            selectedSimSlot,
                            setAsActive,
                            promoToEdit
                        )
                    } else {
                        val now = System.currentTimeMillis()
                        val startTimestamp = now
                        val expirationTimestamp: Long? = if (!isNoExpiry) {
                            val days = durationDaysStr.toIntOrNull()
                            if (days == null || days <= 0) {
                                validationError = "Please enter valid duration days (e.g. 7)."
                                return@Button
                            }
                            startTimestamp + (days * 24L * 3_600_000L)
                        } else {
                            null
                        }

                        onSavePromo(
                            0L,
                            finalPromoName.trim(),
                            allowanceBytes,
                            startTimestamp,
                            expirationTimestamp,
                            usedBytes,
                            selectedSimSlot,
                            setAsActive,
                            null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintPrimary,
                    contentColor = MintOnPrimary
                )
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "Save Promo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintOnPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Pill Chip for Quick Presets matching preview (4).webp.
 */
@Composable
private fun PresetPillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MintPrimaryContainer else Color(0xFF222B3B),
        border = BorderStroke(
            1.dp,
            if (isSelected) MintPrimary else Color(0xFF2E394C)
        )
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MintPrimary else Color(0xFFD1D5DB),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * Styled rounded text input box matching reference inputs.
 */
@Composable
private fun StyledInputFieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E2636),
        border = BorderStroke(1.dp, Color(0xFF2B364A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7A90)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextHighEmphasis
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    cursorBrush = SolidColor(MintPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (suffix != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = suffix,
                    fontSize = 13.sp,
                    color = TextMediumEmphasis
                )
            }
        }
    }
}

/**
 * Segmented Unit Toggle (GB / MB).
 */
@Composable
private fun UnitSegmentPills(
    selectedUnit: DataUnit,
    onUnitSelected: (DataUnit) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E2636),
        border = BorderStroke(1.dp, Color(0xFF2B364A))
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DataUnit.entries.forEach { unit ->
                val isSelected = (selectedUnit == unit)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MintPrimary else Color.Transparent)
                        .clickable { onUnitSelected(unit) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
 * SIM Slot pill selector button.
 */
@Composable
private fun SimSlotPillButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MintPrimaryContainer else Color(0xFF1E2636),
        border = BorderStroke(
            1.dp,
            if (isSelected) MintPrimary else Color(0xFF2B364A)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isSelected) MintPrimary else Color(0xFF4B5563),
                            shape = CircleShape
                        )
                )
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MintPrimary else TextHighEmphasis
                )
            }
        }
    }
}
