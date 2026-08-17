package com.loadpredictor.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromoTest {

    @Test
    fun `valid expiring promo initializes properly and computes duration and expiration`() {
        val start = 1_000_000L
        val end = 2_000_000L
        val allowanceBytes = 5L * 1024L * 1024L * 1024L // 5 GB

        val promo = Promo(
            id = 1L,
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = allowanceBytes,
            startTimestamp = start,
            expirationTimestamp = end,
            initialUsageOffsetBytes = 1024L * 1024L * 500L, // 500 MB offset
            simSlot = SimSlot.SIM_1,
            isActive = true
        )

        assertEquals("Smart GigaSurf 99", promo.name)
        assertEquals(allowanceBytes, promo.totalAllowanceBytes)
        assertEquals(1024L * 1024L * 500L, promo.initialUsageOffsetBytes)
        assertEquals(1_000_000L, promo.totalDurationMillis)
        assertFalse(promo.isNoExpiry)
        assertFalse(promo.isExpired(1_500_000L))
        assertTrue(promo.isExpired(2_000_000L))
        assertTrue(promo.isExpired(2_500_000L))
    }

    @Test
    fun `valid non-expiring promo initializes properly without expiration`() {
        val start = 1_700_000_000_000L
        val allowanceBytes = 24L * 1024L * 1024L * 1024L // 24 GB

        val promo = Promo(
            id = 2L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = allowanceBytes,
            startTimestamp = start,
            expirationTimestamp = null,
            initialUsageOffsetBytes = 0L,
            simSlot = SimSlot.SIM_1,
            isActive = true
        )

        assertEquals("Smart Magic Data 399", promo.name)
        assertEquals(allowanceBytes, promo.totalAllowanceBytes)
        assertEquals(0L, promo.initialUsageOffsetBytes)
        assertNull(promo.totalDurationMillis)
        assertTrue(promo.isNoExpiry)
        assertNull(promo.expirationTimestamp)
        assertFalse(promo.isExpired(start + 1_000_000_000L))
        assertFalse(promo.isExpired(Long.MAX_VALUE))
    }

    @Test
    fun `promo with initialUsageOffsetBytes equal to total allowance initializes validly`() {
        val allowance = 10L * 1024L * 1024L
        val promo = Promo(
            name = "Fully Used Start",
            totalAllowanceBytes = allowance,
            startTimestamp = 1000L,
            expirationTimestamp = null,
            initialUsageOffsetBytes = allowance
        )
        assertEquals(allowance, promo.initialUsageOffsetBytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `promo with negative initialUsageOffsetBytes throws IllegalArgumentException`() {
        Promo(
            name = "Negative Offset",
            totalAllowanceBytes = 5_000_000L,
            startTimestamp = 1_000L,
            expirationTimestamp = null,
            initialUsageOffsetBytes = -1L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `promo with initialUsageOffsetBytes exceeding total allowance throws IllegalArgumentException`() {
        Promo(
            name = "Excessive Offset",
            totalAllowanceBytes = 5_000_000L,
            startTimestamp = 1_000L,
            expirationTimestamp = null,
            initialUsageOffsetBytes = 5_000_001L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-expiring promo with blank name throws IllegalArgumentException`() {
        Promo(
            name = "   ",
            totalAllowanceBytes = 5_000_000L,
            startTimestamp = 1_000L,
            expirationTimestamp = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-expiring promo with zero allowance throws IllegalArgumentException`() {
        Promo(
            name = "Smart Magic Data Zero",
            totalAllowanceBytes = 0L,
            startTimestamp = 1_000L,
            expirationTimestamp = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-expiring promo with negative allowance throws IllegalArgumentException`() {
        Promo(
            name = "Smart Magic Data Negative",
            totalAllowanceBytes = -1024L,
            startTimestamp = 1_000L,
            expirationTimestamp = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `expiring promo with expiration before start throws IllegalArgumentException`() {
        Promo(
            name = "Invalid Dates",
            totalAllowanceBytes = 1000L,
            startTimestamp = 200L,
            expirationTimestamp = 100L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `expiring promo with expiration equal to start throws IllegalArgumentException`() {
        Promo(
            name = "Invalid Same Dates",
            totalAllowanceBytes = 1000L,
            startTimestamp = 200L,
            expirationTimestamp = 200L
        )
    }
}
