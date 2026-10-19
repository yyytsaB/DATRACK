package com.loadpredictor.presentation.promo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import java.text.DateFormat
import java.util.Calendar

enum class DataUnit(val multiplier: Long, val label: String) {
    GB(1024L * 1024L * 1024L, "GB"), MB(1024L * 1024L, "MB")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PromoEntryDialog(
    onDismissRequest: () -> Unit,
    promoToEdit: Promo? = null,
    saveError: String? = null,
    isSaving: Boolean = false,
    onSavePromo: (Long, String, Long, Long, Long?, Long, SimSlot, Boolean, Promo?) -> Unit
) {
    val context = LocalContext.current
    val id = promoToEdit?.id ?: 0L
    var name by rememberSaveable(id) { mutableStateOf(promoToEdit?.name ?: "My promo") }
    var amount by rememberSaveable(id) { mutableStateOf(promoToEdit?.let { (it.totalAllowanceBytes / 1073741824.0).toString() } ?: "2") }
    var used by rememberSaveable(id) { mutableStateOf(promoToEdit?.let { (it.initialUsageOffsetBytes / 1073741824.0).toString() } ?: "0") }
    var noExpiry by rememberSaveable(id) { mutableStateOf(promoToEdit?.isNoExpiry ?: false) }
    var expiry by rememberSaveable(id) { mutableLongStateOf(promoToEdit?.expirationTimestamp ?: (System.currentTimeMillis() + 7 * 86400000L)) }
    var sim by rememberSaveable(id) { mutableStateOf(promoToEdit?.simSlot ?: SimSlot.SIM_1) }
    var error by rememberSaveable(id) { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (promoToEdit == null) "Add your promo" else "Edit promo", style = MaterialTheme.typography.headlineSmall)
            Text("Track your general data allowance. Carrier-specific bonus and unlimited app data are not separated.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (promoToEdit == null) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PromoPresets.ALL_PRESETS.forEach { preset ->
                    SuggestionChip(onClick = {
                        name = preset.title
                        amount = (preset.allowanceBytes / 1073741824.0).toString()
                        noExpiry = preset.durationDays == null
                        expiry = System.currentTimeMillis() + (preset.durationDays ?: 7) * 86400000L
                    }, label = { Text(preset.title) })
                }
            }
            OutlinedTextField(name, { name = it; error = null }, label = { Text("Promo name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(amount, { amount = it; error = null }, label = { Text("Total allowance (GB)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(used, { used = it; error = null }, label = { Text("Already used before tracking (GB)") },
                supportingText = { Text("Exclude data measured since you started tracking this promo.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("No expiry", Modifier.weight(1f).padding(top = 12.dp))
                Switch(noExpiry, { noExpiry = it })
            }
            if (!noExpiry) {
                Text("Actual promo expiry", style = MaterialTheme.typography.titleMedium)
                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(java.util.Date(expiry)))
                OutlinedButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = expiry }
                    DatePickerDialog(context, { _, year, month, day ->
                        cal.set(year, month, day)
                        TimePickerDialog(context, { _, hour, minute ->
                            cal.set(Calendar.HOUR_OF_DAY, hour); cal.set(Calendar.MINUTE, minute)
                            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                            expiry = cal.timeInMillis
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }) { Text("Choose date and time") }
                Text("Already subscribed? Enter when your carrier says the promo ends, not its original duration.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Manual SIM context", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimSlot.entries.forEach { slot -> FilterChip(selected = sim == slot, onClick = { sim = slot },
                    label = { Text(if (slot == SimSlot.SIM_1) "SIM 1" else "SIM 2") }) }
            }
            Text("Android reports total mobile usage across SIMs. This label does not switch your phone's mobile-data SIM.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            (error ?: saveError)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(enabled = !isSaving, modifier = Modifier.fillMaxWidth(), onClick = {
                val total = amount.toDoubleOrNull()
                val offset = used.toDoubleOrNull()
                val now = System.currentTimeMillis()
                val start = promoToEdit?.startTimestamp ?: now
                when {
                    name.isBlank() -> error = "Enter a promo name."
                    total == null || !total.isFinite() || total <= 0 || total > Long.MAX_VALUE / 1073741824.0 -> error = "Enter a valid positive allowance."
                    offset == null || !offset.isFinite() || offset < 0 || offset > total -> error = "Already used must be between zero and the total allowance."
                    !noExpiry && expiry <= start -> error = "Expiry must be after tracking started."
                    else -> onSavePromo(id, name.trim(), (total * 1073741824).toLong(), start,
                        if (noExpiry) null else expiry, (offset * 1073741824).toLong(), sim,
                        promoToEdit?.isActive ?: true, promoToEdit)
                }
            }) { Text(if (isSaving) "Saving…" else if (promoToEdit == null) "Start tracking" else "Save changes") }
        }
    }
}
