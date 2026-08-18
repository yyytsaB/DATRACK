package com.loadpredictor.domain.engine

import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BurnRateEngineTest {

    private lateinit var engine: BurnRateEngine

    @Before
    fun setUp() {
        engine = BurnRateEngine()
    }

    @Test
    fun `elapsed time zero does not divide by zero and returns insufficient data`() {
        val start = 1_000_000L
        val promo = Promo(
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = start,
            expirationTimestamp = start + 7 * 24 * 3_600_000L
        )

        val forecast = engine.calculateForecast(
            promo = promo,
            dataUsedBytesRaw = 0L,
            currentTime = start // T_elapsed = 0
        )

        assertEquals(0.0, forecast.burnRateBytesPerHour, 0.001)
        assertEquals(BurnPace.INSUFFICIENT_DATA, forecast.pace)
        assertEquals(promo.totalAllowanceBytes, forecast.dataRemainingBytes)
        assertEquals(promo.expirationTimestamp, forecast.estimatedDepletionTimestamp)
        assertFalse(forecast.isDepleted)
        assertTrue(forecast.plainLanguageSummary.contains("Calibrating"))
    }

    @Test
    fun `initial hour with burst usage is dampened to insufficient data to prevent distortion`() {
        val start = 1_000_000L
        val current = start + 30 * 60 * 1_000L // 30 minutes elapsed (< 1 hour)
        val promo = Promo(
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L, // 2 GB
            startTimestamp = start,
            expirationTimestamp = start + 7 * 24 * 3_600_000L
        )

        // Used 200 MB in 30 mins
        val used = 200L * 1024L * 1024L
        val forecast = engine.calculateForecast(promo, used, current)

        assertEquals(BurnPace.INSUFFICIENT_DATA, forecast.pace)
        assertEquals(promo.expirationTimestamp, forecast.estimatedDepletionTimestamp)
        assertTrue(forecast.plainLanguageSummary.contains("Calibrating"))
    }

    @Test
    fun `elapsed time past stabilization window but usage below 10 MB returns insufficient data`() {
        val start = 1_000_000L
        val current = start + 3 * 3_600_000L // 3 hours elapsed (> 1 hour)
        val promo = Promo(
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L, // 24 GB
            startTimestamp = start,
            expirationTimestamp = null
        )

        // Only 15 KB consumed (background noise / pings, well below 10 MB threshold)
        val used = 15_000L
        val forecast = engine.calculateForecast(promo, used, current)

        assertEquals(BurnPace.INSUFFICIENT_DATA, forecast.pace)
        assertNull(forecast.estimatedDepletionTimestamp)
        assertNull(forecast.burnStatusIndex)
        assertTrue(forecast.plainLanguageSummary.contains("Calibrating pace"))
    }

    @Test
    fun `usage above 10 MB but elapsed time within stabilization window returns insufficient data`() {
        val start = 1_000_000L
        val current = start + 20 * 60 * 1_000L // 20 minutes elapsed (< 1 hour)
        val promo = Promo(
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L, // 24 GB
            startTimestamp = start,
            expirationTimestamp = null
        )

        // Used 50 MB (> 10 MB) but only 20 minutes elapsed
        val used = 50L * 1024L * 1024L
        val forecast = engine.calculateForecast(promo, used, current)

        assertEquals(BurnPace.INSUFFICIENT_DATA, forecast.pace)
        assertNull(forecast.estimatedDepletionTimestamp)
        assertTrue(forecast.plainLanguageSummary.contains("Calibrating pace"))
    }

    @Test
    fun `non-expiring promo in initial hour returns null depletion date`() {
        val start = 1_000_000L
        val current = start + 20 * 60 * 1_000L // 20 minutes elapsed
        val promo = Promo(
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L, // 24 GB
            startTimestamp = start,
            expirationTimestamp = null
        )

        val used = 100L * 1024L * 1024L
        val forecast = engine.calculateForecast(promo, used, current)

        assertEquals(BurnPace.INSUFFICIENT_DATA, forecast.pace)
        assertNull(forecast.estimatedDepletionTimestamp)
        assertNull(forecast.burnStatusIndex)
        assertTrue(forecast.plainLanguageSummary.contains("Calibrating"))
    }

    @Test
    fun `data consumed exceeding 100 percent clamps remaining data to zero and marks depleted`() {
        val start = 1_000_000L
        val current = start + 10 * 3_600_000L // 10 hours elapsed
        val promo = Promo(
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 1024L * 1024L * 1000L, // 1000 MB
            startTimestamp = start,
            expirationTimestamp = start + 24 * 3_600_000L
        )

        // Used 1200 MB (> 1000 MB)
        val used = 1024L * 1024L * 1200L
        val forecast = engine.calculateForecast(promo, used, current)

        assertEquals(0L, forecast.dataRemainingBytes)
        assertTrue(forecast.isDepleted)
        assertEquals(BurnPace.DEPLETED, forecast.pace)
        assertEquals(current, forecast.estimatedDepletionTimestamp)
        assertTrue(forecast.plainLanguageSummary.startsWith("Depleted!"))
    }

    @Test
    fun `zero burn rate after stabilization window returns insufficient data`() {
        val start = 1_000_000L
        val current = start + 5 * 3_600_000L // 5 hours elapsed
        val promo = Promo(
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = start,
            expirationTimestamp = start + 24 * 3_600_000L
        )

        val forecast = engine.calculateForecast(promo, 0L, current)

        assertEquals(0.0, forecast.burnRateBytesPerHour, 0.001)
        assertEquals(BurnPace.INSUFFICIENT_DATA, forecast.pace)
        assertEquals(promo.expirationTimestamp, forecast.estimatedDepletionTimestamp)
    }

    @Test
    fun `stabilized fast burn pace produces BURNING_FAST and depletion warning`() {
        val start = 1_000_000L
        val validityHours = 100L
        val expiration = start + validityHours * 3_600_000L
        val allowance = 10L * 1024L * 1024L * 1024L // 10 GB

        val promo = Promo(
            name = "Smart Power All 99",
            totalAllowanceBytes = allowance,
            startTimestamp = start,
            expirationTimestamp = expiration
        )

        // Elapsed: 20 hours (20% of time)
        // Consumed: 5 GB (50% of data, > 10 MB)
        // Index: 0.50 / 0.20 = 2.5 > 1.25 -> BURNING_FAST
        val current = start + 20 * 3_600_000L
        val used = 5L * 1024L * 1024L * 1024L

        val forecast = engine.calculateForecast(promo, used, current)

        assertEquals(BurnPace.BURNING_FAST, forecast.pace)
        assertTrue(forecast.burnStatusIndex!! > 1.25)
        assertNotNull(forecast.estimatedDepletionTimestamp)
        assertTrue(forecast.estimatedDepletionTimestamp!! < expiration)
        assertTrue(forecast.plainLanguageSummary.contains("before promo expires"))
    }

    @Test
    fun `stabilized conservative burn pace produces CONSERVATIVE and optimal pace summary`() {
        val start = 1_000_000L
        val validityHours = 100L
        val expiration = start + validityHours * 3_600_000L
        val allowance = 10L * 1024L * 1024L * 1024L

        val promo = Promo(
            name = "Smart Power All 99",
            totalAllowanceBytes = allowance,
            startTimestamp = start,
            expirationTimestamp = expiration
        )

        // Elapsed: 50 hours (50% of time)
        // Consumed: 1 GB (10% of data, > 10 MB)
        // Index: 0.10 / 0.50 = 0.2 < 0.75 -> CONSERVATIVE
        val current = start + 50 * 3_600_000L
        val used = 1L * 1024L * 1024L * 1024L

        val forecast = engine.calculateForecast(promo, used, current)

        assertEquals(BurnPace.CONSERVATIVE, forecast.pace)
        assertTrue(forecast.burnStatusIndex!! < 0.75)
        assertTrue(forecast.plainLanguageSummary.contains("Pace is optimal"))
    }

    @Test
    fun `stabilized on track pace produces ON_TRACK`() {
        val start = 1_000_000L
        val validityHours = 100L
        val expiration = start + validityHours * 3_600_000L
        val allowance = 10L * 1024L * 1024L * 1024L

        val promo = Promo(
            name = "Smart Power All 99",
            totalAllowanceBytes = allowance,
            startTimestamp = start,
            expirationTimestamp = expiration
        )

        // Elapsed: 50 hours (50% of time)
        // Consumed: 5 GB (50% of data, > 10 MB)
        // Index: 0.50 / 0.50 = 1.0 -> ON_TRACK
        val current = start + 50 * 3_600_000L
        val used = 5L * 1024L * 1024L * 1024L

        val forecast = engine.calculateForecast(promo, used, current)

        assertEquals(BurnPace.ON_TRACK, forecast.pace)
        assertEquals(1.0, forecast.burnStatusIndex!!, 0.01)
        assertTrue(forecast.plainLanguageSummary.contains("On track"))
    }

    @Test
    fun `stabilized non-expiring promo computes depletion date without expiration comparison`() {
        val start = 1_000_000L
        val promo = Promo(
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 20L * 1024L * 1024L * 1024L, // 20 GB
            startTimestamp = start,
            expirationTimestamp = null
        )

        // Elapsed: 10 hours
        // Consumed: 2 GB (> 10 MB, Burn rate = 0.2 GB / hr)
        // Remaining: 18 GB -> Projected to last: 18 / 0.2 = 90 hours from now
        val current = start + 10 * 3_600_000L
        val used = 2L * 1024L * 1024L * 1024L

        val forecast = engine.calculateForecast(promo, used, current)

        assertEquals(BurnPace.ON_TRACK, forecast.pace)
        assertNull(forecast.burnStatusIndex)
        val expectedDepletion = current + 90 * 3_600_000L
        assertEquals(expectedDepletion, forecast.estimatedDepletionTimestamp)
        assertFalse(forecast.plainLanguageSummary.contains("before promo expires"))
        assertTrue(forecast.plainLanguageSummary.contains("At current steady pace, Smart Magic Data 399 will run out on"))
    }

    @Test
    fun `remaining balance entry conversion to initial usage offset calculates correct remaining data and depletion`() {
        val start = 1_000_000L
        val totalAllowance = 24L * 1024L * 1024L * 1024L // 24 GB total promo
        val currentRemainingEntered = 20L * 1024L * 1024L * 1024L // User checks *123# and enters 20 GB remaining
        val initialOffset = totalAllowance - currentRemainingEntered // 4 GB offset

        assertEquals(4L * 1024L * 1024L * 1024L, initialOffset)

        val promo = Promo(
            name = "Smart Magic Data 399",
            totalAllowanceBytes = totalAllowance,
            startTimestamp = start,
            expirationTimestamp = null,
            initialUsageOffsetBytes = initialOffset
        )

        // Elapsed: 10 hours of app tracking
        // Live device measured usage during these 10 hours: 2 GB (> 10 MB, Burn rate = 0.2 GB / hr)
        // Total data used: 4 GB (historical) + 2 GB (live) = 6 GB
        // Remaining data: 24 GB - 6 GB = 18 GB
        // Projected depletion: 18 GB / 0.2 GB/hr = 90 hours
        val current = start + 10 * 3_600_000L
        val liveUsed = 2L * 1024L * 1024L * 1024L

        val forecast = engine.calculateForecast(promo, liveUsed, current)

        assertEquals(6L * 1024L * 1024L * 1024L, forecast.dataUsedBytes)
        assertEquals(18L * 1024L * 1024L * 1024L, forecast.dataRemainingBytes)
        assertEquals(2L * 1024L * 1024L * 1024L / 10.0, forecast.burnRateBytesPerHour, 1.0)
        assertEquals(current + 90 * 3_600_000L, forecast.estimatedDepletionTimestamp)
        assertEquals(BurnPace.ON_TRACK, forecast.pace)
    }

    @Test
    fun `full allowance remaining entered converts to zero offset`() {
        val totalAllowance = 24L * 1024L * 1024L * 1024L
        val remainingEntered = 24L * 1024L * 1024L * 1024L
        val offset = totalAllowance - remainingEntered
        assertEquals(0L, offset)
    }

    @Test
    fun `zero remaining entered converts to full allowance offset and marks depleted`() {
        val start = 1_000_000L
        val allowance = 10L * 1024L * 1024L * 1024L
        val remainingEntered = 0L
        val offset = allowance - remainingEntered
        assertEquals(allowance, offset)

        val promo = Promo(
            name = "Fully Used",
            totalAllowanceBytes = allowance,
            startTimestamp = start,
            expirationTimestamp = null,
            initialUsageOffsetBytes = offset
        )

        val forecast = engine.calculateForecast(promo, 0L, start)

        assertEquals(allowance, forecast.dataUsedBytes)
        assertEquals(0L, forecast.dataRemainingBytes)
        assertTrue(forecast.isDepleted)
        assertEquals(BurnPace.DEPLETED, forecast.pace)
    }

    @Test
    fun `formatBytes formats correctly for GB and MB`() {
        assertEquals("2.0 GB", engine.formatBytes(2L * 1024L * 1024L * 1024L))
        assertEquals("500 MB", engine.formatBytes(500L * 1024L * 1024L))
    }

    @Test
    fun `plainLanguageSummary preserves 2-decimal precision during calibration for small nonzero usage`() {
        val start = 1_000_000L
        val totalAllowance = 24L * 1024L * 1024L * 1024L // 24 GB
        val promo = Promo(
            name = "Smart Magic Data 399",
            totalAllowanceBytes = totalAllowance,
            startTimestamp = start,
            expirationTimestamp = null
        )

        // 20 MB used within calibration window (< 1 hour)
        val used = 20L * 1024L * 1024L
        val forecast = engine.calculateForecast(promo, used, start + 30 * 60 * 1000L)

        assertEquals("Calibrating pace • 23.98 GB remaining.", forecast.plainLanguageSummary)
    }

    @Test
    fun `plainLanguageSummary uses standard 1-decimal precision for normal usage`() {
        val start = 1_000_000L
        val totalAllowance = 24L * 1024L * 1024L * 1024L // 24 GB
        val promo = Promo(
            name = "Smart Magic Data 399",
            totalAllowanceBytes = totalAllowance,
            startTimestamp = start,
            expirationTimestamp = null
        )

        // 6 GB used (18 GB remaining)
        val used = 6L * 1024L * 1024L * 1024L
        val forecast = engine.calculateForecast(promo, used, start + 30 * 60 * 1000L)

        assertEquals("Calibrating pace • 18.0 GB remaining.", forecast.plainLanguageSummary)
    }
}
