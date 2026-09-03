package com.loadpredictor.presentation.widget

import android.content.Context
import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.UsageAccessDeniedException
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.usecase.GetActiveBurnForecastUseCase
import com.loadpredictor.domain.usecase.GetDailyUsageBreakdownUseCase
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
        getActiveBurnForecastUseCase: GetActiveBurnForecastUseCase? = null,
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

            val useCase = getActiveBurnForecastUseCase ?: GetActiveBurnForecastUseCase(
                promoRepository = promoRepository,
                usageRepository = usageRepository,
                burnRateEngine = burnRateEngine,
                getDailyUsageBreakdownUseCase = GetDailyUsageBreakdownUseCase(usageRepository)
            )

            when (val result = useCase.execute(activePromo, now)) {
                is BurnForecastResult.PermissionRequired -> WidgetState.PermissionRequired
                is BurnForecastResult.NoActivePromo -> WidgetState.NoActivePromo
                is BurnForecastResult.Error -> WidgetState.Error(result.message)
                is BurnForecastResult.Success -> {
                    val forecast = result.forecast
                    WidgetState.Success(
                        promoName = forecast.promo.name,
                        simSlot = forecast.promo.simSlot,
                        remainingBytes = forecast.dataRemainingBytes,
                        totalAllowanceBytes = forecast.promo.totalAllowanceBytes,
                        pace = forecast.pace,
                        plainLanguageSummary = forecast.plainLanguageSummary,
                        isNoExpiry = forecast.promo.isNoExpiry,
                        lastUpdatedMillis = now,
                        estimatedDepletionTimestamp = forecast.estimatedDepletionTimestamp
                    )
                }
            }
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
        getActiveBurnForecastUseCase: GetActiveBurnForecastUseCase? = null,
        now: Long = System.currentTimeMillis()
    ): WidgetState {
        val state = computeWidgetState(
            promoRepository = promoRepository,
            usageRepository = usageRepository,
            burnRateEngine = burnRateEngine,
            getActiveBurnForecastUseCase = getActiveBurnForecastUseCase,
            now = now
        )
        WidgetStatePreferences.saveStateAndNotify(context, state)
        return state
    }
}
