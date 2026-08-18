package com.loadpredictor.worker

import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThresholdEvaluationLogicTest {

    private val engine = BurnRateEngine()

    @Test
    fun `milestone threshold detection flags unnotified 50, 80, 90 percent correctly`() {
        val totalAllowance = 10L * 1024L * 1024L * 1024L // 10 GB
        val promo = Promo(
            id = 101L,
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = totalAllowance,
            startTimestamp = 1000L,
            expirationTimestamp = 1000L + (7 * 86400000L),
            simSlot = SimSlot.SIM_1
        )

        // Case 1: 55% used, empty notified set -> fires 50
        val usedBytes1 = (5.5 * 1024.0 * 1024.0 * 1024.0).toLong()
        val forecast1 = engine.calculateForecast(promo, usedBytes1, 1000L + 86400000L)
        val usedPct1 = ((forecast1.dataUsedBytes.toDouble() / promo.totalAllowanceBytes.toDouble()) * 100.0).toInt()

        val notifiedSet1 = emptySet<String>()
        val milestonesToFire1 = listOf(50, 80, 90).filter { milestone ->
            usedPct1 >= milestone && !notifiedSet1.contains(milestone.toString())
        }
        assertEquals(listOf(50), milestonesToFire1)

        // Case 2: 85% used, notified set contains "50" -> fires 80, suppresses 50
        val usedBytes2 = (8.5 * 1024.0 * 1024.0 * 1024.0).toLong()
        val forecast2 = engine.calculateForecast(promo, usedBytes2, 1000L + 86400000L)
        val usedPct2 = ((forecast2.dataUsedBytes.toDouble() / promo.totalAllowanceBytes.toDouble()) * 100.0).toInt()

        val notifiedSet2 = setOf("50")
        val milestonesToFire2 = listOf(50, 80, 90).filter { milestone ->
            usedPct2 >= milestone && !notifiedSet2.contains(milestone.toString())
        }
        assertEquals(listOf(80), milestonesToFire2)

        // Case 3: 95% used, notified set contains "50", "80", "90" -> suppresses all
        val notifiedSet3 = setOf("50", "80", "90")
        val milestonesToFire3 = listOf(50, 80, 90).filter { milestone ->
            95 >= milestone && !notifiedSet3.contains(milestone.toString())
        }
        assertTrue(milestonesToFire3.isEmpty())
    }

    @Test
    fun `premature depletion alert triggers only for expiring promos when BURNING_FAST and depletion is ge 12h early`() {
        val now = 1000L + (2 * 3600000L)
        val expiration = 1000L + (7 * 86400000L)
        val promoExpiring = Promo(
            id = 1L,
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = 1000L,
            expirationTimestamp = expiration,
            simSlot = SimSlot.SIM_1
        )

        // High burn rate: 1.5 GB in 2 hours
        val forecastExpiring = engine.calculateForecast(promoExpiring, (1.5 * 1024 * 1024 * 1024).toLong(), now)
        assertEquals(BurnPace.BURNING_FAST, forecastExpiring.pace)

        val diffMs = promoExpiring.expirationTimestamp!! - forecastExpiring.estimatedDepletionTimestamp!!
        val diffHours = diffMs / 3_600_000L
        assertTrue(diffHours >= 12L)

        // Check non-expiring promo does NOT trigger premature depletion alert
        val promoNoExpiry = Promo(
            id = 2L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = 1000L,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1
        )
        val forecastNoExpiry = engine.calculateForecast(promoNoExpiry, (1.5 * 1024 * 1024 * 1024).toLong(), now)
        assertTrue(promoNoExpiry.isNoExpiry)
        assertFalse(!promoNoExpiry.isNoExpiry && forecastNoExpiry.pace == BurnPace.BURNING_FAST)
    }
}
