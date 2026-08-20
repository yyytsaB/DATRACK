package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.model.HistoryTimeRange
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.time.DefaultTimeProvider
import com.loadpredictor.domain.time.TimeProvider

/**
 * UseCase to retrieve daily mobile data consumption breakdown buckets for an active promo.
 *
 * Slices the query window strictly from the promo start timestamp (bounded by the selected [HistoryTimeRange] window)
 * up to the current time, ensuring historical device usage prior to promo registration is not included.
 */
class GetDailyUsageBreakdownUseCase(
    private val usageRepository: UsageRepository,
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) {

    /**
     * Queries daily buckets for the given promo within the specified [timeRange] at the current system time.
     */
    suspend operator fun invoke(
        promo: Promo,
        timeRange: HistoryTimeRange = HistoryTimeRange.LAST_7_DAYS
    ): List<UsageBucket> {
        return invoke(promo, timeProvider.currentTimeMillis(), timeRange)
    }

    /**
     * Queries daily buckets for the given promo within the specified [timeRange] at an explicit [currentTime].
     */
    suspend operator fun invoke(
        promo: Promo,
        currentTime: Long,
        timeRange: HistoryTimeRange = HistoryTimeRange.LAST_7_DAYS
    ): List<UsageBucket> {
        val windowStart = currentTime - timeRange.windowMs
        val startTime = maxOf(promo.startTimestamp, windowStart)
        val endTime = maxOf(startTime, currentTime)

        if (startTime >= endTime) {
            return emptyList()
        }

        return usageRepository.queryDailyUsageBreakdown(startTime, endTime)
    }
}
