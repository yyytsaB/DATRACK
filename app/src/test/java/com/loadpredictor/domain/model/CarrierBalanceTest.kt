package com.loadpredictor.domain.model

import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.backup.*
import org.junit.Assert.*
import org.junit.Test

class CarrierBalanceTest {
    private val original=Promo(id=2,name="Saved",totalAllowanceBytes=10_000_000_000,startTimestamp=1000,initialUsageOffsetBytes=1_000_000_000)
    @Test fun correctionsCanIncreaseOrDecreaseBalanceAndSubtractSubsequentTrafficOnce() {
        val higher=original.copy(correctionRemaining=9_000_000_000,correctionMeasured=2_000_000_000,correctionAt=5000,correctionRevision=1)
        assertEquals(9_000_000_000,higher.remainingBytes(2_000_000_000))
        assertEquals(8_000_000_000,higher.remainingBytes(3_000_000_000))
        assertEquals(0,higher.remainingBytes(Long.MAX_VALUE))
        val lower=higher.copy(correctionRemaining=1_000_000_000)
        assertEquals(500_000_000,lower.remainingBytes(2_500_000_000))
        assertEquals(original.initialUsageOffsetBytes,higher.initialUsageOffsetBytes)
    }
    @Test fun correctionDoesNotTrainTrafficRateAndExpiryStillWins() {
        val adjusted=original.copy(correctionRemaining=8_000_000_000,correctionMeasured=500_000_000,correctionAt=5000,correctionRevision=1)
        val engine=BurnRateEngine();val at=10_000_000L;val measured=1_000_000_000L
        assertEquals(engine.calculateForecast(original,measured,at).burnRateBytesPerHour,
            engine.calculateForecast(adjusted,measured,at).burnRateBytesPerHour,0.0)
        assertEquals(BurnPace.EXPIRED,engine.calculateForecast(adjusted.copy(expirationTimestamp=6000),measured,at).pace)
    }
    @Test fun portableHistoryRoundTripsAndVersionOneStillReads() {
        val p=BackupPromo(2,"Saved",10_000,1000,correctionRemaining=9000,correctionMeasured=2000,correctionAt=5000,correctionRevision=1)
        val doc=BackupDocument(createdAt=6000,promos=listOf(p),corrections=listOf(BackupCorrection(1,2,9000,2000,5000,6000)))
        assertEquals(doc,BackupCodec.decode(BackupCodec.encode(doc,7000),7000))
        val legacy="""{"kind":"datrack.configuration","formatVersion":1,"createdAt":1000,"promos":[],"alerts":{}}"""
        assertEquals(1,BackupCodec.decode(legacy.toByteArray(),7000).formatVersion)
        assertThrows(IllegalArgumentException::class.java){BackupCodec.encode(doc.copy(corrections=emptyList()),7000)}
    }
}
