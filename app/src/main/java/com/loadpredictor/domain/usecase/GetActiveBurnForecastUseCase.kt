package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.BurnForecastResult
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
 * and forecast calculation into a reactive [BurnForecastResult] stream.
 */
class GetActiveBurnForecastUseCase(
    private val promoRepository: PromoRepository,
    private val usageRepository: UsageRepository,
    private val burnRateEngine: BurnRateEngine = BurnRateEngine(),
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) {
    operator fun invoke(): Flow<BurnForecastResult> = promoRepository.getActivePromo()
        .transform { activePromo ->
            if (activePromo == null) {
                emit(BurnForecastResult.NoActivePromo)
            } else {
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
                emit(BurnForecastResult.Success(forecast))
            }
        }
        .catch { throwable ->
            if (throwable is UsageAccessDeniedException || throwable.cause is UsageAccessDeniedException) {
                emit(BurnForecastResult.PermissionRequired)
            } else {
                emit(BurnForecastResult.Error(throwable.localizedMessage ?: "Unexpected error during forecast calculation"))
            }
        }
}
