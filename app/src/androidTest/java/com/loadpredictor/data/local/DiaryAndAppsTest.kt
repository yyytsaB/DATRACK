package com.loadpredictor.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loadpredictor.data.backup.*
import com.loadpredictor.data.stats.MobileAppUsageSource
import com.loadpredictor.data.local.entity.PromoEntity
import com.loadpredictor.testing.PreserveVerificationState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.*

class DiaryAndAppsTest {
    @get:Rule val preserve=PreserveVerificationState()
    @Test fun realMobileQueriesExportExactSnapshotAndKeepAttributionSeparate()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val dao=AppDatabase.getInstance(context).promoDao()
        val zone=ZoneId.systemDefault();val now=System.currentTimeMillis();val today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val start=today.minusDays(2).atTime(12,0).atZone(zone).toInstant().toEpochMilli()
        val id=dao.insertPromo(PromoEntity(name="Diary fixture",totalAllowanceBytes=10_000_000_000,startTimestamp=start,initialUsageOffsetBytes=5_000_000_000,isActive=false))
        val s=DiaryService(context).load(id,today.minusDays(2),today,now,zone)
        assertEquals(3,s.buckets.size);assertEquals(start,s.window.start);assertEquals(now,s.window.end)
        val csv=s.csv();assertTrue(csv.contains("device-total mobile"));assertTrue(csv.contains("partial;"))
        assertEquals(4,csv.trim().lines().size)
        val rows=MobileAppUsageSource(context).query(s.window.start,s.window.end)
        assertEquals(rows.map {it.uid}.toSet().size,rows.size)
        assertTrue(rows.all {it.uid != -1 && it.bytes>0 && it.label.isNotBlank()})
        assertEquals(rows.sortedByDescending {it.bytes},rows)
        assertEquals(5_000_000_000,dao.findById(id)!!.initialUsageOffsetBytes)
    }
    @Test fun correctionBackupRestoresAuditAndConsumesEachNotificationRevisionOnce()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val dao=AppDatabase.getInstance(context).promoDao()
        dao.deactivateAllPromos();val now=System.currentTimeMillis()
        val id=dao.insertPromo(PromoEntity(name="Audited fixture",totalAllowanceBytes=10000,startTimestamp=now-3600000,isActive=true))
        val carrier=CarrierBalanceService(context){_,_->2000}
        carrier.apply(carrier.compare(id,9000,now,now),now)
        carrier.apply(carrier.compare(id,3000,now+1,now+1),now+1)
        carrier.undo(id,2)
        val original=BackupService(context).export();val folder=java.io.File(context.cacheDir,"correction-restore-test").apply {mkdirs()}
        try {
            val service=BackupService(context,directory=folder);service.restore(original,service.export())
            val restored=service.export();assertEquals(original.corrections,restored.corrections);assertEquals(original.promos,restored.promos)
            assertEquals(9000L,dao.findById(id)!!.correctionRemaining)
            val ledger=NotificationPreferencesDataSource(context)
            ledger.primeCorrection(id,3,setOf("50"));ledger.recordThresholdNotified(id,"80")
            ledger.primeCorrection(id,3,setOf("50"));assertEquals(setOf("50","80"),ledger.snapshotLedger().milestones[id])
            ledger.primeCorrection(id,4,emptySet());assertEquals(emptySet<String>(),ledger.snapshotLedger().milestones[id])
        }finally{folder.deleteRecursively()}
    }
}
