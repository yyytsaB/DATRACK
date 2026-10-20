package com.loadpredictor.presentation.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.loadpredictor.data.backup.*
import com.loadpredictor.data.stats.*
import com.loadpredictor.domain.model.*
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.*
import java.time.*

@Composable fun DiaryDialog(promo:Promo,onDismiss:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val today=remember {LocalDate.now()}
    var from by remember {mutableStateOf(today.minusDays(6).toString())};var through by remember {mutableStateOf(today.toString())}
    var snapshot by remember {mutableStateOf<DiarySnapshot?>(null)};var apps by remember {mutableStateOf<List<MobileUidUsage>?>(null)}
    var status by remember {mutableStateOf("Select dates and load measured mobile usage. Dates are clipped to the promo's tracking period; at most 90 recent days are available.")}
    var busy by remember {mutableStateOf(false)};var attributionStatus by remember {mutableStateOf("")}
    var exportCsv by androidx.compose.runtime.saveable.rememberSaveable {mutableStateOf<String?>(null)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->
        val saved=exportCsv
        if(uri!=null && saved!=null)scope.launch {
            busy=true
            try {withContext(Dispatchers.IO){requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).use {it.write(saved.toByteArray(Charsets.UTF_8))}};status="CSV saved. It contains measured traffic, not carrier balances or claimed savings."}
            catch(e:CancellationException){throw e}catch(e:Exception){status=e.message ?: "Export failed"}finally{busy=false;exportCsv=null}
        }
    }
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text("Data Diary and app usage")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(promo.name);Text(status)
        OutlinedTextField(value=from,onValueChange={from=it;snapshot=null;apps=null},enabled=!busy,label={Text("From (YYYY-MM-DD)")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(value=through,onValueChange={through=it;snapshot=null;apps=null},enabled=!busy,label={Text("Through (YYYY-MM-DD)")},modifier=Modifier.fillMaxWidth())
        OutlinedButton(enabled=!busy,onClick={scope.launch {
            busy=true;snapshot=null;apps=null;attributionStatus=""
            try {snapshot=DiaryService(context).load(promo.id,LocalDate.parse(from),LocalDate.parse(through),System.currentTimeMillis(),ZoneId.systemDefault());status="Loaded measured device-total mobile traffic. Android statistics may lag or change; zero does not prove complete carrier accounting."}
            catch(e:CancellationException){throw e}catch(e:Exception){status=e.message ?: "Usage unavailable"}finally{busy=false}
        }}){Text("Load selected dates")}
        snapshot?.let {s->
            Text("${s.buckets.size} day(s) · ${DataFormatter.formatBytes(s.buckets.sumOf {it.totalBytes})} · ${s.window.zone.id}")
            Text("Measured ${java.time.Instant.ofEpochMilli(s.window.start)} to ${java.time.Instant.ofEpochMilli(s.window.end)}. Offsets and carrier corrections are excluded. Partial days are labeled in CSV.")
            OutlinedButton(enabled=!busy,onClick={exportCsv=s.csv();export.launch("DATRACK-diary-$from-to-$through.csv")}){Text("Export this snapshot as CSV")}
            OutlinedButton(enabled=!busy,onClick={scope.launch {
                busy=true;apps=null
                try {apps=MobileAppUsageSource(context).query(s.window.start,s.window.end);attributionStatus="Mobile-only · same date range · checked now"}
                catch(e:CancellationException){throw e}catch(e:Exception){attributionStatus=e.message ?: "App usage unavailable"}finally{busy=false}
            }}){Text("Load mobile usage by app")}
            if(attributionStatus.isNotEmpty())Text(attributionStatus)
            apps?.let {rows->
                Text("Grouped by Android UID, including shared UIDs. App names may be unavailable. These totals can differ from device totals and carrier billing.")
                val device=s.buckets.sumOf {it.totalBytes};val attributed=rows.fold(0L){total,row->if(Long.MAX_VALUE-total<row.bytes)Long.MAX_VALUE else total+row.bytes}
                Text("Device total: ${DataFormatter.formatBytes(device)} · UID total: ${DataFormatter.formatBytes(attributed)}")
                if(device>attributed)Text("Unattributed difference: ${DataFormatter.formatBytes(device-attributed)}")
                if(attributed>device)Text("UID totals exceed device totals; Android attribution and delayed statistics may differ. Values are not forced to match.")
                if(rows.isEmpty())Text("No UID-attributed mobile usage returned for this range.")
                rows.forEach {row->
                    HorizontalDivider()
                    Text(row.label,style=MaterialTheme.typography.titleSmall)
                    Text("${DataFormatter.formatBytes(row.bytes)} · UID ${row.uid}${if(device>0)" · ${java.lang.String.format(java.util.Locale.getDefault(),"%.1f",row.bytes.toDouble()/device*100)}% of device total" else ""}")
                }
            }
        }
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    }},confirmButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Close")}})
}
