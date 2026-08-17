package com.loadpredictor.domain.model

/**
 * Pure Kotlin domain model representing the calculated burn-rate forecast metrics.
 *
 * All data quantities are stored in raw bytes for precision per Engineering Rule #4.
 *
 * @property promo The active promo context for this forecast.
 * @property dataUsedBytes Measured mobile data consumed during the promo window in raw bytes.
 * @property dataRemainingBytes Remaining data allowance in raw bytes (clamped >= 0).
 * @property burnRateBytesPerHour Calculated burn rate in bytes per hour.
 * @property estimatedDepletionTimestamp Epoch timestamp in milliseconds when data is projected to run out, or null if calibrating/no-expiry.
 * @property burnStatusIndex Ratio of data used percentage to time elapsed percentage (null for non-expiring promos).
 * @property pace Pacing classification category.
 * @property plainLanguageSummary Natural, localized plain-language forecast sentence.
 * @property isDepleted True if dataRemainingBytes is 0.
 * @property timeRemainingMillis Milliseconds until promo expiration, or null for non-expiring promos.
 */
data class BurnForecast(
    val promo: Promo,
    val dataUsedBytes: Long,
    val dataRemainingBytes: Long,
    val burnRateBytesPerHour: Double,
    val estimatedDepletionTimestamp: Long?,
    val burnStatusIndex: Double?,
    val pace: BurnPace,
    val plainLanguageSummary: String,
    val isDepleted: Boolean,
    val timeRemainingMillis: Long?
)
