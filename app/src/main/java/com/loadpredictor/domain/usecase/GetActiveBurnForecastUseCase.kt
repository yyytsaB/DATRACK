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

            val freshInstantaneousRate = if (deltaTimeMs > 0L) {
                (deltaBytes.toDouble() / deltaTimeMs.toDouble()) * 3_600_000.0
            } else {
                0.0
            }
            val alpha = (deltaTimeMs.toDouble() / BurnRateEngine.EMA_FULL_ADAPTATION_WINDOW_MS.toDouble())
                .coerceIn(BurnRateEngine.EMA_MIN_ALPHA, BurnRateEngine.EMA_MAX_ALPHA)

            val shouldPersistSyncState = when {
                activePromo.lastActiveBurnRate == null -> isInitialCalibrated
                else -> deltaBytes >= BurnRateEngine.MIN_ACTIVE_DELTA_BYTES && deltaTimeMs >= BurnRateEngine.MIN_ACTIVE_DELTA_TIME_MS
            }

            println(
                """
                [DATRACK_DIAG] === GetActiveBurnForecastUseCase.execute ===
                promo.id: ${activePromo.id} (${activePromo.name})
                promo.lastSyncDataUsedBytes (BEFORE): ${activePromo.lastSyncDataUsedBytes} (${activePromo.lastSyncDataUsedBytes / (1024 * 1024)} MB)
                promo.lastSyncTimestamp (BEFORE): ${activePromo.lastSyncTimestamp}
                currentLiveUsageBytes: $usedBytes (${usedBytes / (1024 * 1024)} MB)
                deltaBytes: $deltaBytes (${deltaBytes / (1024 * 1024)} MB)
                deltaTimeMs: $deltaTimeMs (${deltaTimeMs / (1000 * 60)} mins)
                freshInstantaneousRate: ${"%.2f MB/hr".format(freshInstantaneousRate / (1024 * 1024))}
                alpha: ${"%.4f".format(alpha)}
                promo.lastActiveBurnRate (BEFORE): ${activePromo.lastActiveBurnRate?.let { "%.2f MB/hr".format(it / (1024 * 1024)) } ?: "null"}
                effectiveRate (RESULT): ${"%.2f MB/hr".format(forecast.burnRateBytesPerHour / (1024 * 1024))}
                dataRemainingBytes: ${forecast.dataRemainingBytes} (${"%.2f GB".format(forecast.dataRemainingBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))})
                estimatedDepletion: ${forecast.estimatedDepletionTimestamp?.let { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(it)) } ?: "null"}
                shouldPersistSyncState: $shouldPersistSyncState
                """.trimIndent()
            )

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
