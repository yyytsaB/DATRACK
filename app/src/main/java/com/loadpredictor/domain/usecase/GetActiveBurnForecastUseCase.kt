package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.UsageAccessDeniedException
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.time.DefaultTimeProvider
import com.loadpredictor.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.transform

/**
 * UseCase coordinating active promo lookup, mobile usage data queries,
 * and forecast calculation into a reactive [BurnForecastResult] stream or on-demand execution.
 */
class GetActiveBurnForecastUseCase(
    private val promoRepository: PromoRepository,
    private val usageRepository: UsageRepository,
    private val burnRateEngine: BurnRateEngine = BurnRateEngine(),
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) {
    suspend fun execute(activePromo: Promo?): BurnForecastResult {
        if (activePromo == null) {
            return BurnForecastResult.NoActivePromo
        }
        return try {
            val now = timeProvider.currentTimeMillis()
            val usedBytes = usageRepository.queryMobileUsageBytes(
                startTime = activePromo.startTimestamp,
                endTime = now
            )
            val forecast = burnRateEngine.calculateForecast(
                promo = activePromo,
                dataUsedBytesRaw = usedBytes,
                currentTime = now
            )
            val elapsedTimeMs = maxOf(0L, now - activePromo.startTimestamp)
            val isInitialCalibrated = (elapsedTimeMs >= BurnRateEngine.STABILIZATION_WINDOW_MS) &&
                    (usedBytes >= BurnRateEngine.MIN_MEANINGFUL_USAGE_BYTES)

            val deltaBytes = if (activePromo.lastSyncDataUsedBytes > 0L) {
                usedBytes - activePromo.lastSyncDataUsedBytes
            } else {
                usedBytes
            }
            val deltaTimeMs = if (activePromo.lastSyncTimestamp > 0L) {
                maxOf(0L, now - activePromo.lastSyncTimestamp)
            } else {
                elapsedTimeMs
            }

            val shouldPersistSyncState = when {
                activePromo.lastActiveBurnRate == null -> isInitialCalibrated
                else -> deltaBytes >= BurnRateEngine.MIN_ACTIVE_DELTA_BYTES && deltaTimeMs >= BurnRateEngine.MIN_ACTIVE_DELTA_TIME_MS
            }

            if (shouldPersistSyncState && forecast.burnRateBytesPerHour > 0.0) {
                promoRepository.updateSyncState(
                    promoId = activePromo.id,
                    burnRate = forecast.burnRateBytesPerHour,
                    dataUsedBytes = usedBytes,
                    syncTimestamp = now
                )
            }
            BurnForecastResult.Success(forecast)
        } catch (throwable: Throwable) {
            if (throwable is UsageAccessDeniedException || throwable.cause is UsageAccessDeniedException) {
                BurnForecastResult.PermissionRequired
            } else {
                BurnForecastResult.Error(throwable.localizedMessage ?: "Unexpected error during forecast calculation")
            }
        }
    }

    operator fun invoke(): Flow<BurnForecastResult> = promoRepository.getActivePromo()
        .transform { activePromo ->
            emit(execute(activePromo))
        }
        .catch { throwable ->
            if (throwable is UsageAccessDeniedException || throwable.cause is UsageAccessDeniedException) {
                emit(BurnForecastResult.PermissionRequired)
            } else {
                emit(BurnForecastResult.Error(throwable.localizedMessage ?: "Unexpected error during forecast calculation"))
            }
        }
}
