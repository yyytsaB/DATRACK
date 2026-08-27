package com.loadpredictor.presentation.promo

import com.loadpredictor.domain.model.BurnForecast
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.usecase.GetActiveBurnForecastUseCase
import com.loadpredictor.domain.usecase.GetActivePromoUseCase
import com.loadpredictor.domain.usecase.SavePromoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PromoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val allPromosFlow = MutableStateFlow<List<Promo>>(emptyList())
    private val activePromoFlow = MutableStateFlow<Promo?>(null)
    var lastInsertedPromo: Promo? = null
    var lastUpdatedPromo: Promo? = null

    private val fakePromoRepository = object : PromoRepository {
        override fun getActivePromo(): Flow<Promo?> = activePromoFlow
        override fun getAllPromos(): Flow<List<Promo>> = allPromosFlow
        override fun getPromoById(id: Long): Flow<Promo?> = MutableStateFlow(null)
        override fun getActivePromoForSim(simSlot: SimSlot): Flow<Promo?> = MutableStateFlow(null)
        override suspend fun insertPromo(promo: Promo): Long {
            lastInsertedPromo = promo
            return 1L
        }
        override suspend fun updatePromo(promo: Promo) {
            lastUpdatedPromo = promo
        }
        override suspend fun deletePromo(promo: Promo) {}
        override suspend fun setActivePromo(id: Long) {}
        override suspend fun updateSyncState(promoId: Long, burnRate: Double?, dataUsedBytes: Long, syncTimestamp: Long) {}
    }

    private val fakeUsageRepository = object : UsageRepository {
        override fun hasUsageAccess(): Boolean = true
        override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long = 300L * 1024L * 1024L // 300 MB
        override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long) = emptyList<com.loadpredictor.domain.model.UsageBucket>()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `active promo receives live forecast remaining balance from GetActiveBurnForecastUseCase`() = runTest {
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L, // 24 GB
            startTimestamp = 1_000_000L,
            initialUsageOffsetBytes = 6500L * 1024L * 1024L, // 6.5 GB
            isActive = true
        )
        activePromoFlow.value = promo
        allPromosFlow.value = listOf(promo)

        val savePromoUseCase = SavePromoUseCase(fakePromoRepository)
        val getActivePromoUseCase = GetActivePromoUseCase(fakePromoRepository)
        val getActiveBurnForecastUseCase = GetActiveBurnForecastUseCase(fakePromoRepository, fakeUsageRepository)

        val viewModel = PromoViewModel(
            promoRepository = fakePromoRepository,
            savePromoUseCase = savePromoUseCase,
            getActivePromoUseCase = getActivePromoUseCase,
            getActiveBurnForecastUseCase = getActiveBurnForecastUseCase
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.activeForecast)
        val forecast = state.activeForecast!!

        // Total used = 6.5 GB initial offset + 300 MB live measured = 6.8 GB used
        // Remaining = 24.0 GB - 6.8 GB = 17.2 GB remaining (18,468,306,944 bytes)
        val expectedUsedBytes = (6500L * 1024L * 1024L) + (300L * 1024L * 1024L)
        val expectedRemainingBytes = promo.totalAllowanceBytes - expectedUsedBytes

        assertEquals(expectedRemainingBytes, forecast.dataRemainingBytes)
        assertEquals(expectedUsedBytes, forecast.dataUsedBytes)
    }

    @Test
    fun `inactive promo retains snapshot state when no active forecast applies`() = runTest {
        val inactivePromo = Promo(
            id = 2L,
            name = "Smart Power All 99",
            totalAllowanceBytes = 10L * 1024L * 1024L * 1024L,
            startTimestamp = 1_000_000L,
            initialUsageOffsetBytes = 1L * 1024L * 1024L * 1024L,
            lastSyncDataUsedBytes = 2L * 1024L * 1024L * 1024L, // 2 GB used at last snapshot
            lastSyncTimestamp = 5_000_000L,
            isActive = false
        )
        allPromosFlow.value = listOf(inactivePromo)
        activePromoFlow.value = null

        val savePromoUseCase = SavePromoUseCase(fakePromoRepository)
        val getActivePromoUseCase = GetActivePromoUseCase(fakePromoRepository)
        val getActiveBurnForecastUseCase = GetActiveBurnForecastUseCase(fakePromoRepository, fakeUsageRepository)

        val viewModel = PromoViewModel(
            promoRepository = fakePromoRepository,
            savePromoUseCase = savePromoUseCase,
            getActivePromoUseCase = getActivePromoUseCase,
            getActiveBurnForecastUseCase = getActiveBurnForecastUseCase
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.activeForecast)
        assertEquals(1, state.promos.size)
        val p = state.promos[0]
        assertEquals(2L * 1024L * 1024L * 1024L, p.lastSyncDataUsedBytes)
        assertEquals(5_000_000L, p.lastSyncTimestamp)
    }

    @Test
    fun `savePromo with id zero inserts new promo with clean default sync state`() = runTest {
        val savePromoUseCase = SavePromoUseCase(fakePromoRepository)
        val getActivePromoUseCase = GetActivePromoUseCase(fakePromoRepository)
        val viewModel = PromoViewModel(
            promoRepository = fakePromoRepository,
            savePromoUseCase = savePromoUseCase,
            getActivePromoUseCase = getActivePromoUseCase
        )

        val now = 10_000_000L
        viewModel.savePromo(
            name = "GigaSurf 99",
            allowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = now,
            expirationTimestamp = now + 7 * 24 * 3_600_000L,
            initialUsageOffsetBytes = 0L,
            simSlot = SimSlot.SIM_1,
            isActive = true,
            id = 0L
        )

        advanceUntilIdle()

        assertNotNull(lastInsertedPromo)
        assertNull(lastUpdatedPromo)
        assertEquals("GigaSurf 99", lastInsertedPromo!!.name)
        assertEquals(2L * 1024L * 1024L * 1024L, lastInsertedPromo!!.totalAllowanceBytes)
        assertEquals(0L, lastInsertedPromo!!.lastSyncDataUsedBytes)
        assertEquals(0L, lastInsertedPromo!!.lastSyncTimestamp)
        assertNull(lastInsertedPromo!!.lastActiveBurnRate)
    }

    @Test
    fun `savePromo with existing id updates promo, preserves lastSyncDataUsedBytes and lastSyncTimestamp, and nulls lastActiveBurnRate`() = runTest {
        val existingPromo = Promo(
            id = 42L,
            name = "Old Name",
            totalAllowanceBytes = 10L * 1024L * 1024L * 1024L,
            startTimestamp = 1_000_000L,
            expirationTimestamp = 1_000_000L + 7 * 24 * 3_600_000L,
            initialUsageOffsetBytes = 500L * 1024L * 1024L,
            simSlot = SimSlot.SIM_1,
            isActive = true,
            lastActiveBurnRate = 15_000_000.0,
            lastSyncDataUsedBytes = 3_000_000_000L,
            lastSyncTimestamp = 4_000_000L
        )

        val savePromoUseCase = SavePromoUseCase(fakePromoRepository)
        val getActivePromoUseCase = GetActivePromoUseCase(fakePromoRepository)
        val viewModel = PromoViewModel(
            promoRepository = fakePromoRepository,
            savePromoUseCase = savePromoUseCase,
            getActivePromoUseCase = getActivePromoUseCase
        )

        viewModel.savePromo(
            name = "Smart Magic Data 399",
            allowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = existingPromo.startTimestamp,
            expirationTimestamp = null,
            initialUsageOffsetBytes = existingPromo.initialUsageOffsetBytes,
            simSlot = SimSlot.SIM_2,
            isActive = true,
            id = existingPromo.id,
            existingPromo = existingPromo
        )

        advanceUntilIdle()

        assertNull(lastInsertedPromo)
        assertNotNull(lastUpdatedPromo)
        val updated = lastUpdatedPromo!!
        assertEquals(42L, updated.id)
        assertEquals("Smart Magic Data 399", updated.name)
        assertEquals(24L * 1024L * 1024L * 1024L, updated.totalAllowanceBytes)
        assertEquals(SimSlot.SIM_2, updated.simSlot)
        assertEquals(existingPromo.startTimestamp, updated.startTimestamp)
        assertNull(updated.expirationTimestamp)
        assertEquals(existingPromo.initialUsageOffsetBytes, updated.initialUsageOffsetBytes)

        // Verifying delta anchors are preserved from existingPromo
        assertEquals(3_000_000_000L, updated.lastSyncDataUsedBytes)
        assertEquals(4_000_000L, updated.lastSyncTimestamp)

        // Verifying burn rate is reset to null for clean recalibration
        assertNull(updated.lastActiveBurnRate)
    }
}
