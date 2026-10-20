package com.loadpredictor.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loadpredictor.data.backup.BackupService
import com.loadpredictor.domain.backup.*
import com.loadpredictor.domain.sync.DataOperationGate
import com.loadpredictor.data.repository.PromoRepositoryImpl
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule

class BackupRestoreTest {
    @get:Rule val preserved=com.loadpredictor.testing.PreserveVerificationState()
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun target()=BackupDocument(createdAt=System.currentTimeMillis(),
        promos=listOf(BackupPromo(1234,"Restore fixture",10737418240,1000,offset=8589934592,
            active=true,notified=setOf("50"))),alerts=BackupAlerts(at80=false,premature=false))
    private fun dir()=File(context.cacheDir,"restore-test").apply {mkdirs()}
    @Test fun roundTripPreservesIdsSettingsAndPrimesAlertsWithoutRestoringCaches()=runBlocking {
        val folder=dir();val service=BackupService(context,directory=folder)
        try {
            val before=service.export();val imported=BackupCodec.decode(BackupCodec.encode(target()))
            service.restore(imported,before)
            val saved=service.export()
            assertEquals(imported.promos,saved.promos);assertEquals(imported.alerts,saved.alerts)
            val row=AppDatabase.getInstance(context).promoDao().getActivePromo().first()!!
            assertEquals(1234L,row.id);assertNull(row.lastActiveBurnRate);assertEquals(0L,row.lastSyncTimestamp)
            val ledger=NotificationPreferencesDataSource(context)
            assertTrue(1234L in ledger.snapshotLedger().pendingRestore)
            ledger.primeRestoredPromo(1234,setOf("50","80"))
            assertEquals(setOf("50","80"),ledger.getNotifiedThresholds(1234).first())
            assertFalse(1234L in ledger.snapshotLedger().pendingRestore)
            ledger.primeRestoredPromo(1234,setOf("90"))
            assertFalse("90" in ledger.getNotifiedThresholds(1234).first())
        } finally {DataOperationGate.recoveryCompleted();folder.deleteRecursively()}
    }
    @Test fun interruptionAfterDatabaseRecoversSettingsBeforeEditsAreAllowed()=runBlocking {
        val folder=dir();val normal=BackupService(context,directory=folder)
        try {
            val before=normal.export();val staged=target()
            val interrupted=BackupService(context,directory=folder,afterDatabase={error("Simulated interruption")})
            try {interrupted.restore(staged,before);fail("Must simulate interruption")}catch(_:IllegalStateException){}
            assertTrue(DataOperationGate.isRecoveryRequired)
            try {PromoRepositoryImpl(AppDatabase.getInstance(context).promoDao()).setActivePromo(1234);fail("Edits must be blocked")}
            catch(_:IllegalStateException){}
            normal.recoverPending()
            assertFalse(DataOperationGate.isRecoveryRequired)
            assertTrue(normal.export().sameConfiguration(staged))
            assertFalse(File(folder,"restore-target.json").exists())
        } finally {DataOperationGate.recoveryCompleted();folder.deleteRecursively()}
    }
    @Test fun staleSafetyBackupCannotReplaceNewerSettings()=runBlocking {
        val folder=dir();val service=BackupService(context,directory=folder)
        try {
            val before=service.export();AlertPreferencesDataSource(context).set80AlertEnabled(!before.alerts.at80)
            val current=service.export()
            try {service.restore(target(),before);fail("Must reject stale safety copy")}catch(_:IllegalArgumentException){}
            assertTrue(current.sameConfiguration(service.export()));assertFalse(File(folder,"restore-target.json").exists())
        } finally {DataOperationGate.recoveryCompleted();folder.deleteRecursively()}
    }
    @Test fun corruptedPendingTargetFallsBackToThePrivateSafetyCopy()=runBlocking {
        val folder=dir();val normal=BackupService(context,directory=folder)
        try {
            val before=normal.export()
            try {BackupService(context,directory=folder,afterDatabase={error("Interrupted")}).restore(target(),before)}
            catch(_:IllegalStateException){}
            File(folder,"restore-target.json").writeText("{broken")
            normal.recoverPending()
            assertTrue(before.sameConfiguration(normal.export()))
            assertFalse(DataOperationGate.isRecoveryRequired)
        } finally {DataOperationGate.recoveryCompleted();folder.deleteRecursively()}
    }

}
