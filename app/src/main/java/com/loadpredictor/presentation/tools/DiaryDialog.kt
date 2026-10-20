package com.loadpredictor.presentation.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.loadpredictor.data.backup.*
import com.loadpredictor.data.stats.*
import com.loadpredictor.domain.model.*
import com.loadpredictor.presentation.theme.*
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun DiaryDialog(promo:Promo,onDismiss:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val today=remember {LocalDate.now()}
    var from by remember {mutableStateOf(today.minusDays(6).toString())};var through by remember {mutableStateOf(today.toString())}
    var snapshot by remember {mutableStateOf<DiarySnapshot?>(null)};var apps by remember {mutableStateOf<List<MobileUidUsage>?>(null)}
    var status by remember {mutableStateOf("")};var hasError by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)};var attributionStatus by remember {mutableStateOf("")}
    var editDates by remember {mutableStateOf(true)};var tab by remember {mutableStateOf("Daily usage")}
    var exportCsv by androidx.compose.runtime.saveable.rememberSaveable {mutableStateOf<String?>(null)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->
        val saved=exportCsv
        if(uri!=null && saved!=null)scope.launch {
            busy=true;hasError=false
            try {withContext(Dispatchers.IO){requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).use {it.write(saved.toByteArray(Charsets.UTF_8))}};status="CSV saved. Includes measured traffic and partial-day labels."}
            catch(e:CancellationException){throw e}catch(e:Exception){status=e.message ?: "Export failed";hasError=true}finally{busy=false;exportCsv=null}
        }
    }
    fun loadApps(s:DiarySnapshot) {scope.launch {
        busy=true;apps=null;hasError=false;status=""
        try {apps=MobileAppUsageSource(context).query(s.window.start,s.window.end);attributionStatus="Checked ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date())}"}
        catch(e:CancellationException){throw e}catch(e:Exception){status=e.message ?: "App usage unavailable";hasError=true}finally{busy=false}
    }}
    ToolSheet(title="Data Diary",subtitle=promo.name,busy=busy,onDismiss=onDismiss,scrollable=false,feedback=status,feedbackError=hasError,footer={
        val saved=snapshot
        if(editDates || saved==null)ToolPrimary("Load usage",enabled=!busy,onClick={scope.launch {
            busy=true;snapshot=null;apps=null;attributionStatus="";status="";hasError=false
            try {snapshot=DiaryService(context).load(promo.id,LocalDate.parse(from),LocalDate.parse(through),System.currentTimeMillis(),ZoneId.systemDefault());editDates=false;tab="Daily usage"}
            catch(e:CancellationException){throw e}catch(e:Exception){status=if(e is java.time.format.DateTimeParseException)"Enter dates as YYYY-MM-DD, for example $today." else e.message ?: "Usage unavailable";hasError=true}finally{busy=false}
        }}) else ToolPrimary("Export daily CSV",enabled=!busy,onClick={exportCsv=saved.csv();export.launch("DATRACK-diary-$from-to-$through.csv")})
    }) {
        LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item {
                if(editDates)ToolSection("Date range","Up to 90 recent days, within the promo's tracking period.") {
                    ToolChoices(listOf("7 days","30 days"),"",enabled=!busy,onSelect={choice->from=today.minusDays(if(choice=="7 days")6 else 29).toString();through=today.toString();snapshot=null;apps=null})
                    OutlinedTextField(colors=toolFieldColors(),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),value=from,onValueChange={from=it;snapshot=null;apps=null},enabled=!busy,
                        label={Text("From")},placeholder={Text("YYYY-MM-DD")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(colors=toolFieldColors(),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),value=through,onValueChange={through=it;snapshot=null;apps=null},enabled=!busy,
                        label={Text("Through")},placeholder={Text("YYYY-MM-DD")},singleLine=true,modifier=Modifier.fillMaxWidth())
                } else {
                    val dateFormat=DateTimeFormatter.ofPattern("MMM d, yyyy")
                    ToolActionRow("${LocalDate.parse(from).format(dateFormat)} – ${LocalDate.parse(through).format(dateFormat)}",
                        description="Change dates",enabled=!busy,onClick={editDates=true})
                }
            }
            val s=snapshot
            if(s!=null && !editDates) {
                val device=s.buckets.sumOf {it.totalBytes}
                item {
                    ToolValue("Total mobile usage",DataFormatter.formatBytes(device),emphasize=true)
                    Text("${s.buckets.size} day(s) · ${s.window.zone.id} · measured ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(s.observedAt))}",style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)
                }
                item {ToolChoices(listOf("Daily usage","By app"),tab,enabled=!busy,onSelect={value->tab=value;if(value=="By app" && apps==null)loadApps(s)})}
                if(tab=="Daily usage") {
                    val peak=s.buckets.maxOfOrNull {it.totalBytes} ?: 0
                    items(s.buckets,key={it.startTimestamp}) {bucket->
                        val date=Instant.ofEpochMilli(bucket.startTimestamp).atZone(s.window.zone).format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                        UsageRow(date,DataFormatter.formatBytes(bucket.totalBytes),if(peak>0)bucket.totalBytes.toFloat()/peak else 0f)
                    }
                } else {
                    val rows=apps
                    if(rows!=null) {
                        val attributed=rows.fold(0L){total,row->if(Long.MAX_VALUE-total<row.bytes)Long.MAX_VALUE else total+row.bytes}
                        item {
                            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                ToolValue("App-attributed total",DataFormatter.formatBytes(attributed))
                                if(device>attributed)Text("${DataFormatter.formatBytes(device-attributed)} unattributed difference",style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)
                                if(attributed>device)Text("App totals exceed the device total. Android attribution and delayed readings may differ.",style=MaterialTheme.typography.bodyMedium,color=TextMediumEmphasis)
                                Text(attributionStatus,style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)
                            }
                        }
                        if(rows.isEmpty())item {ToolNotice("No app-attributed mobile usage returned for this range. Android statistics may be delayed.")}
                        items(rows,key={it.uid}) {row->
                            val ratio=if(device>0)row.bytes.toDouble()/device else 0.0
                            UsageRow(row.label,DataFormatter.formatBytes(row.bytes),ratio.toFloat(),
                                "${if(device>0)java.lang.String.format(java.util.Locale.getDefault(),"%.1f%% of device total · ",ratio*100) else ""}UID ${row.uid}")
                        }
                    }
                }
                item {ToolHelp("About these measurements","Mobile-only traffic across SIMs; offsets and carrier corrections are excluded. Android may delay or revise statistics. CSV exports exact bytes, source, scope, timezone and partial-day labels. It contains measured traffic, not carrier balances or claimed savings. App usage is grouped by Android UID, including shared UIDs. Some names may be unavailable. Device and app totals can differ from one another and from carrier billing. Measured window: ${Instant.ofEpochMilli(s.window.start)} to ${Instant.ofEpochMilli(s.window.end)}.")}
            } else item {ToolNotice("Measured mobile usage only. Original usage offsets and carrier corrections are excluded.")}
        }
    }
}

@Composable private fun UsageRow(label:String,value:String,fraction:Float,detail:String?=null) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        ToolValue(label,value)
        detail?.let {Text(it,style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)}
        Box(Modifier.fillMaxWidth().height(4.dp).background(DarkLinearTrack,RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f,1f)).fillMaxHeight().background(MintPrimary,RoundedCornerShape(2.dp)))
        }
    }
}
