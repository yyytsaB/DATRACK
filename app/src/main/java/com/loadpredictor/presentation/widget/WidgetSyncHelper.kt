package com.loadpredictor.presentation.widget

import android.content.Context
import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.UsageAccessDeniedException
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import kotlinx.coroutines.flow.first

/**
 * Shared synchronization helper that computes and writes Glance widget state.
 *
 * Used by both [com.loadpredictor.worker.UsageSyncWorker] and [com.loadpredictor.presentation.promo.PromoViewModel]
 * to guarantee that immediate foreground updates and background worker executions share
 * identical branch logic (PermissionRequired, NoActivePromo, Success, Error) without logic drift.
 */
object WidgetSyncHelper {

    /**
     * Computes the current [WidgetState] based on permission status, active promo, mobile usage, and forecast.
     */
    suspend fun computeWidgetState(
        promoRepository: PromoRepository,
        usageRepository: UsageRepository,
        burnRateEngine: BurnRateEngine = BurnRateEngine(),
        now: Long = System.currentTimeMillis()
    ): WidgetState {
        if (!usageRepository.hasUsageAccess()) {
            return WidgetState.PermissionRequired
        }

        return try {
            val activePromo = promoRepository.getActivePromo().first()
            if (activePromo == null) {
                return WidgetState.NoActivePromo
            }

            val rawUsageBytes = try {
                usageRepository.queryMobileUsageBytes(activePromo.startTimestamp, now)
            } catch (e: UsageAccessDeniedException) {
                return WidgetState.PermissionRequired
            }

            val forecast = burnRateEngine.calculateForecast(activePromo, rawUsageBytes, now)

            WidgetState.Success(
                promoName = forecast.promo.name,
                simSlot = forecast.promo.simSlot,
                remainingBytes = forecast.dataRemainingBytes,
                totalAllowanceBytes = forecast.promo.totalAllowanceBytes,
                pace = forecast.pace,
                plainLanguageSummary = forecast.plainLanguageSummary,
                isNoExpiry = forecast.promo.isNoExpiry,
                lastUpdatedMillis = now
            )
        } catch (e: Exception) {
            WidgetState.Error(e.localizedMessage ?: "Sync failed")
        }
    }

    /**
     * Computes the current [WidgetState] and immediately writes it to Glance widget preferences and triggers update.
     */
    suspend fun syncWidgetState(
        context: Context,
        promoRepository: PromoRepository,
        usageRepository: UsageRepository,
        burnRateEngine: BurnRateEngine = BurnRateEngine(),
        now: Long = System.currentTimeMillis()
    ): WidgetState {
        val state = computeWidgetState(promoRepository, usageRepository, burnRateEngine, now)
        WidgetStatePreferences.saveStateAndNotify(context, state)
        return state
    }
}
