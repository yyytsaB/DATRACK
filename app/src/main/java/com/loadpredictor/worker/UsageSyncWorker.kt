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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    companion object { private val syncMutex = Mutex() }

    override suspend fun doWork(): Result = com.loadpredictor.domain.sync.DataOperationGate.exclusive {
        val trigger=inputData.getString("trigger") ?: "unspecified"
        suspend fun record(status:String) = com.loadpredictor.domain.diagnostics.ObservationLog.record(
            com.loadpredictor.domain.diagnostics.ObservationEvent(System.currentTimeMillis(),"worker:$trigger",status,
                attempt=runAttemptCount,requestedAt=inputData.getLong("requestedAt",0).takeIf {it>0}))
        record("attempt_started")
        try {
            val outcome=runWork()
            record(when(outcome){Result.success()->"completed";Result.retry()->"retry";else->"failed"})
            outcome
        } catch(e:kotlinx.coroutines.CancellationException) {throw e}
    }

    private suspend fun runWork():Result = syncMutex.withLock {
        // Bound explicit retry results as well as thrown failures. Neither path may loop forever.
        if (runAttemptCount >= 5) return@withLock Result.failure()
        try { synchronizeUsage() }
        catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }

    private suspend fun synchronizeUsage(): Result {
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
        val activePromo = promoRepo.getActivePromo().first()
        val useCase = com.loadpredictor.domain.usecase.GetActiveBurnForecastUseCase(
            promoRepo,usageRepo,getDailyUsageBreakdownUseCase=com.loadpredictor.domain.usecase.GetDailyUsageBreakdownUseCase(usageRepo))
        val result = if (!usageRepo.hasUsageAccess()) com.loadpredictor.domain.model.BurnForecastResult.PermissionRequired
            else useCase.execute(activePromo,now)
        val widgetState = WidgetSyncHelper.fromForecastResult(result,now)
        WidgetStatePreferences.saveStateAndNotify(context,widgetState)
        if(usageRepo.hasUsageAccess())try {
            com.loadpredictor.data.backup.AdvisoryService(context).evaluate(activePromo,now)
        }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){
            com.loadpredictor.domain.diagnostics.ObservationLog.record(com.loadpredictor.domain.diagnostics.ObservationEvent(now,"advisory","unavailable"))
        }

        return when (widgetState) {
            WidgetState.PermissionRequired,
            WidgetState.NoActivePromo -> {
                Result.success()
            }
            is WidgetState.Error -> {
                Result.retry()
            }
            is WidgetState.Success -> {
                try {
                    val forecast = (result as? com.loadpredictor.domain.model.BurnForecastResult.Success)?.forecast
                        ?: return Result.retry()
                    val selectedPromo = forecast.promo
                    if (forecast.pace == BurnPace.EXPIRED) return Result.success()

                    // Evaluate Threshold Notifications with Anti-Re-Fire & Configurable Preferences
                    val usedRatio = forecast.dataUsedBytes.toDouble() / forecast.promo.totalAllowanceBytes.toDouble()
                    val usedPercentage = (usedRatio * 100.0).toInt()
                    val crossed=buildSet {
                        listOf(50,80,90).filter {usedPercentage>=it}.forEach {add(it.toString())}
                        if(selectedPromo.expirationTimestamp!=null && forecast.estimatedDepletionTimestamp!=null &&
                            selectedPromo.expirationTimestamp-forecast.estimatedDepletionTimestamp>=12*3600000L) add("PREMATURE_DEPLETION")
                    }
                    notificationPrefs.primeCorrection(selectedPromo.id,selectedPromo.correctionRevision,crossed)
                    notificationPrefs.primeRestoredPromo(selectedPromo.id,crossed)
                    val notifiedThresholds = notificationPrefs.getNotifiedThresholds(selectedPromo.id).first()
                    val alertPrefs = alertPrefsSource.alertPreferencesFlow.first()

                    // 50%, 80%, 90% Milestones
                    val milestones = listOf(
                        50 to alertPrefs.is50Enabled,
                        80 to alertPrefs.is80Enabled,
                        90 to alertPrefs.is90Enabled
                    )
                    for ((milestone, isEnabled) in milestones) {
                        if (usedPercentage >= milestone && !notifiedThresholds.contains(milestone.toString())) {
                            val recorded = !isEnabled || notificationHelper.showThresholdAlert(
                                    selectedPromo.name,
                                    milestone,
                                    forecast.dataRemainingBytes
                                )
                            if (recorded) notificationPrefs.recordThresholdNotified(selectedPromo.id, milestone.toString())
                        }
                    }

                    // Premature Depletion Alert (Expiring Promos Only)
                    if (!selectedPromo.isNoExpiry &&
                        forecast.pace == BurnPace.BURNING_FAST &&
                        forecast.estimatedDepletionTimestamp != null &&
                        selectedPromo.expirationTimestamp != null
                    ) {
                        val diffMs = selectedPromo.expirationTimestamp - forecast.estimatedDepletionTimestamp
                        val diffHours = diffMs / 3_600_000L
                        if (diffHours >= 12L && !notifiedThresholds.contains("PREMATURE_DEPLETION")) {
                            val recorded = !alertPrefs.isPrematureEnabled || notificationHelper.showPrematureDepletionAlert(
                                    selectedPromo.name,
                                    diffHours,
                                    forecast.dataRemainingBytes
                                )
                            if (recorded) notificationPrefs.recordThresholdNotified(selectedPromo.id, "PREMATURE_DEPLETION")
                        }
                    }

                    Result.success()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Result.retry()
                }
            }
        }
    }
}
