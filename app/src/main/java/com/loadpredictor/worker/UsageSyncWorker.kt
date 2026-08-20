package com.loadpredictor.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.local.NotificationPreferencesDataSource
import com.loadpredictor.data.notification.NotificationHelper
import com.loadpredictor.data.repository.PromoRepositoryImpl
import com.loadpredictor.data.repository.UsageRepositoryImpl
import com.loadpredictor.data.stats.NetworkStatsDataSource
import com.loadpredictor.data.stats.UsageAccessHelper
import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.UsageAccessDeniedException
import com.loadpredictor.presentation.widget.WidgetState
import com.loadpredictor.presentation.widget.WidgetStatePreferences
import com.loadpredictor.presentation.widget.WidgetSyncHelper
import kotlinx.coroutines.flow.first

/**
 * Background WorkManager worker responsible for:
 * 1. Periodic data burn measurement and forecast computation.
 * 2. Evaluating threshold alerts (50%, 80%, 90% and premature depletion) with anti-re-fire tracking.
 * 3. Updating Glance home screen widgets across ALL exit paths (PermissionRequired, NoActivePromo, Success, Error).
 */
class UsageSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val usageHelper = UsageAccessHelper(context)
        val database = AppDatabase.getInstance(context)
        val promoRepo = PromoRepositoryImpl(database.promoDao())
        val networkStatsDataSource = NetworkStatsDataSource(context)
        val usageRepo = UsageRepositoryImpl(usageHelper, networkStatsDataSource)
        val burnRateEngine = BurnRateEngine()
        val notificationHelper = NotificationHelper(context)
        val notificationPrefs = NotificationPreferencesDataSource(context)
        val alertPrefsSource = com.loadpredictor.data.local.AlertPreferencesDataSource(context)

        val now = System.currentTimeMillis()
        val widgetState = WidgetSyncHelper.syncWidgetState(
            context = context,
            promoRepository = promoRepo,
            usageRepository = usageRepo,
            burnRateEngine = burnRateEngine,
            now = now
        )

        return when (widgetState) {
            WidgetState.PermissionRequired,
            WidgetState.NoActivePromo -> {
                Result.success()
            }
            is WidgetState.Error -> {
                Result.failure()
            }
            is WidgetState.Success -> {
                try {
                    val activePromo = promoRepo.getActivePromo().first() ?: return Result.success()
                    val rawUsageBytes = usageRepo.queryMobileUsageBytes(activePromo.startTimestamp, now)
                    val forecast = burnRateEngine.calculateForecast(activePromo, rawUsageBytes, now)

                    // Evaluate Threshold Notifications with Anti-Re-Fire & Configurable Preferences
                    val usedRatio = forecast.dataUsedBytes.toDouble() / forecast.promo.totalAllowanceBytes.toDouble()
                    val usedPercentage = (usedRatio * 100.0).toInt()
                    val notifiedThresholds = notificationPrefs.getNotifiedThresholds(activePromo.id).first()
                    val alertPrefs = alertPrefsSource.alertPreferencesFlow.first()

                    // 50%, 80%, 90% Milestones
                    val milestones = listOf(
                        50 to alertPrefs.is50Enabled,
                        80 to alertPrefs.is80Enabled,
                        90 to alertPrefs.is90Enabled
                    )
                    for ((milestone, isEnabled) in milestones) {
                        if (usedPercentage >= milestone && !notifiedThresholds.contains(milestone.toString())) {
                            if (isEnabled) {
                                notificationHelper.showThresholdAlert(
                                    activePromo.name,
                                    milestone,
                                    forecast.dataRemainingBytes
                                )
                            }
                            // Consume milestone to prevent retroactive firing if re-enabled later
                            notificationPrefs.recordThresholdNotified(activePromo.id, milestone.toString())
                        }
                    }

                    // Premature Depletion Alert (Expiring Promos Only)
                    if (!activePromo.isNoExpiry &&
                        forecast.pace == BurnPace.BURNING_FAST &&
                        forecast.estimatedDepletionTimestamp != null &&
                        activePromo.expirationTimestamp != null
                    ) {
                        val diffMs = activePromo.expirationTimestamp - forecast.estimatedDepletionTimestamp
                        val diffHours = diffMs / 3_600_000L
                        if (diffHours >= 12L && !notifiedThresholds.contains("PREMATURE_DEPLETION")) {
                            if (alertPrefs.isPrematureEnabled) {
                                notificationHelper.showPrematureDepletionAlert(
                                    activePromo.name,
                                    diffHours,
                                    forecast.dataRemainingBytes
                                )
                            }
                            // Consume milestone to prevent retroactive firing if re-enabled later
                            notificationPrefs.recordThresholdNotified(activePromo.id, "PREMATURE_DEPLETION")
                        }
                    }

                    Result.success()
                } catch (e: Exception) {
                    Result.failure()
                }
            }
        }
    }
}
