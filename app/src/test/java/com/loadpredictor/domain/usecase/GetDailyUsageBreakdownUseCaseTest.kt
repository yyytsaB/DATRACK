package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.model.HistoryTimeRange
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.time.TimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetDailyUsageBreakdownUseCaseTest {

    private val usageRepository: UsageRepository = mockk()

    private class FakeTimeProvider(var time: Long) : TimeProvider {
        override fun currentTimeMillis(): Long = time
    }

    @Test
    fun `invoke queries 7D breakdown bounded strictly by promo start`() = runTest {
        val now = 100_000_000L
        val timeProvider = FakeTimeProvider(now)
        val useCase = GetDailyUsageBreakdownUseCase(usageRepository, timeProvider)

        val promoStart = now - (5 * 24 * 60 * 60 * 1000L) // 5 days ago
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = promoStart,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1
        )

        val expectedBuckets = listOf(
            UsageBucket(promoStart, promoStart + 86400000L, 500_000L, 100_000L),
            UsageBucket(promoStart + 86400000L, now, 800_000L, 200_000L)
        )

        coEvery {
            usageRepository.queryDailyUsageBreakdown(promoStart, now)
        } returns expectedBuckets

        val result = useCase(promo, timeRange = HistoryTimeRange.LAST_7_DAYS)

        assertEquals(2, result.size)
        assertEquals(expectedBuckets, result)
        coVerify(exactly = 1) {
            usageRepository.queryDailyUsageBreakdown(promoStart, now)
        }
    }

    @Test
    fun `invoke queries 7D, 30D, and Lifetime producing three distinct windows on a 45-day-old promo`() = runTest {
        val now = 100_000_000_000L
        val sevenDaysMillis = 7L * 24L * 60L * 60L * 1000L
        val thirtyDaysMillis = 30L * 24L * 60L * 60L * 1000L
        val fortyFiveDaysMillis = 45L * 24L * 60L * 60L * 1000L
        val timeProvider = FakeTimeProvider(now)
        val useCase = GetDailyUsageBreakdownUseCase(usageRepository, timeProvider)

        val promoStart = now - fortyFiveDaysMillis // 45 days ago
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = promoStart,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1
        )

        val expected7dStart = now - sevenDaysMillis
        val expected30dStart = now - thirtyDaysMillis
        val expectedLifetimeStart = promoStart // 45 days ago (< 90d cap)

        coEvery { usageRepository.queryDailyUsageBreakdown(any(), any()) } returns emptyList()

        // 1. 7D Range -> now - 7 days
        useCase(promo, timeRange = HistoryTimeRange.LAST_7_DAYS)
        coVerify(exactly = 1) { usageRepository.queryDailyUsageBreakdown(expected7dStart, now) }

        // 2. 30D Range -> now - 30 days
        useCase(promo, timeRange = HistoryTimeRange.LAST_30_DAYS)
        coVerify(exactly = 1) { usageRepository.queryDailyUsageBreakdown(expected30dStart, now) }

        // 3. Lifetime Range -> promoStart (45 days ago, distinct from both 7d and 30d)
        useCase(promo, timeRange = HistoryTimeRange.LIFETIME)
        coVerify(exactly = 1) { usageRepository.queryDailyUsageBreakdown(expectedLifetimeStart, now) }
    }

    @Test
    fun `invoke queries 30D breakdown bounded by 30-day window when promo started 40 days ago`() = runTest {
        val now = 100_000_000_000L
        val thirtyDaysMillis = 30L * 24L * 60L * 60L * 1000L
        val timeProvider = FakeTimeProvider(now)
        val useCase = GetDailyUsageBreakdownUseCase(usageRepository, timeProvider)

        val promoStart = now - (40L * 24L * 60L * 60L * 1000L) // 40 days ago
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = promoStart,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1
        )

        val expectedStart = now - thirtyDaysMillis

        coEvery {
            usageRepository.queryDailyUsageBreakdown(expectedStart, now)
        } returns emptyList()

        val result = useCase(promo, timeRange = HistoryTimeRange.LAST_30_DAYS)

        assertTrue(result.isEmpty())
        coVerify(exactly = 1) {
            usageRepository.queryDailyUsageBreakdown(expectedStart, now)
        }
    }

    @Test
    fun `invoke queries Lifetime breakdown capped at 90-day app policy cap when promo started 120 days ago`() = runTest {
        val now = 100_000_000_000L
        val ninetyDaysMillis = 90L * 24L * 60L * 60L * 1000L
        val timeProvider = FakeTimeProvider(now)
        val useCase = GetDailyUsageBreakdownUseCase(usageRepository, timeProvider)

        val promoStart = now - (120L * 24L * 60L * 60L * 1000L) // 120 days ago
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = promoStart,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1
        )

        val expectedStart = now - ninetyDaysMillis

        coEvery {
            usageRepository.queryDailyUsageBreakdown(expectedStart, now)
        } returns emptyList()

        val result = useCase(promo, timeRange = HistoryTimeRange.LIFETIME)

        assertTrue(result.isEmpty())
        coVerify(exactly = 1) {
            usageRepository.queryDailyUsageBreakdown(expectedStart, now)
        }
    }

    @Test
    fun `regression - promo under 24 hours old does not include prior days regardless of range`() = runTest {
        val now = 100_000_000L
        val timeProvider = FakeTimeProvider(now)
        val useCase = GetDailyUsageBreakdownUseCase(usageRepository, timeProvider)

        // Promo registered 2 hours ago
        val promoStart = now - (2 * 60 * 60 * 1000L)
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = promoStart,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1
        )

        val singleDayBucket = listOf(
            UsageBucket(promoStart, now, 15_000L, 2_000L)
        )

        coEvery {
            usageRepository.queryDailyUsageBreakdown(promoStart, now)
        } returns singleDayBucket

        val result = useCase(promo, timeRange = HistoryTimeRange.LIFETIME)

        assertEquals(1, result.size)
        assertEquals(singleDayBucket, result)
        // Must query strictly from promoStart (2 hours ago), NOT 90 days ago
        coVerify(exactly = 1) {
            usageRepository.queryDailyUsageBreakdown(promoStart, now)
        }
    }
}
