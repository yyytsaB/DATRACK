package com.loadpredictor.data.stats

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.RemoteException
import com.loadpredictor.domain.model.UsageAccessDeniedException
import com.loadpredictor.domain.model.UsageBucket

/**
 * Data source wrapping Android's [NetworkStatsManager] to query device mobile data metrics.
 *
 * Crucial Constraints (from SKILL.md Section 2.2):
 * 1. Filter strictly to [ConnectivityManager.TYPE_MOBILE] to exclude WiFi traffic.
 * 2. Pass null for [subscriberId]. Passing an empty string "" fails silently on hardware (matches 0 records).
 *    Passing null aggregates device-wide mobile data across the device's cellular interfaces.
 * 3. Distinguish permission failure ([UsageAccessDeniedException]) from genuine 0L usage.
 */
class NetworkStatsDataSource(
    private val context: Context,
    private val customNetworkStatsManager: NetworkStatsManager? = null
) {

    private val networkStatsManager: NetworkStatsManager?
        get() = customNetworkStatsManager ?: (context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager)

    /**
     * Queries aggregate device mobile data usage (rxBytes + txBytes) between [startTime] and [endTime].
     *
     * @param startTime Start of interval in epoch milliseconds.
     * @param endTime End of interval in epoch milliseconds.
     * @return Sum of received and transmitted bytes over mobile connections, or 0L if no traffic.
     * @throws UsageAccessDeniedException if PACKAGE_USAGE_STATS is not granted or revoked mid-query.
     */
    @Throws(UsageAccessDeniedException::class)
    fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long {
        if (startTime >= endTime || startTime < 0 || endTime <= 0) {
            return 0L
        }

        val manager = networkStatsManager
            ?: throw UsageAccessDeniedException("NetworkStatsManager system service is unavailable")

        return try {
            // Per SKILL.md 2.2: Pass null for subscriberId to aggregate device-wide mobile data.
            @Suppress("DEPRECATION")
            val networkType = ConnectivityManager.TYPE_MOBILE
            val bucket = manager.querySummaryForDevice(
                networkType,
                null,
                startTime,
                endTime
            )
            val rx = bucket.rxBytes.coerceAtLeast(0L)
            val tx = bucket.txBytes.coerceAtLeast(0L)
            rx + tx
        } catch (e: SecurityException) {
            throw UsageAccessDeniedException(
                message = "PACKAGE_USAGE_STATS permission not granted or revoked mid-query",
                cause = e
            )
        } catch (e: RemoteException) {
            0L
        }
    }

    /**
     * Queries daily mobile data consumption buckets between [startTime] and [endTime].
     *
     * @throws UsageAccessDeniedException if PACKAGE_USAGE_STATS is not granted or revoked mid-query.
     */
    @Throws(UsageAccessDeniedException::class)
    fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket> {
        if (startTime >= endTime) return emptyList()

        val manager = networkStatsManager
            ?: throw UsageAccessDeniedException("NetworkStatsManager system service is unavailable")

        val buckets = mutableListOf<UsageBucket>()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        var currentStart = startTime
        @Suppress("DEPRECATION")
        val mobileType = ConnectivityManager.TYPE_MOBILE
        while (currentStart < endTime) {
            val currentEnd = (currentStart + oneDayMillis).coerceAtMost(endTime)
            try {
                val bucket = manager.querySummaryForDevice(
                    mobileType,
                    null,
                    currentStart,
                    currentEnd
                )
                val rx = bucket.rxBytes.coerceAtLeast(0L)
                val tx = bucket.txBytes.coerceAtLeast(0L)
                buckets.add(
                    UsageBucket(
                        startTimestamp = currentStart,
                        endTimestamp = currentEnd,
                        rxBytes = rx,
                        txBytes = tx
                    )
                )
            } catch (e: SecurityException) {
                throw UsageAccessDeniedException(
                    message = "PACKAGE_USAGE_STATS permission not granted or revoked mid-query",
                    cause = e
                )
            } catch (e: Exception) {
                buckets.add(
                    UsageBucket(
                        startTimestamp = currentStart,
                        endTimestamp = currentEnd,
                        rxBytes = 0L,
                        txBytes = 0L
                    )
                )
            }
            currentStart = currentEnd
        }

        return buckets
    }
}
