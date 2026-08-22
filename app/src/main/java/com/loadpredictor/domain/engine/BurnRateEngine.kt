package com.loadpredictor.domain.engine

import com.loadpredictor.domain.model.BurnForecast
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.Promo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure Kotlin mathematical forecast engine.
 *
 * Implements the core Philippine Prepaid Mobile Data Burn-Rate calculation formulas,
 * safety guards against division by zero, stabilization dampening during initial hours,
 * minimum meaningful usage thresholds to prevent background sync noise from triggering premature projections,
 * support for initial historical usage offsets, and plain-language formatting.
 *
 * This engine has zero Android framework dependencies per Engineering Rule #1.
 */
class BurnRateEngine {

    companion object {
        /**
         * Minimum duration in milliseconds before burn rate is considered stabilized (1 hour).
         * Prevents initial burst usage from causing astronomical premature depletion projections.
         */
        const val STABILIZATION_WINDOW_MS = 3_600_000L

        /**
         * Minimum live mobile data consumption in bytes (10 MB) required to exit calibration.
         * Guarantees immunity against background system sync pings, FCM heartbeats, and OS telemetry,
         * ensuring projections are strictly computed from meaningful user data activity.
         */
        const val MIN_MEANINGFUL_USAGE_BYTES = 10L * 1024L * 1024L // 10 MB

        /**
         * Burn Status Index threshold for fast data consumption (>= 25% faster than linear pace).
         */
        const val BURN_FAST_THRESHOLD = 1.25

        /**
         * Burn Status Index threshold for conservative data consumption (>= 25% slower than linear pace).
         */
        const val CONSERVATIVE_THRESHOLD = 0.75
    }

