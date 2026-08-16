package com.loadpredictor.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromoTest {

    @Test
    fun `valid promo initializes properly and computes totalDurationMillis`() {
        val start = 1_000_000L
        val end = 2_000_000L
        val allowanceBytes = 5L * 1024L * 1024L * 1024L // 5 GB

        val promo = Promo(
            id = 1L,
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = allowanceBytes,
            startTimestamp = start,
            expirationTimestamp = end,
            simSlot = SimSlot.SIM_1,
            isActive = true
        )

        assertEquals("Smart GigaSurf 99", promo.name)
        assertEquals(allowanceBytes, promo.totalAllowanceBytes)
        assertEquals(1_000_000L, promo.totalDurationMillis)
        assertFalse(promo.isExpired(1_500_000L))
        assertTrue(promo.isExpired(2_000_000L))
        assertTrue(promo.isExpired(2_500_000L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `promo with blank name throws IllegalArgumentException`() {
        Promo(
            name = "  ",
            totalAllowanceBytes = 1000L,
            startTimestamp = 100L,
            expirationTimestamp = 200L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `promo with non-positive allowance throws IllegalArgumentException`() {
        Promo(
            name = "Invalid Allowance",
            totalAllowanceBytes = 0L,
            startTimestamp = 100L,
            expirationTimestamp = 200L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `promo with expiration before start throws IllegalArgumentException`() {
        Promo(
            name = "Invalid Dates",
            totalAllowanceBytes = 1000L,
            startTimestamp = 200L,
            expirationTimestamp = 100L
        )
    }
}
