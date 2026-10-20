package com.loadpredictor.presentation.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.loadpredictor.data.diagnostics.LocalDiagnostics
import kotlinx.coroutines.*

@Composable fun DataToolsDialog(onDismiss:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var status by remember {mutableStateOf("Diagnostics stay on this phone: up to 1000 events / 7 days.")}
    var busy by remember {mutableStateOf(false)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {uri->
        if(uri!=null) scope.launch {
            busy=true
            try {withContext(Dispatchers.IO) {
                val report=LocalDiagnostics(context).supportReport()
                requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).use {it.write(report.toByteArray())}
            };status="Support report saved. Review its usage windows and byte counts before sharing."}
            catch(_:Exception){status="Could not save the report. Try another location."}
            finally {busy=false}
        }
    }
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text("Data and support")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(status)
        OutlinedButton(enabled=!busy,onClick={export.launch("DATRACK-support.txt")}){Text("Export support report")}
    }},confirmButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Done")}})
}
