package com.loadpredictor
import android.app.Application
import com.loadpredictor.domain.diagnostics.*
import com.loadpredictor.data.diagnostics.LocalDiagnostics
import kotlinx.coroutines.*
class DatrackApplication:Application() {
    override fun onCreate() {
        super.onCreate()
        val local=LocalDiagnostics(this)
        ObservationLog.recorder={local.append(it)}
        runBlocking(Dispatchers.IO) {
            ObservationLog.record(ObservationEvent(System.currentTimeMillis(),"process","started"))
            try {com.loadpredictor.data.backup.BackupService(this@DatrackApplication).recoverPending()}
            catch(_:Exception) {ObservationLog.record(ObservationEvent(System.currentTimeMillis(),"restore","recovery_required"))}
        }
    }
}
