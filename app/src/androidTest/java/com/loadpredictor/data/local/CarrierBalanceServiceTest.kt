package com.loadpredictor.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loadpredictor.data.backup.CarrierBalanceService
import com.loadpredictor.data.local.entity.PromoEntity
import com.loadpredictor.testing.PreserveVerificationState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CarrierBalanceServiceTest {
    @get:Rule val preserve=PreserveVerificationState()
    @Test fun compareApplyDuplicateUndoAndCosmeticEdits()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val dao=AppDatabase.getInstance(context).promoDao()
        dao.deactivateAllPromos();val now=System.currentTimeMillis()
        val id=dao.insertPromo(PromoEntity(name="Correction fixture",totalAllowanceBytes=10000,startTimestamp=now-3600000,initialUsageOffsetBytes=1000,isActive=true))
        val service=CarrierBalanceService(context){_,_->2000}
        val c=service.compare(id,9000,now,now)
        assertEquals(7000,c.appRemaining);assertNull(dao.findById(id)!!.correctionAt)
        service.apply(c,now);assertEquals(9000,dao.findById(id)!!.toDomain().remainingBytes(2000))
        try {service.apply(c,now);fail("duplicate apply")}catch(_:IllegalArgumentException){}
        val corrected=dao.findById(id)!!
        dao.saveConfiguredPromo(corrected.copy(name="Renamed",correctionRemaining=null,correctionMeasured=null,correctionAt=null,correctionRevision=0))
        assertEquals(9000L,dao.findById(id)!!.correctionRemaining)
        service.undo(id,1);assertEquals(7000,dao.findById(id)!!.toDomain().remainingBytes(2000))
        assertTrue(dao.corrections(id).single().undone)
        assertEquals(1000,dao.findById(id)!!.initialUsageOffsetBytes)
        try {service.compare(id,10001,now+1,now+1);fail("impossible reading")}catch(_:IllegalArgumentException){}
        try {service.compare(id,5000,now-1000000,now);fail("stale reading")}catch(_:IllegalArgumentException){}
    }
}
