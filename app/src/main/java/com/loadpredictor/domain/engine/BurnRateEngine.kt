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

        // Active device burn rate strictly from live tracked consumption:
        val burnRateBytesPerHour: Double = if (elapsedTimeMs > 0L) {
            (measuredLiveUsageBytes.toDouble() / elapsedTimeMs.toDouble()) * 3_600_000.0
        } else {
            0.0
        }

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
        } else if (burnRateBytesPerHour <= 0.0 || elapsedTimeMs < STABILIZATION_WINDOW_MS) {
            // Case 2: Zero active burn rate or within initial 1-hour calibration window
            pace = BurnPace.INSUFFICIENT_DATA
            burnStatusIndex = null

            if (promo.expirationTimestamp != null) {
                estimatedDepletionTimestamp = promo.expirationTimestamp
                plainLanguageSummary = "Calibrating: ${formatBytes(dataRemainingBytes)} remaining, on track to expiration."
            } else {
                estimatedDepletionTimestamp = null
                plainLanguageSummary = "Calibrating pace • ${formatBytes(dataRemainingBytes)} remaining."
            }
        } else {
            // Case 3: Stabilized positive burn rate
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
                        plainLanguageSummary = "Pace is optimal: ${formatBytes(dataRemainingBytes)} remaining on ${promo.name} with $daysLeft days left."
                    }
                    else -> {
                        pace = BurnPace.ON_TRACK
                        plainLanguageSummary = "On track: ${formatBytes(dataRemainingBytes)} remaining on ${promo.name}, projected to last through promo validity."
                    }
                }
            } else {
                // Non-expiring promo (Smart Magic Data)
                burnStatusIndex = null
                pace = BurnPace.ON_TRACK
                plainLanguageSummary = "At current pace, ${promo.name} will run out on ${formatTimestamp(depletionTime)}."
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
    fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format(Locale.US, "%.1f GB", gb)
        } else {
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            String.format(Locale.US, "%.0f MB", mb)
        }
    }

    /**
     * Formats an epoch timestamp into a natural day and time format (e.g., "Tuesday at 4:15 PM").
     */
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE 'at' h:mm a", Locale.US)
        return sdf.format(Date(timestamp))
    }
}
