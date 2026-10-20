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
import com.loadpredictor.data.backup.BackupService
import com.loadpredictor.data.diagnostics.LocalDiagnostics
import com.loadpredictor.domain.backup.*
import com.loadpredictor.domain.sync.DataOperationGate
import kotlinx.coroutines.*

@Composable fun DataToolsDialog(onRestored:()->Unit={},onDismiss:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val service=remember {BackupService(context.applicationContext)}
    var status by remember {mutableStateOf("Backups contain promo configuration, usage offsets and alert settings. Files are unencrypted. Android usage history, forecast caches and widgets are not backed up.")}
    var busy by remember {mutableStateOf(false)}
    var staged by remember {mutableStateOf<BackupDocument?>(null)}
    var safety by remember {mutableStateOf<BackupDocument?>(null)}
    fun fail(e:Exception) {status=e.message ?: "The operation failed. Saved data was not intentionally replaced."}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {uri->
        if(uri!=null)scope.launch {
            busy=true
            try {
                val doc=service.export()
                withContext(Dispatchers.IO) {requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).use {it.write(BackupCodec.encode(doc))}}
                if(staged!=null){safety=doc;status="Safety backup saved. Review the replacement below before continuing."}
                else status="Configuration backup saved. Keep this unencrypted file private."
            }catch(e:CancellationException){throw e}catch(e:Exception){fail(e)}finally{busy=false}
        }
    }
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        if(uri!=null)scope.launch {
            busy=true;staged=null;safety=null
            try {
                val doc=withContext(Dispatchers.IO) {requireNotNull(context.contentResolver.openInputStream(uri)).use {BackupCodec.decode(BackupCodec.read(it))}}
                staged=doc
                status="Backup validated. Export a fresh safety backup of your current data before replacement."
            }catch(e:CancellationException){throw e}catch(e:Exception){status="Backup rejected: ${e.message ?: "invalid file"}. Saved data is unchanged."}finally{busy=false}
        }
    }
    val support=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {uri->
        if(uri!=null)scope.launch {
            busy=true
            try {withContext(Dispatchers.IO) {val report=LocalDiagnostics(context).supportReport()
                requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).use {it.write(report.toByteArray())}}
                status="Support report saved. Review its usage windows and byte counts before sharing."
            }catch(e:CancellationException){throw e}catch(e:Exception){fail(e)}finally{busy=false}
        }
    }
    AlertDialog(onDismissRequest={if(!busy && !DataOperationGate.isRecoveryRequired)onDismiss()},title={Text("Data and support")},text={Column(
        Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(status)
        if(DataOperationGate.isRecoveryRequired) {
            Text("An interrupted restore needs recovery before readings or edits can continue.")
            Button(enabled=!busy,onClick={scope.launch {
                busy=true
                try {service.recoverPending();status="Recovery completed.";onRestored()}
                catch(e:CancellationException){throw e}catch(e:Exception){fail(e)}finally{busy=false}
            }}){Text("Retry recovery")}
        } else {
            val doc=staged
            if(doc!=null) {
                val active=doc.promos.firstOrNull {it.active}?.name ?: "None"
                Text("Replace all current saved promos and alert settings with ${doc.promos.size} promo(s). Active promo: $active. Existing measurement checkpoints will be recalibrated; already-crossed alerts will not replay.")
                OutlinedButton(enabled=!busy,onClick={export.launch("DATRACK-before-restore.json")}){Text("Save safety backup")}
                Button(enabled=!busy && safety!=null,onClick={scope.launch {
                    busy=true
                    try {service.restore(doc,requireNotNull(safety));staged=null;safety=null;status="Restore completed. Saved configuration is available; readings will refresh.";onRestored()}
                    catch(e:CancellationException){throw e}catch(e:Exception){safety=null;fail(e)}finally{busy=false}
                }}){Text("Replace saved data")}
                TextButton(enabled=!busy,onClick={staged=null;safety=null;status="Restore cancelled. Saved data is unchanged."}){Text("Cancel restore")}
            } else {
                OutlinedButton(enabled=!busy,onClick={export.launch("DATRACK-backup.json")}){Text("Export configuration backup")}
                OutlinedButton(enabled=!busy,onClick={open.launch(arrayOf("application/json","text/plain","application/octet-stream"))}){Text("Review backup to restore")}
            }
        }
        HorizontalDivider()
        Text("Diagnostics stay on this phone: up to 1000 events / 7 days. Background work may be delayed by Android.")
        OutlinedButton(enabled=!busy,onClick={support.launch("DATRACK-support.txt")}){Text("Export support report")}
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    }},confirmButton={TextButton(enabled=!busy && !DataOperationGate.isRecoveryRequired,onClick=onDismiss){Text("Done")}})
}
