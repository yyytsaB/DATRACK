package com.loadpredictor.data.diagnostics

import android.content.Context
import android.util.AtomicFile
import com.loadpredictor.domain.diagnostics.ObservationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class LocalDiagnostics(context:Context, directory:File=context.noBackupFilesDir) {
    private val app=context.applicationContext
    private val file=AtomicFile(File(directory,"observations.json"))
    private val json=Json {encodeDefaults=true}
    companion object {private val lock=Mutex();const val MAX_EVENTS=1000;const val RETENTION=7*86400000L}
    private fun read():List<ObservationEvent> = try {
        file.openRead().use {stream->
            val data=com.loadpredictor.domain.backup.BackupCodec.read(stream)
            json.decodeFromString(data.toString(Charsets.UTF_8))
        }
    } catch(_:java.io.FileNotFoundException){emptyList()}
    private fun write(events:List<ObservationEvent>) {
        val out=file.startWrite()
        try {out.write(json.encodeToString(events).toByteArray());file.finishWrite(out)}
        catch(e:Exception){file.failWrite(out);throw e}
    }
    suspend fun append(event:ObservationEvent)=withContext(Dispatchers.IO) {lock.withLock {
        // A corrupt diagnostic file may be replaced; configuration files are never touched.
        val old=try{read()}catch(_:Exception){emptyList()}
        write((old.filter {it.at>=event.at-RETENTION && it.at<=event.at+300000}+event).takeLast(MAX_EVENTS))
    }}
    suspend fun events(now:Long=System.currentTimeMillis())=withContext(Dispatchers.IO) {lock.withLock {
        read().filter {it.at>=now-RETENTION && it.at<=now+300000}.takeLast(MAX_EVENTS)
    }}
    suspend fun supportReport():String {
        @Suppress("DEPRECATION") val version=app.packageManager.getPackageInfo(app.packageName,0).versionName
        return "DATRACK local support report\nVersion: $version\nAndroid: ${android.os.Build.VERSION.SDK_INT}\n"+
            "Up to 1000 events / 7 days. Android may delay background work.\n"+
            "Contains usage windows and byte counts; review before sharing. No notification contents or app rankings.\n"+
            json.encodeToString(events())
    }
}
