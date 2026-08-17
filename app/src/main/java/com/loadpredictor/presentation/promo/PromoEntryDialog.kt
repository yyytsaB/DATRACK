package com.loadpredictor.presentation.promo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loadpredictor.domain.model.SimSlot

enum class DataUnit(val multiplier: Long, val label: String) {
    GB(1024L * 1024L * 1024L, "GB"),
    MB(1024L * 1024L, "MB")
}

/**
 * Material 3 Dialog for creating or customizing a mobile data promo.
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
        simSlot: SimSlot,
        isActive: Boolean
    ) -> Unit
) {
    var selectedPresetTitle by remember { mutableStateOf<String?>(null) }
    var promoName by remember { mutableStateOf("") }
    var allowanceValueStr by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(DataUnit.GB) }
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Select a common Smart preset or enter custom promo details:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Presets FlowRow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PromoPresets.ALL_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = (selectedPresetTitle == preset.title),
                            onClick = { applyPreset(preset) },
                            label = { Text(preset.title, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
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
                    placeholder = { Text("e.g. Smart Magic Data 399") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Allowance input + unit toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = allowanceValueStr,
                        onValueChange = {
                            allowanceValueStr = it.filter { ch -> ch.isDigit() || ch == '.' }
                            validationError = null
                        },
                        label = { Text("Allowance") },
                        placeholder = { Text("24") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DataUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = (selectedUnit == unit),
                                onClick = { selectedUnit = unit },
                                label = { Text(unit.label) }
                            )
                        }
                    }
                }

                // No-expiry switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No Expiration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "For data-cap-only promos (e.g. Magic Data)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isNoExpiry,
                            onCheckedChange = {
                                isNoExpiry = it
                                validationError = null
                            }
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
                        placeholder = { Text("7") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // SIM Slot Picker
                Text(
                    text = "Assign to SIM Slot:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedSimSlot == SimSlot.SIM_1),
                            onClick = { selectedSimSlot = SimSlot.SIM_1 }
                        )
                        Text("SIM 1", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedSimSlot == SimSlot.SIM_2),
                            onClick = { selectedSimSlot = SimSlot.SIM_2 }
                        )
                        Text("SIM 2", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Validation error
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (promoName.isBlank()) {
                                validationError = "Promo name cannot be blank."
                                return@Button
                            }
                            val num = allowanceValueStr.toDoubleOrNull()
                            if (num == null || num <= 0.0) {
                                validationError = "Please enter a valid positive allowance."
                                return@Button
                            }
                            val allowanceBytes = (num * selectedUnit.multiplier).toLong()

                            val now = System.currentTimeMillis()
                            val expirationTimestamp: Long? = if (!isNoExpiry) {
                                val days = durationDaysStr.toIntOrNull()
                                if (days == null || days <= 0) {
                                    validationError = "Please enter valid validity days (e.g. 7)."
                                    return@Button
                                }
                                now + (days * 24L * 3_600_000L)
                            } else {
                                null
                            }

                            onSavePromo(
                                promoName.trim(),
                                allowanceBytes,
                                now,
                                expirationTimestamp,
                                selectedSimSlot,
                                setAsActive
                            )
                        }
                    ) {
                        Text("Save Promo")
                    }
                }
            }
        }
    }
}
