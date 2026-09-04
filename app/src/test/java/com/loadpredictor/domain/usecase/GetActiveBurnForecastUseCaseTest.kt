package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.model.UsageAccessDeniedException
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.time.TimeProvider
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetActiveBurnForecastUseCaseTest {

    private class FakeTimeProvider(var currentTime: Long = 10_000_000L) : TimeProvider {
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
        override suspend fun updateSyncState(promoId: Long, burnRate: Double?, dataUsedBytes: Long, syncTimestamp: Long) {
            activePromoFlow.value?.let { current ->
                if (current.id == promoId) {
                    activePromoFlow.value = current.copy(
                        lastActiveBurnRate = burnRate,
                        lastSyncDataUsedBytes = dataUsedBytes,
                        lastSyncTimestamp = syncTimestamp
                    )
                }
            }
        }
    }

    private class FakeUsageRepository(
        var usageBytes: Long = 1024L * 1024L * 500L,
        var exceptionToThrow: Throwable? = null
    ) : UsageRepository {
        override fun hasUsageAccess(): Boolean = true
        override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long {
            exceptionToThrow?.let { throw it }
            return usageBytes
        }
        override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket> = emptyList()
    }

    @Test
    fun `emits NoActivePromo when no promo is active`() = runTest {
        val promoRepo = FakePromoRepository(activePromo = null)
        val usageRepo = FakeUsageRepository()
        val useCase = GetActiveBurnForecastUseCase(promoRepo, usageRepo)

        val result = useCase().first()
        assertTrue(result is BurnForecastResult.NoActivePromo)
    }

    @Test
    fun `emits PermissionRequired when usage query throws UsageAccessDeniedException`() = runTest {
        val promo = Promo(
            id = 1L,
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = 1_000_000L,
            expirationTimestamp = 5_000_000L,
            isActive = true
        )
        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = FakeUsageRepository(exceptionToThrow = UsageAccessDeniedException("Access denied"))
        val useCase = GetActiveBurnForecastUseCase(promoRepo, usageRepo)

        val result = useCase().first()
        assertTrue(result is BurnForecastResult.PermissionRequired)
    }

    @Test
    fun `emits Error when usage query throws unexpected IOException`() = runTest {
        val promo = Promo(
            id = 1L,
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = 1_000_000L,
            expirationTimestamp = 5_000_000L,
            isActive = true
        )
        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = FakeUsageRepository(exceptionToThrow = IOException("Corrupted query index"))
        val useCase = GetActiveBurnForecastUseCase(promoRepo, usageRepo)

        val result = useCase().first()
        assertTrue(result is BurnForecastResult.Error)
        assertEquals("Corrupted query index", (result as BurnForecastResult.Error).message)
    }

    @Test
    fun `emits Success with computed forecast when active promo and valid usage exist`() = runTest {
        val start = 1_000_000L
        val current = start + 5 * 3_600_000L // 5 hours elapsed
        val promo = Promo(
            id = 1L,
            name = "Smart Power All 99",
            totalAllowanceBytes = 8L * 1024L * 1024L * 1024L,
            startTimestamp = start,
            expirationTimestamp = start + 7 * 24 * 3_600_000L,
            isActive = true
        )
        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = FakeUsageRepository(usageBytes = 1024L * 1024L * 1024L) // 1 GB used
        val timeProvider = FakeTimeProvider(currentTime = current)
        val useCase = GetActiveBurnForecastUseCase(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            burnRateEngine = BurnRateEngine(),
            timeProvider = timeProvider
        )

        val result = useCase().first()
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast
        assertEquals("Smart Power All 99", forecast.promo.name)
        assertEquals(7L * 1024L * 1024L * 1024L, forecast.dataRemainingBytes)
        assertEquals(BurnPace.BURNING_FAST, forecast.pace)
    }

    @Test
    fun `anchoring fires when at least 3 completed daily buckets exist`() = runTest {
        val current = 1_700_000_000_000L // arbitrary current time
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = current
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis

        // 3 completed days: each used 209 MB
        val dailyBytes = 209L * 1024L * 1024L
        val completedBuckets = listOf(
            UsageBucket(todayStart - 3 * 86_400_000L, todayStart - 2 * 86_400_000L, dailyBytes, 0L),
            UsageBucket(todayStart - 2 * 86_400_000L, todayStart - 1 * 86_400_000L, dailyBytes, 0L),
            UsageBucket(todayStart - 1 * 86_400_000L, todayStart, dailyBytes, 0L)
        )

        // Inflated EMA rate = 21.3 MB/hr (~512 MB/day)
        val inflatedEmaRate = 21.3 * 1024.0 * 1024.0
        val usedBytes = 8L * 1024L * 1024L * 1024L // 8 GB used of 24 GB -> 16 GB remaining
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = current - 10 * 86_400_000L,
            expirationTimestamp = null,
            lastActiveBurnRate = inflatedEmaRate,
            lastSyncDataUsedBytes = usedBytes,
            lastSyncTimestamp = current - 3_600_000L,
            isActive = true
        )

        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = object : UsageRepository {
            override fun hasUsageAccess(): Boolean = true
            override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long = usedBytes
            override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket> = completedBuckets
        }
        val timeProvider = FakeTimeProvider(currentTime = current)
        val dailyBreakdownUseCase = GetDailyUsageBreakdownUseCase(usageRepo, timeProvider)

        val useCase = GetActiveBurnForecastUseCase(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            burnRateEngine = BurnRateEngine(),
            timeProvider = timeProvider,
            getDailyUsageBreakdownUseCase = dailyBreakdownUseCase
        )

        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        val expectedDailyMeanRate = dailyBytes.toDouble() / 24.0 // ~8.7 MB/hr
        assertEquals(expectedDailyMeanRate, forecast.burnRateBytesPerHour, 1.0)

        val remainingHours = forecast.dataRemainingBytes.toDouble() / expectedDailyMeanRate
        val expectedDepletion = current + (remainingHours * 3_600_000.0).toLong()
        assertEquals(expectedDepletion.toDouble(), forecast.estimatedDepletionTimestamp!!.toDouble(), 1000.0)

        // Confirm depletion date is far later than it would have been with the inflated EMA rate
        val emaDepletion = current + ((forecast.dataRemainingBytes.toDouble() / inflatedEmaRate) * 3_600_000.0).toLong()
        assertTrue(forecast.estimatedDepletionTimestamp!! > emaDepletion + 20 * 86_400_000L)
    }

    @Test
    fun `anchoring does not fire when fewer than 3 completed daily buckets exist`() = runTest {
        val current = 1_700_000_000_000L
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = current
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis

        // Only 2 completed days
        val dailyBytes = 209L * 1024L * 1024L
        val completedBuckets = listOf(
            UsageBucket(todayStart - 2 * 86_400_000L, todayStart - 1 * 86_400_000L, dailyBytes, 0L),
            UsageBucket(todayStart - 1 * 86_400_000L, todayStart, dailyBytes, 0L)
        )

        val storedEmaRate = 10.0 * 1024.0 * 1024.0
        val usedBytes = 5L * 1024L * 1024L * 1024L
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = current - 5 * 86_400_000L,
            expirationTimestamp = null,
            lastActiveBurnRate = storedEmaRate,
            lastSyncDataUsedBytes = usedBytes,
            lastSyncTimestamp = current - 60_000L, // 1 min ago -> delta < 5 min -> preserves stored EMA
            isActive = true
        )

        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = object : UsageRepository {
            override fun hasUsageAccess(): Boolean = true
            override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long = usedBytes
            override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket> = completedBuckets
        }
        val timeProvider = FakeTimeProvider(currentTime = current)
        val dailyBreakdownUseCase = GetDailyUsageBreakdownUseCase(usageRepo, timeProvider)

        val useCase = GetActiveBurnForecastUseCase(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            burnRateEngine = BurnRateEngine(),
            timeProvider = timeProvider,
            getDailyUsageBreakdownUseCase = dailyBreakdownUseCase
        )

        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        // When < 3 completed days, rate should be the EMA rate, NOT the 209 MB/day rate
        assertEquals(storedEmaRate, forecast.burnRateBytesPerHour, 1.0)
    }

    @Test
    fun `anchoring does not fire when getDailyUsageBreakdownUseCase is null`() = runTest {
        val current = 1_700_000_000_000L
        val storedEmaRate = 15.0 * 1024.0 * 1024.0
        val usedBytes = 5L * 1024L * 1024L * 1024L
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = current - 5 * 86_400_000L,
            expirationTimestamp = null,
            lastActiveBurnRate = storedEmaRate,
            lastSyncDataUsedBytes = usedBytes,
            lastSyncTimestamp = current - 60_000L,
            isActive = true
        )

        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = FakeUsageRepository(usageBytes = usedBytes)
        val timeProvider = FakeTimeProvider(currentTime = current)

        // 4-arg constructor: getDailyUsageBreakdownUseCase is null
        val useCase = GetActiveBurnForecastUseCase(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            burnRateEngine = BurnRateEngine(),
            timeProvider = timeProvider
        )

        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast
        assertEquals(storedEmaRate, forecast.burnRateBytesPerHour, 1.0)
    }

    @Test
    fun `anchoring is skipped when forecast pace is INSUFFICIENT_DATA`() = runTest {
        val current = 1_700_000_000_000L
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = current
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis

        // 5 completed days of history
        val dailyBytes = 209L * 1024L * 1024L
        val completedBuckets = (1..5).map { i ->
            UsageBucket(todayStart - i * 86_400_000L, todayStart - (i - 1) * 86_400_000L, dailyBytes, 0L)
        }

        // Fresh promo (< 1 hour elapsed, 5 MB used < 10 MB threshold -> INSUFFICIENT_DATA)
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = current - 30 * 60_000L, // 30 mins elapsed
            expirationTimestamp = null,
            lastActiveBurnRate = null,
            lastSyncDataUsedBytes = 0L,
            lastSyncTimestamp = 0L,
            isActive = true
        )

        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = object : UsageRepository {
            override fun hasUsageAccess(): Boolean = true
            override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long = 5L * 1024L * 1024L // 5 MB
            override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket> = completedBuckets
        }
        val timeProvider = FakeTimeProvider(currentTime = current)
        val dailyBreakdownUseCase = GetDailyUsageBreakdownUseCase(usageRepo, timeProvider)

        val useCase = GetActiveBurnForecastUseCase(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            burnRateEngine = BurnRateEngine(),
            timeProvider = timeProvider,
            getDailyUsageBreakdownUseCase = dailyBreakdownUseCase
        )

        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        assertEquals(BurnPace.INSUFFICIENT_DATA, forecast.pace)
        assertEquals(0.0, forecast.burnRateBytesPerHour, 0.001)
    }

    @Test
    fun `anchored_forecast_populates_interval_when_ge_5_days`() = runTest {
        val current = 1_700_000_000_000L
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = current
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis

        // 5 varied completed buckets
        val dailyMegabytes = listOf(100L, 150L, 200L, 250L, 300L)
        val completedBuckets = dailyMegabytes.mapIndexed { index, mb ->
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

        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = object : UsageRepository {
            override fun hasUsageAccess(): Boolean = true
            override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long = 5L * 1024L * 1024L * 1024L
            override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket> = completedBuckets
        }
        val timeProvider = FakeTimeProvider(currentTime = current)
        val dailyBreakdownUseCase = GetDailyUsageBreakdownUseCase(usageRepo, timeProvider)

        val useCase = GetActiveBurnForecastUseCase(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            burnRateEngine = BurnRateEngine(),
            timeProvider = timeProvider,
            getDailyUsageBreakdownUseCase = dailyBreakdownUseCase
        )

        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        assertNotNull(forecast.depletionEarlyTimestamp)
        assertNotNull(forecast.depletionLateTimestamp)
        assertTrue(forecast.depletionEarlyTimestamp!! < forecast.estimatedDepletionTimestamp!!)
        assertTrue(forecast.estimatedDepletionTimestamp!! < forecast.depletionLateTimestamp!!)
    }

    @Test
    fun `anchored_forecast_no_interval_when_lt_5_days`() = runTest {
        val current = 1_700_000_000_000L
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = current
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis

        // 3 completed buckets (>= 3 for mean anchor, but < 5 for interval)
        val dailyMegabytes = listOf(150L, 200L, 250L)
        val completedBuckets = dailyMegabytes.mapIndexed { index, mb ->
            val dayOffset = (3 - index).toLong()
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
            startTimestamp = current - 5 * 86_400_000L,
            expirationTimestamp = null,
            isActive = true
        )

        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = object : UsageRepository {
            override fun hasUsageAccess(): Boolean = true
            override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long = 5L * 1024L * 1024L * 1024L
            override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket> = completedBuckets
        }
        val timeProvider = FakeTimeProvider(currentTime = current)
        val dailyBreakdownUseCase = GetDailyUsageBreakdownUseCase(usageRepo, timeProvider)

        val useCase = GetActiveBurnForecastUseCase(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            burnRateEngine = BurnRateEngine(),
            timeProvider = timeProvider,
            getDailyUsageBreakdownUseCase = dailyBreakdownUseCase
        )

        val result = useCase.execute(promo)
        assertTrue(result is BurnForecastResult.Success)
        val forecast = (result as BurnForecastResult.Success).forecast

        // Daily mean IS anchored
        val expectedRate = (200L * 1024L * 1024L).toDouble() / 24.0
        assertEquals(expectedRate, forecast.burnRateBytesPerHour, 1.0)

        // Interval is null because size (3) < MIN_INTERVAL_DAYS (5)
        assertNull(forecast.depletionEarlyTimestamp)
        assertNull(forecast.depletionLateTimestamp)
    }
}
