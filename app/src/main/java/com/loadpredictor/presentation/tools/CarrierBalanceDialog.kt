package com.loadpredictor.presentation.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loadpredictor.data.backup.*
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.presentation.theme.*
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable fun CarrierBalanceDialog(promo:Promo,onChanged:()->Unit,onDismiss:()->Unit) {
    val context=LocalContext.current;val service=remember {CarrierBalanceService(context)};val scope=rememberCoroutineScope()
    var amount by remember {mutableStateOf("")};var unit by remember {mutableStateOf("GB")}
    var minutes by remember {mutableStateOf("0")};var status by remember {mutableStateOf("")}
    var hasError by remember {mutableStateOf(false)}
    var comparison by remember {mutableStateOf<CarrierComparison?>(null)};var busy by remember {mutableStateOf(false)}
    var history by remember {mutableStateOf<List<com.loadpredictor.data.local.entity.BalanceCorrectionEntity>>(emptyList())}
    var showHistory by remember {mutableStateOf(false)};var confirmUndo by remember {mutableStateOf(false)}
    LaunchedEffect(promo.id){try {history=service.history(promo.id)}catch(e:CancellationException){throw e}catch(e:Exception){status=e.message ?: "History unavailable";hasError=true}}
    fun run(action:suspend ()->Unit){scope.launch {busy=true;hasError=false;status="";try {action()}catch(e:CancellationException){throw e}catch(e:Exception){status=e.message ?: "Unable to compare";hasError=true}finally{busy=false}}}
    ToolSheet(title="Carrier balance",subtitle=promo.name,busy=busy,onDismiss=onDismiss,feedback=status,feedbackError=hasError,footer={
        if(confirmUndo) {
            ToolPrimary("Undo correction",enabled=!busy,danger=true,onClick={run {service.undo(promo.id,promo.correctionRevision);onChanged();onDismiss()}})
            ToolSecondary("Keep correction",enabled=!busy,onClick={confirmUndo=false})
        } else comparison?.let {c->
            ToolPrimary("Use this reading",enabled=!busy,onClick={run {service.apply(c,System.currentTimeMillis());onChanged();onDismiss()}})
        } ?: ToolPrimary("Compare balance",enabled=!busy,onClick={run {
            val value=amount.toBigDecimalOrNull() ?: error("Enter a valid remaining amount")
            require(value.signum()>=0){"Balance cannot be negative"}
            val bytes=value.multiply(java.math.BigDecimal(if(unit=="GB")1073741824L else 1048576L)).setScale(0,java.math.RoundingMode.HALF_UP).longValueExact()
            val age=minutes.toLongOrNull() ?: error("Enter an age between 0 and 15 minutes")
            require(age in 0..15){"Reading age must be 0–15 minutes"}
            val now=System.currentTimeMillis();comparison=service.compare(promo.id,bytes,now-age*60_000,now)
            status="Comparison only. Nothing has been changed."
        }})
    }) {
        if(confirmUndo)ToolNotice("Undo the latest correction and return to the previous balance baseline? Your correction history is retained.")
        val c=comparison
        if(c==null) {
            ToolSection("Your carrier reading","Enter the remaining general/open-access data from your carrier.") {
                OutlinedTextField(colors=toolFieldColors(),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),value=amount,onValueChange={amount=it;comparison=null},enabled=!busy,
                    label={Text("Remaining data")},suffix={Text(unit)},singleLine=true,
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
                ToolChoices(listOf("MB","GB"),unit,enabled=!busy,onSelect={unit=it;comparison=null})
                OutlinedTextField(colors=toolFieldColors(),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),value=minutes,onValueChange={minutes=it;comparison=null},enabled=!busy,
                    label={Text("Reading age")},suffix={Text("min")},supportingText={Text("0 = just now. Readings must be from the last 15 minutes.")},
                    singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
            }
        } else {
            ToolSection("Review the comparison","At ${Instant.ofEpochMilli(c.observedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))}") {
                ToolValue("App estimate",DataFormatter.formatBytes(c.appRemaining))
                ToolValue("Carrier reading",DataFormatter.formatBytes(c.reported),emphasize=true)
                HorizontalDivider(color=DarkOutline)
                ToolValue("Difference","${if(c.reported>=c.appRemaining)"+" else "−"}${DataFormatter.formatBytes(kotlin.math.abs(c.reported-c.appRemaining))}")
                Text("Applying this reading changes your balance baseline. Your original offset and daily measured usage stay.",style=MaterialTheme.typography.bodyMedium,color=TextMediumEmphasis)
                ToolSecondary("Edit reading",enabled=!busy,onClick={comparison=null;status=""})
            }
        }
        ToolHelp("How carrier corrections work","Device measurements include all mobile traffic across SIMs and may differ from carrier accounting. MB/GB use 1024-based units, matching your saved allowance. Use the general/open-access pool only. Already-crossed alerts are consumed once after a correction; later crossings can notify again.")
        if(promo.correctionRemaining!=null && !confirmUndo)ToolSecondary("Undo latest correction",enabled=!busy,onClick={confirmUndo=true})
        if(history.isNotEmpty()) {
            ToolActionRow("Correction history (${history.size})",icon=Icons.Default.History,onClick={showHistory=!showHistory})
            if(showHistory)Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                history.take(30).forEach {record->ToolValue(
                    "${Instant.ofEpochMilli(record.observedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))}${if(record.undone)" · undone" else ""}",DataFormatter.formatBytes(record.remaining))}
                if(history.size>30)Text("Most recent 30 shown. Full history is included in configuration backups.",style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)
            }
        }
    }
}
