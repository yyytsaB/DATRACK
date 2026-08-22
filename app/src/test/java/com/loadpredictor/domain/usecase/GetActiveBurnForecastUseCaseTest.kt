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
}
