package com.loadpredictor.domain.model

import java.time.*
import org.junit.Assert.*
import org.junit.Test
class AdvisoryPolicyTest {
    private val zone=ZoneId.of("America/New_York")
    @Test fun localMidnightRespectsDstAndDailyKeysRollOver() {
        val at=ZonedDateTime.of(2026,3,8,23,59,0,0,zone).toInstant().toEpochMilli()
        val start=AdvisoryPolicy.dayStart(at,zone)
        val next=AdvisoryPolicy.dayStart(at+120000,zone)
        assertEquals(23*3600000L,next-start)
        val settings=AdvisorySettings(100,true)
        assertEquals(0,AdvisoryPolicy.eligible(settings,null,99,at,zone).size)
        val a=AdvisoryPolicy.eligible(settings,null,100,at,zone).single()
        val b=AdvisoryPolicy.eligible(settings,null,100,at+120000,zone).single()
        assertNotEquals(a.key,b.key)
    }
    @Test fun actualExpiryOnlySelectedPromoAndValidWindow() {
        val now=100_000_000L;val settings=AdvisorySettings(expiryHours=6)
        val p=Promo(id=1,name="Promo",totalAllowanceBytes=1000,startTimestamp=1,expirationTimestamp=now+6*3600000)
        fun events(promo:Promo?,at:Long=now)=AdvisoryPolicy.eligible(settings,promo,9999,at,zone)
        assertEquals(1,events(p).size)
        assertTrue(events(p.copy(expirationTimestamp=null)).isEmpty())
        assertTrue(events(p.copy(isActive=false)).isEmpty())
        assertTrue(events(p,p.expirationTimestamp!!).isEmpty())
        assertTrue(events(p,now-1).isEmpty())
        assertNotEquals(events(p).single().key,events(p.copy(expirationTimestamp=p.expirationTimestamp-1)).single().key)
    }
    @Test fun invalidBudgetsAndReminderWindowsAreRejected() {
        listOf(AdvisorySettings(0),AdvisorySettings(dailyNotify=true),AdvisorySettings(expiryHours=7)).forEach {
            assertThrows(IllegalArgumentException::class.java){it.validate()}
        }
    }
}
