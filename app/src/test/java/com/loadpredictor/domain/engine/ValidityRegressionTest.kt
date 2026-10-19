package com.loadpredictor.domain.engine

import com.loadpredictor.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class ValidityRegressionTest {
    private val hour = 3_600_000L
    @Test fun `unused data becomes unavailable at expiry boundary`() {
        val promo = Promo(name = "Fixture", totalAllowanceBytes = 1000L,
            startTimestamp = 0L, expirationTimestamp = 48 * hour)
        val result = BurnRateEngine().calculateForecast(promo, 100L, 48 * hour)
        assertEquals(BurnPace.EXPIRED, result.pace)
        assertNull(result.estimatedDepletionTimestamp)
        assertFalse(result.plainLanguageSummary.contains("optimal"))
    }
    @Test fun `inactive balance includes offset and clamps extreme readings`() {
        val promo = Promo(name = "Fixture", totalAllowanceBytes = 1000L,
            startTimestamp = 0L, initialUsageOffsetBytes = 400L)
        assertEquals(500L, promo.remainingBytes(100L))
        assertEquals(0L, promo.remainingBytes(Long.MAX_VALUE))
    }
    @Test fun `partial first day is never a full training day`() {
        val day = 86_400_000L
        assertFalse(UsageBucket(hour, day, 100, 0).isCompletedDay(day * 2, ZoneId.of("UTC")))
        assertTrue(UsageBucket(0, day, 100, 0).isCompletedDay(day * 2, ZoneId.of("UTC")))
    }
}
