package com.loadpredictor.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.loadpredictor.domain.model.BurnForecast
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.usecase.GetActiveBurnForecastUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.loadpredictor.domain.usecase.GetActivePromoUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getActivePromoUseCase: GetActivePromoUseCase = mockk(relaxed = true)
    private val getActiveBurnForecastUseCase: GetActiveBurnForecastUseCase = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val appWidgetManager: AppWidgetManager = mockk(relaxed = true)

    private val forecastFlow = MutableStateFlow<BurnForecastResult>(BurnForecastResult.NoActivePromo)
    private val promoFlow = MutableStateFlow<Promo?>(null)

    private val testPromo = Promo(
        id = 1L,
        name = "Smart Magic Data 99",
        totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
        startTimestamp = System.currentTimeMillis() - 86400000L,
        expirationTimestamp = null,
        simSlot = SimSlot.SIM_1
    )

    private val testForecast = BurnForecast(
        promo = testPromo,
        dataUsedBytes = 500L * 1024L * 1024L,
        dataRemainingBytes = 1500L * 1024L * 1024L,
        burnRateBytesPerHour = 20.0 * 1024.0 * 1024.0,
        estimatedDepletionTimestamp = System.currentTimeMillis() + 86400000L * 3,
        burnStatusIndex = 0.8,
        pace = BurnPace.ON_TRACK,
        plainLanguageSummary = "⚡ On Track • 1.5 GB remaining",
        isDepleted = false,
        timeRemainingMillis = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManager
        every { context.getSystemService(Context.APPWIDGET_SERVICE) } returns appWidgetManager
        every { appWidgetManager.isRequestPinAppWidgetSupported } returns true
        every { getActiveBurnForecastUseCase() } returns forecastFlow
        every { getActivePromoUseCase() } returns promoFlow
    }

    @After
    fun tearDown() {
        unmockkStatic(AppWidgetManager::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState observes null forecast when no active promo exists`() = runTest {
        forecastFlow.value = BurnForecastResult.NoActivePromo
        val viewModel = WidgetsViewModel(getActivePromoUseCase, getActiveBurnForecastUseCase, context)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.activeForecast)
        assertFalse(state.isLoading)
        assertTrue(state.isPinSupported)
    }

    @Test
    fun `initial uiState observes active forecast when active promo exists`() = runTest {
        forecastFlow.value = BurnForecastResult.Success(testForecast)
        val viewModel = WidgetsViewModel(getActivePromoUseCase, getActiveBurnForecastUseCase, context)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.activeForecast)
        assertEquals(testPromo.name, state.activeForecast?.promo?.name)
        assertEquals(BurnPace.ON_TRACK, state.activeForecast?.pace)
        assertFalse(state.isLoading)
    }

    @Test
    fun `refresh updates active forecast`() = runTest {
        forecastFlow.value = BurnForecastResult.NoActivePromo
        promoFlow.value = testPromo
        coEvery { getActiveBurnForecastUseCase.execute(testPromo) } returns BurnForecastResult.Success(testForecast)

        val viewModel = WidgetsViewModel(getActivePromoUseCase, getActiveBurnForecastUseCase, context)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.activeForecast)

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.activeForecast)
        assertEquals(testPromo.name, state.activeForecast?.promo?.name)
    }

    @Test
    fun `requestPinWidget dispatches request to AppWidgetManager when supported`() {
        every { appWidgetManager.isRequestPinAppWidgetSupported } returns true
        every { appWidgetManager.requestPinAppWidget(any(), any(), any()) } returns true

        val viewModel = WidgetsViewModel(getActivePromoUseCase, getActiveBurnForecastUseCase, context)
        val result = viewModel.requestPinWidget(context)

        assertTrue(result)
        verify(exactly = 1) {
            appWidgetManager.requestPinAppWidget(any<ComponentName>(), null, null)
        }
    }

    @Test
    fun `requestPinWidget returns false when pinning is unsupported`() {
        every { appWidgetManager.isRequestPinAppWidgetSupported } returns false

        val viewModel = WidgetsViewModel(getActivePromoUseCase, getActiveBurnForecastUseCase, context)
        val result = viewModel.requestPinWidget(context)

        assertFalse(result)
        verify(exactly = 0) {
            appWidgetManager.requestPinAppWidget(any(), any(), any())
        }
    }
}
