package com.loadpredictor.presentation

import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.usecase.CheckUsagePermissionUseCase
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
        override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long = 0L
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
    fun `initial state reflects permission status and observes active promo`() = runTest {
        hasPermission = false
        val checkUsagePermissionUseCase = CheckUsagePermissionUseCase(fakeUsageRepository)
        val getActivePromoUseCase = GetActivePromoUseCase(fakePromoRepository)

        val viewModel = MainViewModel(
            checkUsagePermissionUseCase = checkUsagePermissionUseCase,
            getActivePromoUseCase = getActivePromoUseCase
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUsagePermissionGranted)
        assertNull(viewModel.uiState.value.activePromo)
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

        // Simulate permission granted on return from settings
        hasPermission = true
        viewModel.checkPermission()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isUsagePermissionGranted)
    }
}
