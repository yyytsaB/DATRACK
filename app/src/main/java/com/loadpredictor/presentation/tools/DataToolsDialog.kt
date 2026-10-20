package com.loadpredictor.presentation.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.loadpredictor.data.backup.BackupService
import com.loadpredictor.data.diagnostics.LocalDiagnostics
import com.loadpredictor.domain.backup.*
import com.loadpredictor.domain.sync.DataOperationGate
import kotlinx.coroutines.*

@Composable fun DataToolsDialog(onRestored:()->Unit={},onDismiss:()->Unit) {
    val context=androidx.compose.ui.platform.LocalContext.current;val scope=rememberCoroutineScope()
    val service=remember {BackupService(context.applicationContext)}
    var status by remember {mutableStateOf("")};var hasError by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var staged by remember {mutableStateOf<BackupDocument?>(null)}
    var safety by remember {mutableStateOf<BackupDocument?>(null)}
    fun fail(e:Exception) {status=e.message ?: "The operation failed. Saved data was not intentionally replaced.";hasError=true}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {uri->
        if(uri!=null)scope.launch {
            busy=true;hasError=false
            try {
                val doc=service.export()
                withContext(Dispatchers.IO) {requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).use {it.write(BackupCodec.encode(doc))}}
                if(staged!=null){safety=doc;status="Safety backup saved. Review the replacement before continuing."}
                else status="Configuration backup saved. Keep this unencrypted file private."
            }catch(e:CancellationException){throw e}catch(e:Exception){fail(e)}finally{busy=false}
        }
    }
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        if(uri!=null)scope.launch {
            busy=true;staged=null;safety=null;hasError=false
            try {
                val doc=withContext(Dispatchers.IO) {requireNotNull(context.contentResolver.openInputStream(uri)).use {BackupCodec.decode(BackupCodec.read(it))}}
                staged=doc;status="Backup validated. Save a fresh safety backup before replacing your data."
            }catch(e:CancellationException){throw e}catch(e:Exception){status="Backup rejected: ${e.message ?: "invalid file"}. Saved data is unchanged.";hasError=true}finally{busy=false}
        }
    }
    val support=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {uri->
        if(uri!=null)scope.launch {
            busy=true;hasError=false
            try {withContext(Dispatchers.IO) {val report=LocalDiagnostics(context).supportReport()
                requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).use {it.write(report.toByteArray())}}
                status="Support report saved. Review its usage windows and byte counts before sharing."
            }catch(e:CancellationException){throw e}catch(e:Exception){fail(e)}finally{busy=false}
        }
    }
    val recovery=DataOperationGate.isRecoveryRequired
    val doc=staged
    ToolSheet(title="Data and support",subtitle="Backups, recovery and local diagnostics",busy=busy,
        canDismiss=!recovery,onDismiss=onDismiss,heightFraction=.84f,feedback=status,feedbackError=hasError,footer={
        if(recovery)ToolPrimary("Retry recovery",enabled=!busy,onClick={scope.launch {
            busy=true;hasError=false
            try {service.recoverPending();status="Recovery completed.";onRestored()}
            catch(e:CancellationException){throw e}catch(e:Exception){fail(e)}finally{busy=false}
        }}) else if(doc!=null) {
            if(safety==null)ToolPrimary("Save safety backup",enabled=!busy,onClick={export.launch("DATRACK-before-restore.json")})
            else ToolPrimary("Replace saved data",enabled=!busy,danger=true,onClick={scope.launch {
                busy=true;hasError=false
                try {service.restore(doc,requireNotNull(safety));staged=null;safety=null;status="Restore completed. Your configuration is available; readings will refresh.";onRestored()}
                catch(e:CancellationException){throw e}catch(e:Exception){safety=null;fail(e)}finally{busy=false}
            }})
        }
    }) {
        if(recovery)ToolNotice("An interrupted restore needs recovery before readings or edits can continue.",error=true)
        else if(doc!=null) {
            ToolSection("Review replacement") {
                ToolValue("Saved promos",doc.promos.size.toString())
                ToolValue("Active promo",doc.promos.firstOrNull {it.active}?.name ?: "None")
                ToolNotice("Replaces all current promos, carrier corrections, budgets and alert settings. Readings will recalibrate; already-crossed alerts will not replay.")
                Text(if(safety==null)"First, save a fresh safety copy of your current configuration." else "Your safety copy is saved. Replace only if this is the backup you intend to use.",
                    style=MaterialTheme.typography.bodyMedium,color=com.loadpredictor.presentation.theme.TextMediumEmphasis)
                ToolSecondary("Cancel restore",enabled=!busy,onClick={staged=null;safety=null;status="Restore cancelled. Saved data is unchanged.";hasError=false})
            }
        } else ToolSection("Configuration backup","Unencrypted JSON · keep exported files private.") {
            ToolActionRow("Export configuration backup","Save promos, corrections and preferences",Icons.Default.FileUpload,enabled=!busy,onClick={export.launch("DATRACK-backup.json")})
            ToolActionRow("Review backup to restore","Validate a file before replacing anything",Icons.Default.Restore,enabled=!busy,onClick={open.launch(arrayOf("application/json","text/plain","application/octet-stream"))})
        }
        HorizontalDivider(color=com.loadpredictor.presentation.theme.DarkOutline)
        ToolSection("Local diagnostics","Up to 1000 events or 7 days. Nothing uploads automatically.") {
            ToolActionRow("Export support report","Review the file before sharing",Icons.Default.Description,enabled=!busy,onClick={support.launch("DATRACK-support.txt")})
        }
        ToolHelp("What's included in a backup?","Promos, original usage offsets, carrier corrections and alert/budget settings are included. Files are unencrypted. Android usage history, forecast caches, work queues and widgets are not backed up. Restore requires a fresh safety export. Background readings and notifications can be delayed by Android.")
    }
}
