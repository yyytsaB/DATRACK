package com.loadpredictor.domain.usecase

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
    fun `invoke queries breakdown strictly from promo start when start is within 30 days`() = runTest {
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

        val result = useCase(promo)

        assertEquals(2, result.size)
        assertEquals(expectedBuckets, result)
        coVerify(exactly = 1) {
            usageRepository.queryDailyUsageBreakdown(promoStart, now)
        }
    }

    @Test
    fun `regression - promo under 24 hours old does not include prior days before startTimestamp`() = runTest {
        val now = 100_000_000L
        val timeProvider = FakeTimeProvider(now)
        val useCase = GetDailyUsageBreakdownUseCase(usageRepository, timeProvider)

        // Promo registered 2 hours ago (well under 24 hours)
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

        val result = useCase(promo)

        assertEquals(1, result.size)
        assertEquals(singleDayBucket, result)
        // Must query strictly from promoStart (2 hours ago), NOT 7 or 30 days ago
        coVerify(exactly = 1) {
            usageRepository.queryDailyUsageBreakdown(promoStart, now)
        }
    }

    @Test
    fun `invoke caps start query window to max 30 days ago when promo started earlier`() = runTest {
        val now = 100_000_000_000L
        val thirtyDaysMillis = 30L * 24L * 60L * 60L * 1000L
        val timeProvider = FakeTimeProvider(now)
        val useCase = GetDailyUsageBreakdownUseCase(usageRepository, timeProvider)

        val promoStart = now - (60L * 24L * 60L * 60L * 1000L) // 60 days ago
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

        val result = useCase(promo)

        assertTrue(result.isEmpty())
        coVerify(exactly = 1) {
            usageRepository.queryDailyUsageBreakdown(expectedStart, now)
        }
    }
}
