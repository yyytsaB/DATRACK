package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.time.TimeProvider
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DepletionIntervalTest {

    private class FakeTimeProvider(var currentTime: Long = 1_700_000_000_000L) : TimeProvider {
        override fun currentTimeMillis(): Long = currentTime
    }

    private class FakePromoRepository(activePromo: Promo? = null) : PromoRepository {
        val activePromoFlow = MutableStateFlow(activePromo)

        override fun getActivePromo(): Flow<Promo?> = activePromoFlow
        override fun getAllPromos(): Flow<List<Promo>> = MutableStateFlow(emptyList())
        override fun getPromoById(id: Long): Flow<Promo?> = MutableStateFlow(null)
        override fun getActivePromoForSim(simSlot: SimSlot): Flow<Promo?> = MutableStateFlow(null)
        override suspend fun insertPromo(promo: Promo): Long = 1L
        override suspend fun updatePromo(promo: Promo) {}
        override suspend fun deletePromo(promo: Promo) {}
        override suspend fun setActivePromo(id: Long) {}
        override suspend fun updateSyncState(promoId: Long, burnRate: Double?, dataUsedBytes: Long, syncTimestamp: Long) {}
    }

    private fun getTodayStart(currentTime: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun createUseCase(
        promo: Promo,
        usageBytes: Long,
        buckets: List<UsageBucket>,
        currentTime: Long
    ): GetActiveBurnForecastUseCase {
        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = object : UsageRepository {
            override fun hasUsageAccess(): Boolean = true
            override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long = usageBytes
            override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket> = buckets
        }
        val timeProvider = FakeTimeProvider(currentTime = currentTime)
        val dailyBreakdownUseCase = GetDailyUsageBreakdownUseCase(usageRepo, timeProvider)

        return GetActiveBurnForecastUseCase(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            burnRateEngine = BurnRateEngine(),
            timeProvider = timeProvider,
            getDailyUsageBreakdownUseCase = dailyBreakdownUseCase
        )
    }

    @Test
    fun `interval_is_null_when_fewer_than_5_completed_days`() = runTest {
        val current = 1_700_000_000_000L
        val todayStart = getTodayStart(current)
        val dailyBytes = 200L * 1024L * 1024L

        // 4 completed buckets (< 5)
        val buckets = (1..4).map { i ->
            UsageBucket(todayStart - i * 86_400_000L, todayStart - (i - 1) * 86_400_000L, dailyBytes, 0L)
        }

        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = current - 10 * 86_400_000L,
            expirationTimestamp = null,
            isActive = true
        )

        val useCase = createUseCase(promo, usageBytes = 5L * 1024L * 1024L * 1024L, buckets, current)
        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        // Daily mean rate IS anchored (since 4 >= 3)
        assertEquals(dailyBytes.toDouble() / 24.0, forecast.burnRateBytesPerHour, 1.0)
        // But confidence interval requires >= 5 days
        assertNull(forecast.depletionEarlyTimestamp)
        assertNull(forecast.depletionLateTimestamp)
    }

    @Test
    fun `interval_computed_with_5_uniform_days`() = runTest {
        val current = 1_700_000_000_000L
        val todayStart = getTodayStart(current)
        val dailyBytes = 200L * 1024L * 1024L

        // 5 identical completed buckets -> stdDev = 0 -> halfSpread = 0 -> suppressed
        val buckets = (1..5).map { i ->
            UsageBucket(todayStart - i * 86_400_000L, todayStart - (i - 1) * 86_400_000L, dailyBytes, 0L)
        }

        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = current - 10 * 86_400_000L,
            expirationTimestamp = null,
            isActive = true
        )

        val useCase = createUseCase(promo, usageBytes = 5L * 1024L * 1024L * 1024L, buckets, current)
        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        assertNull(forecast.depletionEarlyTimestamp)
        assertNull(forecast.depletionLateTimestamp)
    }

    @Test
    fun `interval_computed_with_5_varied_days`() = runTest {
        val current = 1_700_000_000_000L
        val todayStart = getTodayStart(current)
        val dailyMegabytes = listOf(100L, 150L, 200L, 250L, 300L)

        val buckets = dailyMegabytes.mapIndexed { index, mb ->
            val dayOffset = (5 - index).toLong()
            UsageBucket(
                todayStart - dayOffset * 86_400_000L,
                todayStart - (dayOffset - 1) * 86_400_000L,
                mb * 1024L * 1024L,
                0L
            )
        }

        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = current - 10 * 86_400_000L,
            expirationTimestamp = null,
            isActive = true
        )

        val useCase = createUseCase(promo, usageBytes = 5L * 1024L * 1024L * 1024L, buckets, current)
        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        assertNotNull(forecast.estimatedDepletionTimestamp)
        assertNotNull(forecast.depletionEarlyTimestamp)
        assertNotNull(forecast.depletionLateTimestamp)

        val early = forecast.depletionEarlyTimestamp!!
        val anchored = forecast.estimatedDepletionTimestamp!!
        val late = forecast.depletionLateTimestamp!!

        // early bound runs out sooner (higher burn rate), late bound lasts longer (lower burn rate)
        assertTrue("Early timestamp ($early) should precede anchored timestamp ($anchored)", early < anchored)
        assertTrue("Anchored timestamp ($anchored) should precede late timestamp ($late)", anchored < late)
    }

    @Test
    fun `late_bound_capped_at_promo_expiry`() = runTest {
        val current = 1_700_000_000_000L
        val todayStart = getTodayStart(current)
        val dailyMegabytes = listOf(100L, 150L, 200L, 250L, 300L)

        val buckets = dailyMegabytes.mapIndexed { index, mb ->
            val dayOffset = (5 - index).toLong()
            UsageBucket(
                todayStart - dayOffset * 86_400_000L,
                todayStart - (dayOffset - 1) * 86_400_000L,
                mb * 1024L * 1024L,
                0L
            )
        }

        // Remaining data: 1 GB. Expiry: 6 days from now.
        // μ = 200 MB/day. Remaining = 1000 MB. Anchored depletion = 5 days.
        // σ ≈ 79 MB/day. Late rate = 121 MB/day. Uncapped late = 8.27 days (> 6 days expiry).
        val promoExpiry = current + 6 * 86_400_000L
        val promo = Promo(
            id = 1L,
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = current - 5 * 86_400_000L,
            expirationTimestamp = promoExpiry,
            isActive = true
        )

        val useCase = createUseCase(promo, usageBytes = 1L * 1024L * 1024L * 1024L, buckets, current)
        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        assertNotNull(forecast.depletionLateTimestamp)
        assertEquals(promoExpiry, forecast.depletionLateTimestamp)
        assertNotNull(forecast.depletionEarlyTimestamp)
        assertTrue(forecast.depletionEarlyTimestamp!! < forecast.depletionLateTimestamp!!)
    }

    @Test
    fun `interval_suppressed_when_sigma_exceeds_mean`() = runTest {
        val current = 1_700_000_000_000L
        val todayStart = getTodayStart(current)
        // High variance: four 0 MB days and one 2000 MB day -> mean = 400 MB, stdDev ≈ 894 MB > mean
        val dailyMegabytes = listOf(0L, 0L, 0L, 0L, 2000L)

        val buckets = dailyMegabytes.mapIndexed { index, mb ->
            val dayOffset = (5 - index).toLong()
            UsageBucket(
                todayStart - dayOffset * 86_400_000L,
                todayStart - (dayOffset - 1) * 86_400_000L,
                mb * 1024L * 1024L,
                0L
            )
        }

        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = current - 10 * 86_400_000L,
            expirationTimestamp = null,
            isActive = true
        )

        val useCase = createUseCase(promo, usageBytes = 5L * 1024L * 1024L * 1024L, buckets, current)
        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        assertNull(forecast.depletionEarlyTimestamp)
        assertNull(forecast.depletionLateTimestamp)
    }

    @Test
    fun `interval_suppressed_when_pace_is_INSUFFICIENT_DATA`() = runTest {
        val current = 1_700_000_000_000L
        val todayStart = getTodayStart(current)
        val dailyMegabytes = listOf(100L, 150L, 200L, 250L, 300L, 350L)

        val buckets = dailyMegabytes.mapIndexed { index, mb ->
            val dayOffset = (6 - index).toLong()
            UsageBucket(
                todayStart - dayOffset * 86_400_000L,
                todayStart - (dayOffset - 1) * 86_400_000L,
                mb * 1024L * 1024L,
                0L
            )
        }

        // Promo started 30 mins ago, used only 5 MB (< 10 MB) -> INSUFFICIENT_DATA
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = current - 30 * 60_000L,
            expirationTimestamp = null,
            isActive = true
        )

        val useCase = createUseCase(promo, usageBytes = 5L * 1024L * 1024L * 1024L, buckets, current)
        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        assertEquals(BurnPace.INSUFFICIENT_DATA, forecast.pace)
        assertNull(forecast.depletionEarlyTimestamp)
        assertNull(forecast.depletionLateTimestamp)
    }

    @Test
    fun `interval_suppressed_when_pace_is_DEPLETED`() = runTest {
        val current = 1_700_000_000_000L
        val todayStart = getTodayStart(current)
        val dailyMegabytes = listOf(100L, 150L, 200L, 250L, 300L, 350L)

        val buckets = dailyMegabytes.mapIndexed { index, mb ->
            val dayOffset = (6 - index).toLong()
            UsageBucket(
                todayStart - dayOffset * 86_400_000L,
                todayStart - (dayOffset - 1) * 86_400_000L,
                mb * 1024L * 1024L,
                0L
            )
        }

        // Remaining = 0 -> DEPLETED
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 10L * 1024L * 1024L * 1024L,
            startTimestamp = current - 10 * 86_400_000L,
            expirationTimestamp = null,
            isActive = true
        )

        val useCase = createUseCase(promo, usageBytes = 10L * 1024L * 1024L * 1024L, buckets, current)
        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        assertEquals(BurnPace.DEPLETED, forecast.pace)
        assertNull(forecast.depletionEarlyTimestamp)
        assertNull(forecast.depletionLateTimestamp)
    }

    @Test
    fun `interval_uses_same_buckets_as_mean_anchor`() = runTest {
        val current = 1_700_000_000_000L
        val todayStart = getTodayStart(current)

        // 10 completed buckets: first 3 are massive outliers, last 7 are normal
        val outlierMegabytes = listOf(9999L, 9999L, 9999L)
        val recentSevenMegabytes = listOf(100L, 150L, 200L, 250L, 300L, 350L, 400L)
        val allMegabytes = outlierMegabytes + recentSevenMegabytes

        val buckets = allMegabytes.mapIndexed { index, mb ->
            val dayOffset = (10 - index).toLong()
            UsageBucket(
                todayStart - dayOffset * 86_400_000L,
                todayStart - (dayOffset - 1) * 86_400_000L,
                mb * 1024L * 1024L,
                0L
            )
        }

        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = current - 15 * 86_400_000L,
            expirationTimestamp = null,
            isActive = true
        )

        val useCase = createUseCase(promo, usageBytes = 5L * 1024L * 1024L * 1024L, buckets, current)
        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        // Verify daily mean matches average of the 7 most recent buckets (250 MB/day)
        val expectedSevenMeanBytesPerDay = recentSevenMegabytes.map { it * 1024L * 1024L }.average()
        val expectedSevenMeanBytesPerHour = expectedSevenMeanBytesPerDay / 24.0
        assertEquals(expectedSevenMeanBytesPerHour, forecast.burnRateBytesPerHour, 1.0)

        // Verify stdDev and interval are derived from the 7 most recent buckets
        val recentBytes = recentSevenMegabytes.map { it * 1024.0 * 1024.0 }
        val variance = recentBytes.sumOf { (it - expectedSevenMeanBytesPerDay) * (it - expectedSevenMeanBytesPerDay) } / (7 - 1)
        val stdDev = kotlin.math.sqrt(variance)
        val earlyRate = (expectedSevenMeanBytesPerDay + stdDev) / 24.0
        val remainingBytes = 24L * 1024L * 1024L * 1024L - 5L * 1024L * 1024L * 1024L
        val expectedEarlyTs = current + (remainingBytes / earlyRate * 3_600_000.0).toLong()

        assertNotNull(forecast.depletionEarlyTimestamp)
        assertEquals(expectedEarlyTs, forecast.depletionEarlyTimestamp!!)
    }
}
