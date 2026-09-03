package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.BurnForecast
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.UsageAccessDeniedException
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.time.DefaultTimeProvider
import com.loadpredictor.domain.time.TimeProvider
import com.loadpredictor.domain.usecase.GetDailyUsageBreakdownUseCase
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.transform

/**
 * UseCase coordinating active promo lookup, mobile usage data queries,
 * and forecast calculation into a reactive [BurnForecastResult] stream or on-demand execution.
 *
 * Anchors the depletion projection to the 7-day rolling daily mean from completed daily buckets
 * when at least [MIN_DAILY_ANCHOR_DAYS] completed days of history exist, keeping user-facing
 * depletion forecasts consistent with the daily average while preserving the EMA sync state.
 */
class GetActiveBurnForecastUseCase(
    private val promoRepository: PromoRepository,
    private val usageRepository: UsageRepository,
    private val burnRateEngine: BurnRateEngine = BurnRateEngine(),
    private val timeProvider: TimeProvider = DefaultTimeProvider(),
    private val getDailyUsageBreakdownUseCase: GetDailyUsageBreakdownUseCase? = null
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

            // Attempt to anchor the depletion projection to the 7-day rolling daily mean from completed
            // daily buckets. If anchoring is available (>= MIN_DAILY_ANCHOR_DAYS completed days), it
            // overrides the EMA-derived burnRateBytesPerHour in the user-facing BurnForecast to ensure
            // the "Daily avg" chip and "At this pace" chip are mathematically consistent.
            // The EMA path (lastActiveBurnRate, shouldPersistSyncState, updateSyncState) is unchanged.
            val anchoredForecast: BurnForecast = run {
                val dailyMeanRate = computeSevenDayDailyMeanBytesPerHour(activePromo, now)
                if (dailyMeanRate != null && dailyMeanRate > 0.0 &&
                    forecast.pace != BurnPace.INSUFFICIENT_DATA &&
                    forecast.pace != BurnPace.DEPLETED
                ) {
                    val remainingHours = forecast.dataRemainingBytes.toDouble() / dailyMeanRate
                    val remainingMs = (remainingHours * 3_600_000.0).toLong()
                    val anchoredDepletionTime = now + remainingMs
                    forecast.copy(
                        burnRateBytesPerHour = dailyMeanRate,
                        estimatedDepletionTimestamp = anchoredDepletionTime,
                        plainLanguageSummary = buildAnchoredSummary(activePromo, anchoredDepletionTime, now)
                    )
                } else {
                    forecast
                }
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
                anchoredRate: ${anchoredForecast.burnRateBytesPerHour.let { "%.2f MB/hr".format(it / (1024 * 1024)) }} (${if (anchoredForecast.burnRateBytesPerHour != forecast.burnRateBytesPerHour) "ANCHORED to daily mean" else "EMA path"})
                dataRemainingBytes: ${forecast.dataRemainingBytes} (${"%.2f GB".format(forecast.dataRemainingBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))})
                estimatedDepletion: ${anchoredForecast.estimatedDepletionTimestamp?.let { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(it)) } ?: "null"}
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
            BurnForecastResult.Success(anchoredForecast)
        } catch (throwable: Throwable) {
            if (throwable is UsageAccessDeniedException || throwable.cause is UsageAccessDeniedException) {
                BurnForecastResult.PermissionRequired
            } else {
                BurnForecastResult.Error(throwable.localizedMessage ?: "Unexpected error during forecast calculation")
            }
        }
    }

    /**
     * Computes a 7-day rolling daily mean burn rate in bytes/hour from completed daily buckets.
     *
     * Returns null if:
     * - [getDailyUsageBreakdownUseCase] is not injected, or
     * - fewer than [MIN_DAILY_ANCHOR_DAYS] completed buckets exist.
     *
     * "Completed" means the bucket's date is strictly before today's calendar date (no partial today).
     * The most recent [DAILY_ANCHOR_WINDOW_DAYS] completed buckets are averaged.
     */
    private suspend fun computeSevenDayDailyMeanBytesPerHour(
        promo: Promo,
        currentTime: Long
    ): Double? {
        val useCase = getDailyUsageBreakdownUseCase ?: return null
        val buckets = try {
            useCase(promo, currentTime)
        } catch (e: Exception) {
            return null
        }
        val todayStartMs = run {
            val cal = Calendar.getInstance()
            cal.timeInMillis = currentTime
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        val completedBuckets = buckets.filter { it.startTimestamp < todayStartMs && it.totalBytes >= 0L }
        if (completedBuckets.size < MIN_DAILY_ANCHOR_DAYS) return null
        val recentCompletedBuckets = completedBuckets.takeLast(DAILY_ANCHOR_WINDOW_DAYS)
        val meanBytesPerDay = recentCompletedBuckets.map { it.totalBytes }.average()
        return meanBytesPerDay / 24.0 // bytes/day -> bytes/hour
    }

    private fun buildAnchoredSummary(
        promo: Promo,
        depletionTime: Long,
        currentTime: Long
    ): String {
        return if (promo.expirationTimestamp != null) {
            "At current pace, ${promo.name} data will run out on ${burnRateEngine.formatTimestamp(depletionTime, currentTime)}."
        } else {
            "At current steady pace, ${promo.name} will run out on ${burnRateEngine.formatTimestamp(depletionTime, currentTime)}."
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

    companion object {
        /** Minimum number of completed daily buckets required to anchor the forecast to the daily mean. */
        const val MIN_DAILY_ANCHOR_DAYS = 3

        /** Maximum number of most-recent completed daily buckets used to compute the daily mean. */
        const val DAILY_ANCHOR_WINDOW_DAYS = 7
    }
}
