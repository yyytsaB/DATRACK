package com.loadpredictor.data.repository

import com.loadpredictor.data.stats.NetworkStatsDataSource
import com.loadpredictor.data.stats.UsageAccessHelper
import com.loadpredictor.domain.model.UsageAccessDeniedException
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.repository.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of [UsageRepository] utilizing [UsageAccessHelper] and [NetworkStatsDataSource].
 */
class UsageRepositoryImpl(
    private val usageAccessHelper: UsageAccessHelper,
    private val networkStatsDataSource: NetworkStatsDataSource
) : UsageRepository {

    override fun hasUsageAccess(): Boolean {
        return usageAccessHelper.hasUsageAccessPermission()
    }

    override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long {
        return withContext(Dispatchers.IO) {
            if (!hasUsageAccess()) {
                throw UsageAccessDeniedException("PACKAGE_USAGE_STATS permission is not granted")
            }
            networkStatsDataSource.queryMobileUsageBytes(startTime, endTime)
        }
    }

    override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket> {
        return withContext(Dispatchers.IO) {
            if (!hasUsageAccess()) {
                throw UsageAccessDeniedException("PACKAGE_USAGE_STATS permission is not granted")
            }
            networkStatsDataSource.queryDailyUsageBreakdown(startTime, endTime)
        }
    }
}
