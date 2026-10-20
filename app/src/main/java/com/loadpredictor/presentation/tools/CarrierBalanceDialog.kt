package com.loadpredictor.presentation.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.loadpredictor.data.backup.*
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable fun CarrierBalanceDialog(promo:Promo,onChanged:()->Unit,onDismiss:()->Unit) {
    val context=LocalContext.current;val service=remember {CarrierBalanceService(context)};val scope=rememberCoroutineScope()
    var amount by remember {mutableStateOf("")};var unit by remember {mutableStateOf("GB")}
    var minutes by remember {mutableStateOf("0")};var status by remember {mutableStateOf("")}
    var comparison by remember {mutableStateOf<CarrierComparison?>(null)};var busy by remember {mutableStateOf(false)}
    var history by remember {mutableStateOf<List<com.loadpredictor.data.local.entity.BalanceCorrectionEntity>>(emptyList())}
    var confirmUndo by remember {mutableStateOf(false)}
    LaunchedEffect(promo.id){history=service.history(promo.id)}
    fun run(action:suspend ()->Unit){scope.launch {busy=true;try {action()}catch(e:CancellationException){throw e}catch(e:Exception){status=e.message ?: "Unable to compare"}finally{busy=false}}}
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text("Check against carrier balance")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(promo.name)
        Text("Use the general/open-access pool from your carrier. Device measurements include all mobile traffic across SIMs; they can differ from carrier accounting. Enter a reading from the last 15 minutes.")
        OutlinedTextField(value=amount,onValueChange={amount=it;comparison=null},enabled=!busy,label={Text("Reported remaining ($unit)")},modifier=Modifier.fillMaxWidth())
        Row {listOf("MB","GB").forEach {label->TextButton(enabled=!busy,onClick={unit=label;comparison=null}) {Text(if(unit==label)"$label ✓" else label)}}}
        Text("MB/GB use 1024-based units, matching your saved allowance.")
        OutlinedTextField(value=minutes,onValueChange={minutes=it;comparison=null},enabled=!busy,label={Text("Reading age in minutes (0–15)")},modifier=Modifier.fillMaxWidth())
        OutlinedButton(enabled=!busy,onClick={run {
            val value=amount.toBigDecimalOrNull() ?: error("Enter a valid remaining amount")
            require(value.signum()>=0){"Balance cannot be negative"}
            val bytes=value.multiply(java.math.BigDecimal(if(unit=="GB")1073741824L else 1048576L)).setScale(0,java.math.RoundingMode.HALF_UP).longValueExact()
            val age=minutes.toLongOrNull() ?: error("Enter an age between 0 and 15 minutes")
            require(age in 0..15){"Reading age must be 0–15 minutes"}
            val now=System.currentTimeMillis();comparison=service.compare(promo.id,bytes,now-age*60_000,now)
            status="Comparison only. Nothing has been changed."
        }}){Text("Compare without changes")}
        comparison?.let {c->
            Text("App at reading time: ${DataFormatter.formatBytes(c.appRemaining)}\nCarrier: ${DataFormatter.formatBytes(c.reported)}\nDifference: ${if(c.reported>=c.appRemaining)"+" else "−"}${DataFormatter.formatBytes(kotlin.math.abs(c.reported-c.appRemaining))}")
            Text("Use this reading changes only the balance baseline. Your original offset and daily measured usage stay. Already-crossed alerts are consumed once; later crossings can notify again.")
            Button(enabled=!busy,onClick={run {service.apply(c,System.currentTimeMillis());onChanged();onDismiss()}}){Text("Use this reading")}
        }
        if(promo.correctionRemaining!=null) {
            if(confirmUndo) {
                Text("Undo the latest correction and return to the previous balance baseline?")
                TextButton(enabled=!busy,onClick={run {service.undo(promo.id,promo.correctionRevision);onChanged();onDismiss()}}){Text("Confirm undo")}
            } else TextButton(enabled=!busy,onClick={confirmUndo=true}){Text("Undo latest correction")}
        }
        if(history.isNotEmpty()) {
            Text("Correction history",style=MaterialTheme.typography.titleSmall)
            history.take(30).forEach {c->Text("${Instant.ofEpochMilli(c.observedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))}: ${DataFormatter.formatBytes(c.remaining)}${if(c.undone)" · undone" else ""}")}
            if(history.size>30)Text("Most recent 30 shown. Full history is included in configuration backups.")
        }
        if(status.isNotEmpty())Text(status)
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    }},confirmButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Close")}})
}
