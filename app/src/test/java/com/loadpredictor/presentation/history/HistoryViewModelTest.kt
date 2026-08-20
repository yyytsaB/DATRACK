package com.loadpredictor.presentation.history

import com.loadpredictor.domain.model.HistoryTimeRange
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.usecase.GetActivePromoUseCase
import com.loadpredictor.domain.usecase.GetDailyUsageBreakdownUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val getActivePromoUseCase: GetActivePromoUseCase = mockk()
    private val getDailyUsageBreakdownUseCase: GetDailyUsageBreakdownUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load queries 7D range by default and selects latest bucket`() = runTest {
        val now = 100_000_000L
        val promoStart = now - (3 * 86_400_000L)
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = promoStart,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1
        )

        val bucket1 = UsageBucket(promoStart, promoStart + 86_400_000L, 100_000L, 20_000L)
        val bucket2 = UsageBucket(promoStart + 86_400_000L, now, 200_000L, 40_000L)
        val buckets = listOf(bucket1, bucket2)

        every { getActivePromoUseCase() } returns flowOf(promo)
        coEvery { getDailyUsageBreakdownUseCase(promo, HistoryTimeRange.LAST_7_DAYS) } returns buckets

        val viewModel = HistoryViewModel(getActivePromoUseCase, getDailyUsageBreakdownUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HistoryTimeRange.LAST_7_DAYS, state.selectedRange)
        assertEquals(2, state.dailyBuckets.size)
        assertEquals(bucket2.startTimestamp, state.selectedBucketTimestamp)
        assertFalse(state.isLoading)
        assertEquals(360_000L, state.totalBurntBytes)
        assertEquals(180_000L, state.dailyAverageBytes)
        assertEquals(bucket2, state.peakDayBucket)
    }

    @Test
    fun `setTimeRange updates selectedRange and re-queries breakdown`() = runTest {
        val now = 100_000_000L
        val promoStart = now - (20 * 86_400_000L)
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = promoStart,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1
        )

        val buckets7D = listOf(UsageBucket(now - 86_400_000L, now, 100_000L, 20_000L))
        val buckets30D = listOf(
            UsageBucket(now - (2 * 86_400_000L), now - 86_400_000L, 50_000L, 10_000L),
            UsageBucket(now - 86_400_000L, now, 100_000L, 20_000L)
        )

        every { getActivePromoUseCase() } returns flowOf(promo)
        coEvery { getDailyUsageBreakdownUseCase(promo, HistoryTimeRange.LAST_7_DAYS) } returns buckets7D
        coEvery { getDailyUsageBreakdownUseCase(promo, HistoryTimeRange.LAST_30_DAYS) } returns buckets30D

        val viewModel = HistoryViewModel(getActivePromoUseCase, getDailyUsageBreakdownUseCase)
        advanceUntilIdle()

        viewModel.setTimeRange(HistoryTimeRange.LAST_30_DAYS)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HistoryTimeRange.LAST_30_DAYS, state.selectedRange)
        assertEquals(2, state.dailyBuckets.size)
    }

    @Test
    fun `isNewlyRegistered identifies promo within 48 hours vs older promo`() {
        val now = 100_000_000L
        val freshPromo = Promo(
            id = 1L,
            name = "Fresh Promo",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = now - (24 * 3_600_000L), // 24 hours old
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1
        )

        val oldPromo = Promo(
            id = 2L,
            name = "Old Promo",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = now - (72 * 3_600_000L), // 72 hours old (3 days)
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1
        )

        val freshState = HistoryUiState(activePromo = freshPromo)
        val oldState = HistoryUiState(activePromo = oldPromo)

        assertTrue(freshState.isNewlyRegistered(now))
        assertFalse(oldState.isNewlyRegistered(now))
    }

    @Test
    fun `refresh preserves selectedBucketTimestamp across 30s ticker updates`() = runTest {
        val now = 100_000_000L
        val promoStart = now - (3 * 86_400_000L)
        val promo = Promo(
            id = 1L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = promoStart,
            expirationTimestamp = null,
            simSlot = SimSlot.SIM_1
        )

        val bucket1 = UsageBucket(promoStart, promoStart + 86_400_000L, 100_000L, 20_000L)
        val bucket2 = UsageBucket(promoStart + 86_400_000L, now, 200_000L, 40_000L)
        val initialBuckets = listOf(bucket1, bucket2)

        every { getActivePromoUseCase() } returns flowOf(promo)
        coEvery { getDailyUsageBreakdownUseCase(promo, HistoryTimeRange.LAST_7_DAYS) } returns initialBuckets

        val viewModel = HistoryViewModel(getActivePromoUseCase, getDailyUsageBreakdownUseCase)
        advanceUntilIdle()

        // User explicitly taps and selects bucket 1 (yesterday)
        viewModel.selectBucket(bucket1.startTimestamp)
        assertEquals(bucket1.startTimestamp, viewModel.uiState.value.selectedBucketTimestamp)

        // 30 seconds later, refresh ticker fires with slightly updated byte count for bucket 2
        val updatedBucket2 = UsageBucket(promoStart + 86_400_000L, now + 30_000L, 205_000L, 41_000L)
        val refreshedBuckets = listOf(bucket1, updatedBucket2)
        coEvery { getDailyUsageBreakdownUseCase(promo, HistoryTimeRange.LAST_7_DAYS) } returns refreshedBuckets

        viewModel.refresh()
        advanceUntilIdle()

        // Selection should REMAIN on bucket 1!
        assertEquals(bucket1.startTimestamp, viewModel.uiState.value.selectedBucketTimestamp)
        assertEquals(bucket1, viewModel.uiState.value.resolvedSelectedBucket)
    }
}
