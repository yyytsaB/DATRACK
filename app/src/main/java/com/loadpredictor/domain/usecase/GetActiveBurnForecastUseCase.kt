package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.engine.BurnRateEngine
import com.loadpredictor.domain.model.BurnForecast
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.UsageAccessDeniedException
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.time.DefaultTimeProvider
import com.loadpredictor.domain.time.TimeProvider
import com.loadpredictor.domain.usecase.GetDailyUsageBreakdownUseCase
import java.util.Calendar
import com.loadpredictor.domain.model.isCompletedDay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.transformLatest

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
    suspend fun execute(activePromo: Promo?, currentTime: Long? = null): BurnForecastResult = try {
        com.loadpredictor.domain.sync.DataOperationGate.exclusive {
            val at=currentTime ?: timeProvider.currentTimeMillis()
            val result=evaluate(activePromo,at)
            val category=when(result) {
                is BurnForecastResult.Success -> "success"
                BurnForecastResult.PermissionRequired -> "permission_required"
                BurnForecastResult.NoActivePromo -> "no_active_promo"
                is BurnForecastResult.Error -> "unavailable"
            }
            com.loadpredictor.domain.diagnostics.ObservationLog.record(
                com.loadpredictor.domain.diagnostics.ObservationEvent(at,"forecast",category,
                    activePromo?.startTimestamp,activePromo?.let {minOf(at,it.expirationTimestamp ?: at)},
                    null))
            result
        }
    } catch(e:CancellationException){throw e} catch(e:Exception) {
        BurnForecastResult.Error(e.message ?: "Reading unavailable")
    }

    private suspend fun evaluate(activePromo:Promo?,currentTime:Long?):BurnForecastResult {
        if (activePromo == null) {
            return BurnForecastResult.NoActivePromo
        }
        return try {
            val now = currentTime ?: timeProvider.currentTimeMillis()
            if (now < activePromo.lastSyncTimestamp) return BurnForecastResult.Error("A newer reading is already available. Refresh again.")
            val usedBytes = usageRepository.queryMobileUsageBytes(
                startTime = activePromo.startTimestamp,
                endTime = minOf(now, activePromo.expirationTimestamp ?: now)
            )
            com.loadpredictor.domain.diagnostics.ObservationLog.record(
                com.loadpredictor.domain.diagnostics.ObservationEvent(now,"measurement","observed",activePromo.startTimestamp,
                    minOf(now,activePromo.expirationTimestamp ?: now),usedBytes))
            if(!activePromo.isExpired(now) && activePromo.correctionMeasured!=null && usedBytes<activePromo.correctionMeasured)
                return BurnForecastResult.Error("Android revised usage before the carrier checkpoint. Check against a fresh carrier balance.")
            if (!activePromo.isExpired(now) && activePromo.remainingBytes(usedBytes)>0 &&
                usedBytes < activePromo.lastSyncDataUsedBytes && now >= activePromo.lastSyncTimestamp) {
                promoRepository.updateObservedSyncState(activePromo,null,usedBytes,now)
                return BurnForecastResult.Error("Android revised its usage history. Refresh to recalibrate the estimate.")
            }
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
            // When >= MIN_INTERVAL_DAYS completed buckets exist, a confidence interval is also derived
            // from the observed standard deviation.
            // The EMA path (lastActiveBurnRate, shouldPersistSyncState, updateSyncState) is unchanged.
            val recentCompletedBuckets = getRecentCompletedDailyBuckets(activePromo, now)
            val anchoredForecast: BurnForecast = run {
                val dailyMeanRate = computeDailyMeanBytesPerHour(recentCompletedBuckets)
                if (dailyMeanRate != null && dailyMeanRate > 0.0 &&
                    forecast.pace != BurnPace.INSUFFICIENT_DATA &&
                    forecast.pace != BurnPace.DEPLETED && forecast.pace != BurnPace.EXPIRED
                ) {
                    val remainingHours = forecast.dataRemainingBytes.toDouble() / dailyMeanRate
                    val remainingMs = (remainingHours * 3_600_000.0).toLong()
                    val anchoredDepletionTime = now + remainingMs
                    val intervalPair = computeDepletionInterval(
                        buckets = recentCompletedBuckets,
                        remainingBytes = forecast.dataRemainingBytes,
                        now = now,
                        promo = activePromo
                    )
                    forecast.copy(
                        burnRateBytesPerHour = dailyMeanRate,
                        modelSource = "7-day completed-day average",
                        pace = projectionPace(activePromo, anchoredDepletionTime),
                        estimatedDepletionTimestamp = anchoredDepletionTime,
                        plainLanguageSummary = buildAnchoredSummary(activePromo, anchoredDepletionTime, now),
                        depletionEarlyTimestamp = intervalPair?.first,
                        depletionLateTimestamp = intervalPair?.second
                    )
                } else {
                    forecast
                }
            }

            if (shouldPersistSyncState && forecast.burnRateBytesPerHour > 0.0) {
                if (!promoRepository.updateObservedSyncState(activePromo,forecast.burnRateBytesPerHour,usedBytes,now)) {
                    return BurnForecastResult.Error("The reading changed during refresh. Try again.")
                }
            }
            if (!promoRepository.isCurrentConfiguration(activePromo)) {
                return BurnForecastResult.Error("The selected promo changed. Refresh for its current reading.")
            }
            val consistentForecast = if (anchoredForecast.pace != BurnPace.EXPIRED &&
                anchoredForecast.pace != BurnPace.DEPLETED && anchoredForecast.pace != BurnPace.INSUFFICIENT_DATA &&
                anchoredForecast.estimatedDepletionTimestamp != null) {
                anchoredForecast.copy(pace = projectionPace(activePromo, anchoredForecast.estimatedDepletionTimestamp),
                    plainLanguageSummary = buildAnchoredSummary(activePromo, anchoredForecast.estimatedDepletionTimestamp, now))
            } else anchoredForecast
            BurnForecastResult.Success(consistentForecast)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            if (throwable is UsageAccessDeniedException || throwable.cause is UsageAccessDeniedException) {
                BurnForecastResult.PermissionRequired
            } else {
                BurnForecastResult.Error(throwable.localizedMessage ?: "Unexpected error during forecast calculation")
            }
        }
    }

    /**
     * Extracts up to [DAILY_ANCHOR_WINDOW_DAYS] most recent completed daily buckets.
     * "Completed" means the bucket's date is strictly before today's calendar date (no partial today).
     */
    private suspend fun getRecentCompletedDailyBuckets(
        promo: Promo,
        currentTime: Long
    ): List<UsageBucket> {
        val useCase = getDailyUsageBreakdownUseCase ?: return emptyList()
        val buckets = try {
            useCase.completedDays(promo, currentTime, DAILY_ANCHOR_WINDOW_DAYS.toLong())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return emptyList()
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
        val completedBuckets = buckets.filter { it.isCompletedDay(currentTime) && it.totalBytes >= 0L }
        return completedBuckets.takeLast(DAILY_ANCHOR_WINDOW_DAYS)
    }

    /**
     * Computes a 7-day rolling daily mean burn rate in bytes/hour from completed daily buckets.
     *
     * Returns null if fewer than [MIN_DAILY_ANCHOR_DAYS] completed buckets exist.
     */
    internal fun computeDailyMeanBytesPerHour(buckets: List<UsageBucket>): Double? {
        if (buckets.size < MIN_DAILY_ANCHOR_DAYS) return null
        val meanBytesPerDay = buckets.map { it.totalBytes }.average()
        return meanBytesPerDay / 24.0 // bytes/day -> bytes/hour
    }

    /**
     * Computes the depletion confidence interval [Pair] of (earlyTs, lateTs) using sample standard deviation.
     *
     * Returns null if:
     * - fewer than [MIN_INTERVAL_DAYS] completed buckets exist
     * - [remainingBytes] <= 0
     * - late rate <= 0 (i.e. σ >= μ, high variance/noise)
     * - half-spread in whole days <= 0
     */
    internal fun computeDepletionInterval(
        buckets: List<UsageBucket>,
        remainingBytes: Long,
        now: Long,
        promo: Promo
    ): Pair<Long, Long>? {
        if (buckets.size < MIN_INTERVAL_DAYS || remainingBytes <= 0L) return null

        val bytesList = buckets.map { it.totalBytes.toDouble() }
        val n = bytesList.size
        val mean = bytesList.average()
        if (mean <= 0.0) return null

        val variance = bytesList.sumOf { (it - mean) * (it - mean) } / (n - 1)
        val stdDev = kotlin.math.sqrt(variance)

        val earlyRate = (mean + stdDev) / 24.0
        val lateRate = (mean - stdDev) / 24.0

        if (lateRate <= 0.0) return null

        val earlyHours = remainingBytes.toDouble() / earlyRate
        val lateHours = remainingBytes.toDouble() / lateRate

        val earlyTs = now + (earlyHours * 3_600_000.0).toLong()
        var lateTs = now + (lateHours * 3_600_000.0).toLong()

        if (promo.expirationTimestamp != null) {
            lateTs = minOf(lateTs, promo.expirationTimestamp)
        }

        val halfSpreadDays = kotlin.math.round((lateTs - earlyTs) / 86_400_000.0 / 2.0).toInt()
        if (halfSpreadDays <= 0) return null

        return Pair(earlyTs, lateTs)
    }

    private fun projectionPace(promo: Promo, depletion: Long): BurnPace {
        val expiry = promo.expirationTimestamp ?: return BurnPace.ON_TRACK
        return when {
            depletion < expiry -> BurnPace.BURNING_FAST
            depletion > expiry + 12L * 3_600_000L -> BurnPace.CONSERVATIVE
            else -> BurnPace.ON_TRACK
        }
    }

    private fun buildAnchoredSummary(
        promo: Promo,
        depletionTime: Long,
        currentTime: Long
    ): String {
        return if (promo.expirationTimestamp != null && depletionTime >= promo.expirationTimestamp) {
            "Expected to last until ${burnRateEngine.formatTimestamp(promo.expirationTimestamp, currentTime)} when ${promo.name} expires."
        } else if (promo.expirationTimestamp != null) {
            "At current pace, ${promo.name} data will run out on ${burnRateEngine.formatTimestamp(depletionTime, currentTime)}."
        } else {
            "At current steady pace, ${promo.name} will run out on ${burnRateEngine.formatTimestamp(depletionTime, currentTime)}."
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<BurnForecastResult> = promoRepository.getActivePromo()
        .transformLatest { activePromo ->
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

        /** Minimum completed daily buckets required to compute a confidence interval. */
        const val MIN_INTERVAL_DAYS = 5

        /** Maximum number of most-recent completed daily buckets used to compute the daily mean and interval. */
        const val DAILY_ANCHOR_WINDOW_DAYS = 7
    }
}
