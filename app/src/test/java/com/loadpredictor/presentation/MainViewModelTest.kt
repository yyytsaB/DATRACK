package com.loadpredictor.presentation

import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.usecase.CheckUsagePermissionUseCase
import com.loadpredictor.domain.usecase.GetActiveBurnForecastUseCase
import com.loadpredictor.domain.usecase.GetActivePromoUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val activePromoFlow = MutableStateFlow<Promo?>(null)
    private var hasPermission = false

    private val fakePromoRepository = object : PromoRepository {
        override fun getActivePromo(): Flow<Promo?> = activePromoFlow
        override fun getAllPromos(): Flow<List<Promo>> = MutableStateFlow(emptyList())
        override fun getPromoById(id: Long): Flow<Promo?> = MutableStateFlow(null)
        override fun getActivePromoForSim(simSlot: SimSlot): Flow<Promo?> = MutableStateFlow(null)
        override suspend fun insertPromo(promo: Promo): Long = 1L
        override suspend fun updatePromo(promo: Promo) {}
        override suspend fun deletePromo(promo: Promo) {}
        override suspend fun setActivePromo(id: Long) {}
    }

    private val fakeUsageRepository = object : UsageRepository {
        override fun hasUsageAccess(): Boolean = hasPermission
        override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long = 100_000L
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
    fun `initial state reflects permission status and observes active promo and forecast`() = runTest {
        hasPermission = false
        val checkUsagePermissionUseCase = CheckUsagePermissionUseCase(fakeUsageRepository)
        val getActivePromoUseCase = GetActivePromoUseCase(fakePromoRepository)
        val getActiveBurnForecastUseCase = GetActiveBurnForecastUseCase(fakePromoRepository, fakeUsageRepository)

        val viewModel = MainViewModel(
            checkUsagePermissionUseCase = checkUsagePermissionUseCase,
            getActivePromoUseCase = getActivePromoUseCase,
            getActiveBurnForecastUseCase = getActiveBurnForecastUseCase
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUsagePermissionGranted)
        assertNull(viewModel.uiState.value.activePromo)
        assertEquals(BurnForecastResult.NoActivePromo, viewModel.uiState.value.forecastResult)
        assertFalse(viewModel.uiState.value.isLoading)

        // Simulate promo activation
        val promo = Promo(
            id = 1L,
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = 1000L,
            expirationTimestamp = 5000L,
            simSlot = SimSlot.SIM_1,
            isActive = true
        )
        activePromoFlow.value = promo

        advanceUntilIdle()

        assertEquals(promo, viewModel.uiState.value.activePromo)
        assertTrue(viewModel.uiState.value.forecastResult is BurnForecastResult.Success)

        // Simulate permission granted on return from settings
        hasPermission = true
        viewModel.checkPermission()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isUsagePermissionGranted)
    }

    @Test
    fun `refresh re-queries forecast and daily usage breakdown and updates state`() = runTest {
        hasPermission = true
        var queryCount = 0
        val trackingUsageRepo = object : UsageRepository {
            override fun hasUsageAccess(): Boolean = true
            override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long {
                queryCount++
                return 500_000L * queryCount
            }
            override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long) = listOf(
                com.loadpredictor.domain.model.UsageBucket(
                    startTimestamp = 1000L,
                    endTimestamp = 2000L,
                    rxBytes = 100L,
                    txBytes = 200L
                )
            )
        }

        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = 1000L,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1,
            isActive = true
        )
        activePromoFlow.value = promo

        val checkUsagePermissionUseCase = CheckUsagePermissionUseCase(trackingUsageRepo)
        val getActivePromoUseCase = GetActivePromoUseCase(fakePromoRepository)
        val getActiveBurnForecastUseCase = GetActiveBurnForecastUseCase(fakePromoRepository, trackingUsageRepo)
        val getDailyUsageBreakdownUseCase = com.loadpredictor.domain.usecase.GetDailyUsageBreakdownUseCase(trackingUsageRepo)

        val viewModel = MainViewModel(
            checkUsagePermissionUseCase = checkUsagePermissionUseCase,
            getActivePromoUseCase = getActivePromoUseCase,
            getActiveBurnForecastUseCase = getActiveBurnForecastUseCase,
            getDailyUsageBreakdownUseCase = getDailyUsageBreakdownUseCase
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.forecastResult is BurnForecastResult.Success)
        assertEquals(1, viewModel.uiState.value.dailyUsageBreakdown.size)

        // Trigger manual/ticker refresh
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.forecastResult is BurnForecastResult.Success)
        assertEquals(1, viewModel.uiState.value.dailyUsageBreakdown.size)
        assertTrue(queryCount >= 2)
    }
}
