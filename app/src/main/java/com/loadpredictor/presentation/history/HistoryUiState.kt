package com.loadpredictor.presentation.history

import com.loadpredictor.domain.model.HistoryTimeRange
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.model.UsagePatternInsight

/**
 * Immutable UI State for the History analytics screen.
 *
 * @param activePromo The currently active tracking promo, or null if none.
 * @param selectedRange The active time-range filter (7D, 30D, Lifetime).
 * @param dailyBuckets List of day-by-day consumption buckets for the active promo within [selectedRange].
 * @param selectedBucketTimestamp Timestamp of the currently inspected bar/bucket (stable across ticker refreshes).
 * @param patternInsight Computed behavioral insight comparing weekday and weekend usage.
 * @param isLoading Whether data is currently loading.
 */
data class HistoryUiState(
    val activePromo: Promo? = null,
    val selectedRange: HistoryTimeRange = HistoryTimeRange.LAST_7_DAYS,
    val dailyBuckets: List<UsageBucket> = emptyList(),
    val selectedBucketTimestamp: Long? = null,
    val patternInsight: UsagePatternInsight = UsagePatternInsight.InsufficientData,
    val isLoading: Boolean = true
) {
    /**
     * Total data volume consumed across all buckets in the selected range.
     */
    val totalBurntBytes: Long
        get() = dailyBuckets.sumOf { it.totalBytes }

    /**
     * Average daily data consumption across available buckets in the selected range.
     */
    val dailyAverageBytes: Long
        get() = if (dailyBuckets.isEmpty()) 0L else (dailyBuckets.map { it.totalBytes }.average()).toLong()

    /**
     * The single highest consumption bucket in the selected range.
     */
    val peakDayBucket: UsageBucket?
        get() = dailyBuckets.maxByOrNull { it.totalBytes }

    /**
     * True if the active promo was registered within the last 48 hours.
     * Triggers the RegistrationExplainerCard informing the user that historical pre-registration days are excluded.
     */
    val isNewlyRegisteredPromo: Boolean
        get() = isNewlyRegistered(System.currentTimeMillis())

    fun isNewlyRegistered(now: Long): Boolean {
        val promo = activePromo ?: return false
        val promoAgeMs = (now - promo.startTimestamp).coerceAtLeast(0L)
        return promoAgeMs <= 48L * 60L * 60L * 1000L // 48 hours
    }

    /**
     * Resolves the currently selected bucket by timestamp, falling back to the latest bucket if unselected.
     */
    val resolvedSelectedBucket: UsageBucket?
        get() = if (selectedBucketTimestamp != null) {
            dailyBuckets.find { it.startTimestamp == selectedBucketTimestamp } ?: dailyBuckets.lastOrNull()
        } else {
            dailyBuckets.lastOrNull()
        }
}
