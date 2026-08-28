package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.model.PatternType
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.model.UsagePatternInsight
import com.loadpredictor.domain.time.DefaultTimeProvider
import com.loadpredictor.domain.time.TimeProvider
import com.loadpredictor.util.DataFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * UseCase to compute comparative behavioral data consumption patterns between weekdays and weekends.
 *
 * Enforces strict sample gating:
 * 1. Excludes the in-progress "today" bucket so partial day usage does not distort the averages.
 * 2. Requires at least [MIN_COMPLETED_WEEKDAYS] (3) completed weekday buckets AND
 *    at least [MIN_COMPLETED_WEEKENDS] (2) completed weekend buckets to return a [UsagePatternInsight.Pattern].
 * 3. Applies a 25% difference threshold (>= 1.25x) before classifying as Weekend-Heavy or Weekday-Heavy.
 *
 * Follows Engineering Rule #1 (zero Android dependencies in domain layer).
 */
class GetUsagePatternInsightUseCase(
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) {

    companion object {
        const val MIN_COMPLETED_WEEKDAYS = 3
        const val MIN_COMPLETED_WEEKENDS = 2
        const val HEAVY_PATTERN_THRESHOLD = 1.25
    }

    operator fun invoke(
        dailyBuckets: List<UsageBucket>,
        currentTime: Long = timeProvider.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): UsagePatternInsight {
        if (dailyBuckets.isEmpty()) {
            return UsagePatternInsight.InsufficientData
        }

        // 1. Determine start of current day in target timezone to exclude in-progress today bucket
        val todayCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = currentTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = todayCal.timeInMillis

        val completedBuckets = dailyBuckets.filter { it.startTimestamp < startOfToday }

        // 2. Classify completed buckets into Weekdays (Mon-Fri) vs Weekends (Sat-Sun)
        val weekdayBuckets = mutableListOf<UsageBucket>()
        val weekendBuckets = mutableListOf<UsageBucket>()

        for (bucket in completedBuckets) {
            val cal = Calendar.getInstance(timeZone).apply {
                timeInMillis = bucket.startTimestamp
            }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                weekendBuckets.add(bucket)
            } else {
                weekdayBuckets.add(bucket)
            }
        }

        // 3. Sample sufficiency check
        if (weekdayBuckets.size < MIN_COMPLETED_WEEKDAYS || weekendBuckets.size < MIN_COMPLETED_WEEKENDS) {
            return UsagePatternInsight.InsufficientData
        }

        // 4. Calculate daily averages
        val weekdayAvgBytes = (weekdayBuckets.map { it.totalBytes }.average()).toLong()
        val weekendAvgBytes = (weekendBuckets.map { it.totalBytes }.average()).toLong()

        // 5. Determine pattern ratio and classification
        val patternType: PatternType
        val ratio: Double
        val summary: String

        val formattedWeekday = DataFormatter.formatBytes(weekdayAvgBytes)
        val formattedWeekend = DataFormatter.formatBytes(weekendAvgBytes)

        when {
            weekdayAvgBytes > 0L && weekendAvgBytes >= (weekdayAvgBytes.toDouble() * HEAVY_PATTERN_THRESHOLD).toLong() -> {
                patternType = PatternType.WEEKEND_HEAVY
                ratio = weekendAvgBytes.toDouble() / weekdayAvgBytes.toDouble()
                val ratioText = formatRatio(ratio)
                summary = "You use ~$ratioText more data on weekends (avg $formattedWeekend/day vs $formattedWeekday/day weekdays)"
            }
            weekendAvgBytes > 0L && weekdayAvgBytes >= (weekendAvgBytes.toDouble() * HEAVY_PATTERN_THRESHOLD).toLong() -> {
                patternType = PatternType.WEEKDAY_HEAVY
                ratio = weekdayAvgBytes.toDouble() / weekendAvgBytes.toDouble()
                val ratioText = formatRatio(ratio)
                summary = "You use ~$ratioText more data on weekdays (avg $formattedWeekday/day vs $formattedWeekend/day weekends)"
            }
            else -> {
                patternType = PatternType.BALANCED
                ratio = if (weekdayAvgBytes > 0L) weekendAvgBytes.toDouble() / weekdayAvgBytes.toDouble() else 1.0
                val combinedAvg = (weekdayAvgBytes + weekendAvgBytes) / 2
                val formattedCombined = DataFormatter.formatBytes(combinedAvg)
                summary = "Your data usage is evenly balanced across weekdays and weekends (~$formattedCombined/day)"
            }
        }

        return UsagePatternInsight.Pattern(
            weekdayAvgBytes = weekdayAvgBytes,
            weekendAvgBytes = weekendAvgBytes,
            ratio = ratio,
            patternType = patternType,
            summary = summary
        )
    }

    private fun formatRatio(ratio: Double): String {
        val rounded1Dec = String.format(Locale.US, "%.1f", ratio)
        return if (rounded1Dec.endsWith(".0")) {
            "${rounded1Dec.substringBefore(".0")}x"
        } else {
            "${rounded1Dec}x"
        }
    }
}
