package com.loadpredictor.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Utility for scheduling and triggering [UsageSyncWorker] background jobs.
 */
object WorkManagerScheduler {

    private const val PERIODIC_WORK_NAME = "usage_sync_periodic"
    private const val ONE_TIME_WORK_NAME = "usage_sync_immediate"

    /**
     * Enqueues the periodic 1-hour background sync job in compliance with battery constraints in SKILL.md.
     */
    fun schedulePeriodicSync(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<UsageSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /**
     * Triggers an immediate one-time sync (e.g. on widget refresh or active promo update).
     */
    fun enqueueImmediateSync(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<UsageSyncWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
