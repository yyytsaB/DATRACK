package com.loadpredictor.domain.repository

import com.loadpredictor.domain.model.UsageAccessDeniedException
import com.loadpredictor.domain.model.UsageBucket

/**
 * Domain repository contract for querying device mobile data consumption
 * and checking required platform permissions.
 */
interface UsageRepository {
    /**
     * Checks if the app has been granted android.permission.PACKAGE_USAGE_STATS (Usage Access).
     */
    fun hasUsageAccess(): Boolean

    /**
     * Queries total device-level mobile data consumed (rxBytes + txBytes)
     * strictly within [startTime] and [endTime] in epoch milliseconds.
     * Excludes WiFi traffic.
     *
     * @return Total consumed bytes (0L if valid range has zero traffic).
     * @throws UsageAccessDeniedException if PACKAGE_USAGE_STATS permission is missing or revoked.
     */
    @Throws(UsageAccessDeniedException::class)
    suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long

    /**
     * Queries daily mobile data breakdown buckets within the specified interval.
     *
     * @throws UsageAccessDeniedException if PACKAGE_USAGE_STATS permission is missing or revoked.
     */
    @Throws(UsageAccessDeniedException::class)
    suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket>
}