    /**
     * Calculates the burn-rate forecast metrics.
     *
     * Total data used is the sum of the historical starting baseline offset ([promo.initialUsageOffsetBytes])
     * and live device-measured mobile bytes consumed during tracking ([promo.startTimestamp, currentTime]).
     *
     * The active burn rate ($R_{burn}$) is computed strictly from live device-measured usage divided by
     * elapsed tracking time ($T_{elapsed}$), ensuring active velocity is not artificially inflated by
     * historical pre-app usage.
     *
     * Both [STABILIZATION_WINDOW_MS] AND [MIN_MEANINGFUL_USAGE_BYTES] must be satisfied (strict AND)
     * before the engine exits [BurnPace.INSUFFICIENT_DATA] / Calibrating.
     *
     * @param promo The tracked promo domain model.
     * @param dataUsedBytesRaw The measured aggregate mobile bytes consumed during [promo.startTimestamp, currentTime].
     * @param currentTime Current epoch timestamp in milliseconds.
     * @return Fully populated [BurnForecast].
     */
    fun calculateForecast(
        promo: Promo,
        dataUsedBytesRaw: Long,
        currentTime: Long
    ): BurnForecast {
        val elapsedTimeMs = maxOf(0L, currentTime - promo.startTimestamp)
        val measuredLiveUsageBytes = maxOf(0L, dataUsedBytesRaw)
        val dataUsedBytes = minOf(
            promo.totalAllowanceBytes,
            maxOf(0L, promo.initialUsageOffsetBytes + measuredLiveUsageBytes)
        )
        val dataRemainingBytes = maxOf(0L, promo.totalAllowanceBytes - dataUsedBytes)
        val isDepleted = (dataRemainingBytes == 0L)

        val timeRemainingMillis: Long? = promo.expirationTimestamp?.let { exp ->
            maxOf(0L, exp - currentTime)
        }

        // Check calibration status: must clear BOTH the 1-hour time window AND 10 MB volume threshold
        val isCalibrated = (elapsedTimeMs >= STABILIZATION_WINDOW_MS) &&
                (measuredLiveUsageBytes >= MIN_MEANINGFUL_USAGE_BYTES)

        // Detect if fresh mobile data traffic occurred since last recorded sync
        val hasNewActiveUsage = (measuredLiveUsageBytes > promo.lastSyncDataUsedBytes)

        // Effective burn rate: use fresh live velocity on active traffic; freeze to last known active rate when idle
        val burnRateBytesPerHour: Double = when {
            hasNewActiveUsage -> {
                if (elapsedTimeMs > 0L) {
                    (measuredLiveUsageBytes.toDouble() / elapsedTimeMs.toDouble()) * 3_600_000.0
                } else {
                    0.0
                }
            }
            promo.lastActiveBurnRate != null && promo.lastActiveBurnRate > 0.0 -> {
                promo.lastActiveBurnRate
            }
            elapsedTimeMs > 0L -> {
                (measuredLiveUsageBytes.toDouble() / elapsedTimeMs.toDouble()) * 3_600_000.0
            }
            else -> 0.0
        }

        val hasSufficientData = isCalibrated || (promo.lastActiveBurnRate != null && promo.lastActiveBurnRate > 0.0)

        val estimatedDepletionTimestamp: Long?
        val burnStatusIndex: Double?
        val pace: BurnPace
        val plainLanguageSummary: String

        if (isDepleted) {
            // Case 1: Promo is completely depleted
            pace = BurnPace.DEPLETED
            estimatedDepletionTimestamp = currentTime
            burnStatusIndex = if (promo.expirationTimestamp != null) Double.POSITIVE_INFINITY else null
            plainLanguageSummary = "Depleted! ${promo.name} promo has 0 MB remaining."
        } else if (!hasSufficientData || burnRateBytesPerHour <= 0.0) {
            // Case 2: Insufficient time (< 1 hr), insufficient data volume (< 10 MB), or zero active burn rate
            pace = BurnPace.INSUFFICIENT_DATA
            burnStatusIndex = null

            if (promo.expirationTimestamp != null) {
                estimatedDepletionTimestamp = promo.expirationTimestamp
                plainLanguageSummary = "Calibrating: ${formatBytes(dataRemainingBytes, promo.totalAllowanceBytes)} remaining, on track to expiration."
            } else {
                estimatedDepletionTimestamp = null
                plainLanguageSummary = "Calibrating pace • ${formatBytes(dataRemainingBytes, promo.totalAllowanceBytes)} remaining."
            }
        } else {
            // Case 3: Stabilized positive burn rate with meaningful usage
            val remainingHours = dataRemainingBytes.toDouble() / burnRateBytesPerHour
            val remainingMs = (remainingHours * 3_600_000.0).toLong()
            val depletionTime = currentTime + remainingMs
            estimatedDepletionTimestamp = depletionTime

            if (promo.expirationTimestamp != null) {
                val totalDurationMs = maxOf(1L, promo.expirationTimestamp - promo.startTimestamp)
                val timeElapsedRatio = elapsedTimeMs.toDouble() / totalDurationMs.toDouble()
                val dataUsedRatio = dataUsedBytes.toDouble() / promo.totalAllowanceBytes.toDouble()
                val index = if (timeElapsedRatio > 0.0) dataUsedRatio / timeElapsedRatio else 1.0
                burnStatusIndex = index

                when {
                    index > BURN_FAST_THRESHOLD -> {
                        pace = BurnPace.BURNING_FAST
                        val diffMs = maxOf(0L, promo.expirationTimestamp - depletionTime)
                        val diffHours = diffMs / 3_600_000L
                        val diffText = if (diffHours >= 24) {
                            val days = diffHours / 24
                            val hours = diffHours % 24
                            if (hours > 0) "$days days $hours hrs" else "$days days"
                        } else {
                            "$diffHours hours"
                        }
                        plainLanguageSummary = "At current pace, ${promo.name} data will run out on ${formatTimestamp(depletionTime)} ($diffText before promo expires)."
                    }
                    index < CONSERVATIVE_THRESHOLD -> {
                        pace = BurnPace.CONSERVATIVE
                        val daysLeft = maxOf(0L, (promo.expirationTimestamp - currentTime) / (24 * 3_600_000L))
                        plainLanguageSummary = "Pace is optimal: ${formatBytes(dataRemainingBytes, promo.totalAllowanceBytes)} remaining on ${promo.name} with $daysLeft days left."
                    }
                    else -> {
                        pace = BurnPace.ON_TRACK
                        plainLanguageSummary = "On track: ${formatBytes(dataRemainingBytes, promo.totalAllowanceBytes)} remaining on ${promo.name}, projected to last through promo validity."
                    }
                }
            } else {
                // Non-expiring promo (Smart Magic Data)
                burnStatusIndex = null
                pace = BurnPace.ON_TRACK
                plainLanguageSummary = "At current steady pace, ${promo.name} will run out on ${formatTimestamp(depletionTime)}."
            }
        }

        return BurnForecast(
            promo = promo,
            dataUsedBytes = dataUsedBytes,
            dataRemainingBytes = dataRemainingBytes,
            burnRateBytesPerHour = burnRateBytesPerHour,
            estimatedDepletionTimestamp = estimatedDepletionTimestamp,
            burnStatusIndex = burnStatusIndex,
            pace = pace,
            plainLanguageSummary = plainLanguageSummary,
            isDepleted = isDepleted,
            timeRemainingMillis = timeRemainingMillis
        )
    }

    /**
     * Formats bytes to MB or GB with clean precision.
     */
    fun formatBytes(bytes: Long, totalAllowanceBytes: Long? = null): String {
        return com.loadpredictor.util.DataFormatter.formatDataAmount(bytes, totalAllowanceBytes)
    }

    /**
     * Formats an epoch timestamp into a natural day and time format (e.g., "Tuesday at 4:15 PM").
     */
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE 'at' h:mm a", Locale.US)
        return sdf.format(Date(timestamp))
    }
}
