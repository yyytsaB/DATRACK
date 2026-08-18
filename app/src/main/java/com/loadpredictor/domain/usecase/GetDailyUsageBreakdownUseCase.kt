package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.time.DefaultTimeProvider
import com.loadpredictor.domain.time.TimeProvider

/**
 * UseCase to retrieve daily mobile data consumption breakdown buckets for an active promo.
 *
 * Slices the query window strictly from the promo start timestamp (capped at max 30 days in the past)
 * up to the current time, ensuring historical device usage prior to promo registration is not included.
 */
class GetDailyUsageBreakdownUseCase(
    private val usageRepository: UsageRepository,
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) {

    companion object {
        const val MAX_HISTORY_WINDOW_MS = 30L * 24L * 60L * 60L * 1000L // 30 days
    }

    /**
     * Queries daily buckets for the given promo.
     *
     * @param promo The promo context.
     * @param currentTime Epoch timestamp (defaults to [TimeProvider.currentTimeMillis]).
     * @return List of [UsageBucket] representing day-by-day consumption.
     */
    suspend operator fun invoke(
        promo: Promo,
        currentTime: Long = timeProvider.currentTimeMillis()
    ): List<UsageBucket> {
        val thirtyDaysAgo = currentTime - MAX_HISTORY_WINDOW_MS
        val startTime = maxOf(promo.startTimestamp, thirtyDaysAgo)
        val endTime = maxOf(startTime, currentTime)

        if (startTime >= endTime) {
            return emptyList()
        }

        return usageRepository.queryDailyUsageBreakdown(startTime, endTime)
    }
}
