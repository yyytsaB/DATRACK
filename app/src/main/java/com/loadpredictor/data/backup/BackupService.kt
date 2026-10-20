package com.loadpredictor.data.backup

import android.content.Context
import android.util.AtomicFile
import androidx.room.withTransaction
import com.loadpredictor.data.local.*
import com.loadpredictor.data.local.entity.toEntity
import com.loadpredictor.domain.backup.*
import com.loadpredictor.domain.model.AlertPreferences
import com.loadpredictor.domain.sync.DataOperationGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File

/** Target journal is durable before Room/DataStore writes; startup finishes an interrupted restore. */
class BackupService(context:Context,
    private val db:AppDatabase=AppDatabase.getInstance(context),
    private val alerts:AlertPreferencesDataSource=AlertPreferencesDataSource(context),
    private val notifications:NotificationPreferencesDataSource=NotificationPreferencesDataSource(context),
    directory:File=context.noBackupFilesDir,
    private val afterDatabase:suspend ()->Unit={}) {
    private val app=context.applicationContext
    private val target=AtomicFile(File(directory,"restore-target.json"))
    private val safety=AtomicFile(File(directory,"restore-before.json"))
    private fun exists(file:AtomicFile)=file.baseFile.exists() || File(file.baseFile.path+".bak").exists()
    private fun write(file:AtomicFile,doc:BackupDocument) {
        val bytes=BackupCodec.encode(doc);val stream=file.startWrite()
        try {stream.write(bytes);file.finishWrite(stream)}catch(e:Exception){file.failWrite(stream);throw e}
    }
    private fun read(file:AtomicFile)=file.openRead().use {BackupCodec.decode(BackupCodec.read(it))}
    private suspend fun snapshot():BackupDocument {
        val flags=alerts.alertPreferencesFlow.first();val ledger=notifications.snapshotLedger()
        val rows=db.promoDao().getAllPromos().first().map {
            BackupPromo(it.id,it.name,it.totalAllowanceBytes,it.startTimestamp,it.expirationTimestamp,
                it.initialUsageOffsetBytes,it.simSlot.name,it.isActive,ledger.milestones[it.id].orEmpty(),
                it.correctionRemaining,it.correctionMeasured,it.correctionAt,it.correctionRevision)
        }
        return BackupDocument(createdAt=System.currentTimeMillis(),promos=rows,
            alerts=BackupAlerts(flags.is50Enabled,flags.is80Enabled,flags.is90Enabled,flags.isPrematureEnabled),
            corrections=db.promoDao().allCorrections().map {BackupCorrection(it.id,it.promoId,it.remaining,it.measured,it.observedAt,it.createdAt,it.undone)})
    }
    suspend fun export():BackupDocument=withContext(Dispatchers.IO) {DataOperationGate.exclusive {snapshot()}}
    private suspend fun apply(doc:BackupDocument,injectFailure:Boolean=false) {
        doc.validate(System.currentTimeMillis())
        db.withTransaction {
            db.promoDao().clearForRestore()
            doc.promos.forEach {db.promoDao().insertPromo(it.toPromo().toEntity())}
            doc.corrections.forEach {db.promoDao().insertCorrection(com.loadpredictor.data.local.entity.BalanceCorrectionEntity(it.id,it.promoId,it.remaining,it.measured,it.observedAt,it.createdAt,it.undone))}
        }
        if(injectFailure)afterDatabase()
        alerts.replace(AlertPreferences(doc.alerts.at50,doc.alerts.at80,doc.alerts.at90,doc.alerts.premature))
        notifications.replaceLedger(NotificationLedger(doc.promos.associate {it.id to it.notified},
            doc.promos.map {it.id}.toSet()))
        (app.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).cancelAll()
    }
    suspend fun restore(doc:BackupDocument,expectedCurrent:BackupDocument)=withContext(Dispatchers.IO) {
        DataOperationGate.exclusive {
            doc.validate(System.currentTimeMillis())
            val before=snapshot()
            require(before.sameConfiguration(expectedCurrent)) {"Saved data changed. Export a fresh safety backup before restoring."}
            write(safety,before)
            write(target,doc)
            try {
                apply(doc,true)
                target.delete()
                check(!exists(target)){"Restore journal could not be cleared"}
            } catch(e:Exception) {
                DataOperationGate.requireRecovery()
                if(e is kotlinx.coroutines.CancellationException)throw e
                throw IllegalStateException("Restore interrupted. Use Retry recovery or reopen DATRACK to finish it.",e)
            }
        }
    }
    suspend fun recoverPending()=withContext(Dispatchers.IO) {DataOperationGate.recovery {
        if(exists(target)) {
            try {
                // If the target itself was corrupted, recover the previous validated configuration.
                val doc=try {read(target)}catch(_:Exception){read(safety)}
                apply(doc)
                target.delete()
                check(!exists(target)){"Restore journal could not be cleared"}
            }catch(e:Exception){DataOperationGate.requireRecovery();throw e}
        }
        DataOperationGate.recoveryCompleted()
    }}
}
