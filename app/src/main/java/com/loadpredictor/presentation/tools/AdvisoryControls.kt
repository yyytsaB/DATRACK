package com.loadpredictor.presentation.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.loadpredictor.data.backup.AdvisoryService
import com.loadpredictor.data.local.AdvisoryPreferences
import com.loadpredictor.domain.model.*
import com.loadpredictor.presentation.theme.*
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.*

@Composable fun AdvisoryControls(promo:Promo?=null) {
    val context=LocalContext.current;val prefs=remember {AdvisoryPreferences(context)}
    val loaded by prefs.flow.collectAsState(initial=null)
    val state=loaded ?: return
    var open by remember {mutableStateOf(false)};var reading by remember {mutableStateOf("")}
    val lifecycle=LocalLifecycleOwner.current
    LaunchedEffect(state.settings,lifecycle) {lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        if(state.settings.dailyCap==null)return@repeatOnLifecycle
        do {
            reading=try {val at=System.currentTimeMillis();val bytes=AdvisoryService(context).today(at)
                "Today: ${DataFormatter.formatBytes(bytes)} of ${DataFormatter.formatBytes(state.settings.dailyCap ?: 0)} · checked ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(at))}"
            }catch(e:CancellationException){throw e}catch(e:Exception){e.message ?: "Today's usage unavailable"}
            delay(60_000)
        }while(isActive && state.settings.dailyCap!=null)
    }}
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        ToolActionRow("Budgets & reminders",description="Daily mobile budget and actual-expiry alerts",icon=Icons.Default.NotificationsNone,onClick={open=true})
        if(state.settings.dailyCap!=null)Text(reading,style=MaterialTheme.typography.bodyMedium,color=TextMediumEmphasis)
    }
    if(open)AdvisoryDialog(state.settings,promo,onDismiss={open=false})
}

@Composable private fun AdvisoryDialog(settings:AdvisorySettings,promo:Promo?,onDismiss:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var budget by remember {mutableStateOf(settings.dailyCap?.toBigDecimal()?.divide(java.math.BigDecimal(1048576))?.stripTrailingZeros()?.toPlainString() ?: "")}
    var notify by remember {mutableStateOf(settings.dailyNotify)};var expiry by remember {mutableStateOf(settings.expiryHours)}
    var status by remember {mutableStateOf("")};var busy by remember {mutableStateOf(false)}
    ToolSheet(title="Budgets & reminders",subtitle="Advisory only · no data blocking",busy=busy,onDismiss=onDismiss,feedback=status,feedbackError=true,footer={
        ToolPrimary("Save changes",enabled=!busy,onClick={scope.launch {
            busy=true;status=""
            try {
                val cap=if(budget.isBlank())null else (budget.toBigDecimalOrNull() ?: error("Enter a valid budget")).multiply(java.math.BigDecimal(1048576)).setScale(0,java.math.RoundingMode.HALF_UP).longValueExact()
                require(cap==null || cap>0){"Budget must be greater than zero"}
                AdvisoryService(context).save(AdvisorySettings(cap,notify && cap!=null,expiry),System.currentTimeMillis())
                com.loadpredictor.worker.WorkManagerScheduler.enqueueImmediateSync(context,"advisory_settings")
                onDismiss()
            }catch(e:CancellationException){throw e}catch(e:Exception){status=e.message ?: "Unable to save"}finally{busy=false}
        }})
    }) {
        ToolSection("Daily mobile budget","Counts mobile traffic since local midnight across all SIMs.") {
            OutlinedTextField(colors=toolFieldColors(),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),value=budget,onValueChange={budget=it;if(it.isBlank())notify=false},enabled=!busy,
                label={Text("Daily budget (MB)")},suffix={Text("MB")},singleLine=true,
                supportingText={Text("Leave blank to turn off. 1024 MB = 1 GB.")},
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("Budget notification",style=MaterialTheme.typography.titleSmall)
                    Text("Once when today's budget is reached",style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)
                }
                Switch(checked=notify,onCheckedChange={notify=it},enabled=!busy && budget.isNotBlank(),
                    colors=SwitchDefaults.colors(checkedThumbColor=MintOnPrimary,checkedTrackColor=MintPrimary))
            }
            if(promo?.expirationTimestamp!=null && !promo.isExpired(System.currentTimeMillis())) {
                val hours=(promo.expirationTimestamp-System.currentTimeMillis())/3600000.0
                if(hours>0 && promo.lastSyncTimestamp>0)ToolNotice("Optional pace guide: ${DataFormatter.formatBytes((promo.remainingBytes(promo.lastSyncDataUsedBytes)/(hours/24.0)).toLong())} per day until actual expiry. This is an estimate; choose your own budget.")
            }
        }
        HorizontalDivider(color=DarkOutline)
        ToolSection("Expiry reminder","Remind me before the active promo's actual expiry.") {
            val options=listOf("Off","6 hours","24 hours","48 hours")
            ToolChoices(options,expiry?.let {"$it hours"} ?: "Off",enabled=!busy,onSelect={expiry=it.substringBefore(' ').toIntOrNull()})
            Text(if(promo?.isNoExpiry==true)"${promo.name} has no expiry. This setting applies when an expiring promo is active." else "No-expiry promos are ignored. Android may delay delivery.",
                style=MaterialTheme.typography.bodyMedium,color=TextMediumEmphasis)
        }
        ToolHelp("How budgets and notifications work","Budgets count device mobile traffic since local midnight, excluding original offsets and carrier corrections. 1 MB = 1,048,576 bytes. Android readings and background checks can be delayed; data is never blocked. Already-reached events are consumed when settings are saved. Future daily crossings and future expiry windows may notify once. Notification permission and channel settings still apply.")
    }
}
