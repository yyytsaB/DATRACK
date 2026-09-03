package com.loadpredictor.presentation.widget

import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.model.UsageAccessDeniedException
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSyncHelperTest {

    private val testNow = 1_700_000_000_000L

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
        var hasAccess: Boolean = true,
        var usageBytes: Long = 1024L * 1024L * 500L,
        var exceptionToThrow: Throwable? = null
    ) : UsageRepository {
        override fun hasUsageAccess(): Boolean = hasAccess
        override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long {
            exceptionToThrow?.let { throw it }
            return usageBytes
        }
        override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket> = emptyList()
    }

    @Test
    fun computeWidgetState_returnsPermissionRequired_whenUsageAccessNotGranted() = runTest {
        val promoRepo = FakePromoRepository()
        val usageRepo = FakeUsageRepository(hasAccess = false)

        val result = WidgetSyncHelper.computeWidgetState(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            now = testNow
        )

        assertEquals(WidgetState.PermissionRequired, result)
    }

    @Test
    fun computeWidgetState_returnsNoActivePromo_whenNoPromoActive() = runTest {
        val promoRepo = FakePromoRepository(activePromo = null)
        val usageRepo = FakeUsageRepository(hasAccess = true)

        val result = WidgetSyncHelper.computeWidgetState(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            now = testNow
        )

        assertEquals(WidgetState.NoActivePromo, result)
    }

    @Test
    fun computeWidgetState_returnsPermissionRequired_whenUsageQueryThrowsAccessDenied() = runTest {
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024 * 1024 * 1024,
            startTimestamp = testNow - 86_400_000L,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1,
            isActive = true
        )
        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = FakeUsageRepository(
            hasAccess = true,
            exceptionToThrow = UsageAccessDeniedException()
        )

        val result = WidgetSyncHelper.computeWidgetState(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            now = testNow
        )

        assertEquals(WidgetState.PermissionRequired, result)
    }

    @Test
    fun computeWidgetState_returnsSuccess_withIdenticalFieldsToBurnRateEngine() = runTest {
        val promo = Promo(
            id = 42L,
            name = "Smart Magic Data 99",
            totalAllowanceBytes = 2L * 1024 * 1024 * 1024,
            startTimestamp = testNow - 86_400_000L,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_2,
            isActive = true
        )
        val promoRepo = FakePromoRepository(activePromo = promo)
        val usageRepo = FakeUsageRepository(
            hasAccess = true,
            usageBytes = 500L * 1024 * 1024
        )
        val engine = BurnRateEngine()
        val expectedForecast = engine.calculateForecast(promo, 500L * 1024 * 1024, testNow)

        val result = WidgetSyncHelper.computeWidgetState(
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            burnRateEngine = engine,
            now = testNow
        )

        assertTrue(result is WidgetState.Success)
        val successState = result as WidgetState.Success
        assertEquals(expectedForecast.promo.name, successState.promoName)
        assertEquals(expectedForecast.promo.simSlot, successState.simSlot)
        assertEquals(expectedForecast.dataRemainingBytes, successState.remainingBytes)
        assertEquals(expectedForecast.promo.totalAllowanceBytes, successState.totalAllowanceBytes)
        assertEquals(expectedForecast.pace, successState.pace)
        assertEquals(expectedForecast.plainLanguageSummary, successState.plainLanguageSummary)
        assertEquals(expectedForecast.promo.isNoExpiry, successState.isNoExpiry)
        assertEquals(testNow, successState.lastUpdatedMillis)
        assertEquals(expectedForecast.estimatedDepletionTimestamp, successState.estimatedDepletionTimestamp)
    }
}
