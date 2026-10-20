package com.loadpredictor.presentation.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.loadpredictor.data.backup.AdvisoryService
import com.loadpredictor.data.local.AdvisoryPreferences
import com.loadpredictor.domain.model.*
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.*

@Composable fun AdvisoryControls(promo:Promo?=null) {
    val context=LocalContext.current;val prefs=remember {AdvisoryPreferences(context)}
    val state by prefs.flow.collectAsState(initial=com.loadpredictor.data.local.AdvisoryState())
    var open by remember {mutableStateOf(false)};var reading by remember {mutableStateOf("")}
    val lifecycle=LocalLifecycleOwner.current
    LaunchedEffect(state.settings,lifecycle) {lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        if(state.settings.dailyCap==null)return@repeatOnLifecycle
        do {
            reading=try {val at=System.currentTimeMillis();val bytes=AdvisoryService(context).today(at)
                "Today: ${DataFormatter.formatBytes(bytes)} / ${DataFormatter.formatBytes(state.settings.dailyCap ?: 0)} · checked ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(at))}"
            }catch(e:CancellationException){throw e}catch(e:Exception){e.message ?: "Today's usage unavailable"}
            delay(60_000)
        }while(isActive && state.settings.dailyCap!=null)
    }}
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        if(state.settings.dailyCap!=null)Text(reading,style=MaterialTheme.typography.bodyMedium)
        TextButton(onClick={open=true}){Text("Daily budget and expiry reminders")}
    }
    if(open)AdvisoryDialog(state.settings,promo,onDismiss={open=false})
}

@Composable private fun AdvisoryDialog(settings:AdvisorySettings,promo:Promo?,onDismiss:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var budget by remember {mutableStateOf(settings.dailyCap?.toBigDecimal()?.divide(java.math.BigDecimal(1048576))?.stripTrailingZeros()?.toPlainString() ?: "")}
    var notify by remember {mutableStateOf(settings.dailyNotify)};var expiry by remember {mutableStateOf(settings.expiryHours)}
    var status by remember {mutableStateOf("")};var busy by remember {mutableStateOf(false)}
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text("Budgets and reminders")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Advisory only. Counts device mobile traffic since local midnight across all SIMs. Original offsets and carrier corrections are excluded. Checks can be delayed by Android; data is never blocked.")
        OutlinedTextField(value=budget,onValueChange={budget=it},enabled=!busy,label={Text("Daily budget in MB (blank = off)")},modifier=Modifier.fillMaxWidth())
        Text("1 MB = 1,048,576 bytes; 1024 MB = 1 GB.")
        Row {Text("Notify when budget is reached",modifier=Modifier.weight(1f));Switch(checked=notify,onCheckedChange={notify=it},enabled=!busy)}
        if(promo?.expirationTimestamp!=null && !promo.isExpired(System.currentTimeMillis())) {
            val hours=(promo.expirationTimestamp-System.currentTimeMillis())/3600000.0
            if(hours>0 && promo.lastSyncTimestamp>0)Text("Optional pace guide: ${DataFormatter.formatBytes((promo.remainingBytes(promo.lastSyncDataUsedBytes)/(hours/24.0)).toLong())} per day until actual expiry. Choose your own budget; this is an estimate.")
        }
        Text("Actual-expiry reminder (applies to the selected active promo; no-expiry promos have none)")
        // One option per row keeps long accessibility text visible.
        listOf<Int?>(null,6,24,48).forEach {value->TextButton(enabled=!busy,onClick={expiry=value}){Text("${if(expiry==value)"✓ " else ""}${value?.let {"Within $it hours"} ?: "Off"}")}}
        Text("Already-reached events are consumed when settings are saved. Future daily crossings and future expiry windows may notify once. Notification permission and channel settings still apply.")
        if(status.isNotEmpty())Text(status)
    }},confirmButton={TextButton(enabled=!busy,onClick={scope.launch {
        busy=true
        try {
            val cap=if(budget.isBlank())null else (budget.toBigDecimalOrNull() ?: error("Enter a valid budget")).multiply(java.math.BigDecimal(1048576)).setScale(0,java.math.RoundingMode.HALF_UP).longValueExact()
            require(cap==null || cap>0){"Budget must be greater than zero"}
            AdvisoryService(context).save(AdvisorySettings(cap,notify && cap!=null,expiry),System.currentTimeMillis())
            com.loadpredictor.worker.WorkManagerScheduler.enqueueImmediateSync(context,"advisory_settings")
            onDismiss()
        }catch(e:CancellationException){throw e}catch(e:Exception){status=e.message ?: "Unable to save"}finally{busy=false}
    }}){Text("Save")}},dismissButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Cancel")}})
}
