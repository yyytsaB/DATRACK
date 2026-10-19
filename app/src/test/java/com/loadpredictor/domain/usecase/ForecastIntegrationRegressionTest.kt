package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.model.*
import com.loadpredictor.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ForecastIntegrationRegressionTest {
    private val day = 86400000L
    private val gb = 1073741824L
    @Test fun `expired promo never counts traffic after validity ends`() = runTest {
        val repo = mockk<PromoRepository>(relaxed = true)
        val usage = mockk<UsageRepository>()
        val promo = Promo(name = "Expired", totalAllowanceBytes = gb, startTimestamp = 0, expirationTimestamp = day)
        coEvery { usage.queryMobileUsageBytes(0, day) } returns 100L
        val result = GetActiveBurnForecastUseCase(repo, usage).execute(promo, day * 2) as BurnForecastResult.Success
        assertEquals(BurnPace.EXPIRED, result.forecast.pace)
        coVerify(exactly = 1) { usage.queryMobileUsageBytes(0, day) }
    }
    @Test fun `daily anchor reconciles risk when EMA predicts opposite outcome`() = runTest {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 10, 20)
        val now = today.atStartOfDay(zone).toInstant().toEpochMilli() + 12 * 3600000L
        val repo = mockk<PromoRepository>(relaxed = true)
        val usage = mockk<UsageRepository>()
        val buckets = (3L downTo 1L).map { n -> UsageBucket(
            today.minusDays(n).atStartOfDay(zone).toInstant().toEpochMilli(),
            today.minusDays(n - 1).atStartOfDay(zone).toInstant().toEpochMilli(), 100L * 1048576, 0) }
        coEvery { usage.queryMobileUsageBytes(any(), any()) } returns gb
        coEvery { usage.queryDailyUsageBreakdown(any(), any()) } returns buckets
        val promo = Promo(name = "Fixture", totalAllowanceBytes = 10 * gb, startTimestamp = now - 10 * day,
            expirationTimestamp = now + 3 * day, lastActiveBurnRate = gb.toDouble(), lastSyncDataUsedBytes = gb, lastSyncTimestamp = now)
        val useCase = GetActiveBurnForecastUseCase(repo, usage, getDailyUsageBreakdownUseCase = GetDailyUsageBreakdownUseCase(usage))
        val forecast = (useCase.execute(promo, now) as BurnForecastResult.Success).forecast
        assertTrue(forecast.estimatedDepletionTimestamp!! > promo.expirationTimestamp!!)
        assertEquals(BurnPace.CONSERVATIVE, forecast.pace)
        assertTrue(forecast.plainLanguageSummary.startsWith("Expected to last"))
    }
    @Test fun `cancellation is never reported as a normal error`() = runTest {
        val usage = mockk<UsageRepository>()
        coEvery { usage.queryMobileUsageBytes(any(), any()) } throws CancellationException("cancelled")
        try {
            GetActiveBurnForecastUseCase(mockk(relaxed = true), usage).execute(Promo(name = "Fixture", totalAllowanceBytes = gb, startTimestamp = 0), day)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
