package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.model.PatternType
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.model.UsagePatternInsight
import com.loadpredictor.domain.time.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class GetUsagePatternInsightUseCaseTest {

    private val timeZone = TimeZone.getTimeZone("UTC")

    private class FakeTimeProvider(var time: Long) : TimeProvider {
        override fun currentTimeMillis(): Long = time
    }

    private fun getUtcTimestamp(year: Int, month: Int, day: Int, hour: Int = 0): Long {
        return Calendar.getInstance(timeZone).apply {
            set(year, month - 1, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private val MB = 1024L * 1024L

    @Test
    fun `returns InsufficientData when total buckets are empty`() {
        val useCase = GetUsagePatternInsightUseCase()
        val result = useCase(
            dailyBuckets = emptyList(),
            currentTime = getUtcTimestamp(2026, 8, 25),
            timeZone = timeZone
        )
        assertTrue(result is UsagePatternInsight.InsufficientData)
    }

    @Test
    fun `returns InsufficientData when only 1 weekday and 1 weekend day are present`() {
        val useCase = GetUsagePatternInsightUseCase()

        // Mon Aug 17 and Sat Aug 22 (Current time is Tue Aug 25)
        val buckets = listOf(
            UsageBucket(getUtcTimestamp(2026, 8, 17), getUtcTimestamp(2026, 8, 18), 100 * MB, 0L),
            UsageBucket(getUtcTimestamp(2026, 8, 22), getUtcTimestamp(2026, 8, 23), 200 * MB, 0L)
        )

        val result = useCase(
            dailyBuckets = buckets,
            currentTime = getUtcTimestamp(2026, 8, 25),
            timeZone = timeZone
        )

        assertTrue(result is UsagePatternInsight.InsufficientData)
    }

    @Test
    fun `returns InsufficientData when fewer than 3 weekdays are present even with 2 weekend days`() {
        val useCase = GetUsagePatternInsightUseCase()

        // 2 weekdays (Mon Aug 17, Tue Aug 18) and 2 weekend days (Sat Aug 22, Sun Aug 23)
        val buckets = listOf(
            UsageBucket(getUtcTimestamp(2026, 8, 17), getUtcTimestamp(2026, 8, 18), 100 * MB, 0L),
            UsageBucket(getUtcTimestamp(2026, 8, 18), getUtcTimestamp(2026, 8, 19), 100 * MB, 0L),
            UsageBucket(getUtcTimestamp(2026, 8, 22), getUtcTimestamp(2026, 8, 23), 200 * MB, 0L),
            UsageBucket(getUtcTimestamp(2026, 8, 23), getUtcTimestamp(2026, 8, 24), 200 * MB, 0L)
        )

        val result = useCase(
            dailyBuckets = buckets,
            currentTime = getUtcTimestamp(2026, 8, 25),
            timeZone = timeZone
        )

        assertTrue(result is UsagePatternInsight.InsufficientData)
    }

    @Test
    fun `returns InsufficientData when fewer than 2 weekend days are present even with 5 weekdays`() {
        val useCase = GetUsagePatternInsightUseCase()

        // 5 weekdays (Mon-Fri) + only 1 weekend day (Sat)
        val buckets = listOf(
            UsageBucket(getUtcTimestamp(2026, 8, 17), getUtcTimestamp(2026, 8, 18), 100 * MB, 0L), // Mon
            UsageBucket(getUtcTimestamp(2026, 8, 18), getUtcTimestamp(2026, 8, 19), 100 * MB, 0L), // Tue
            UsageBucket(getUtcTimestamp(2026, 8, 19), getUtcTimestamp(2026, 8, 20), 100 * MB, 0L), // Wed
            UsageBucket(getUtcTimestamp(2026, 8, 20), getUtcTimestamp(2026, 8, 21), 100 * MB, 0L), // Thu
            UsageBucket(getUtcTimestamp(2026, 8, 21), getUtcTimestamp(2026, 8, 22), 100 * MB, 0L), // Fri
            UsageBucket(getUtcTimestamp(2026, 8, 22), getUtcTimestamp(2026, 8, 23), 200 * MB, 0L)  // Sat
        )

        val result = useCase(
            dailyBuckets = buckets,
            currentTime = getUtcTimestamp(2026, 8, 25),
            timeZone = timeZone
        )

        assertTrue(result is UsagePatternInsight.InsufficientData)
    }

    @Test
    fun `excludes in-progress today bucket when today is a weekday`() {
        val useCase = GetUsagePatternInsightUseCase()

        // 2 completed weekend days (Sat Aug 15, Sun Aug 16) + 3 completed weekdays (Mon, Tue, Wed) + 1 partial weekday (Thu 10 AM)
        val prevSat = UsageBucket(getUtcTimestamp(2026, 8, 15), getUtcTimestamp(2026, 8, 16), 200 * MB, 0L)
        val prevSun = UsageBucket(getUtcTimestamp(2026, 8, 16), getUtcTimestamp(2026, 8, 17), 200 * MB, 0L)
        val mon = UsageBucket(getUtcTimestamp(2026, 8, 17), getUtcTimestamp(2026, 8, 18), 100 * MB, 0L)
        val tue = UsageBucket(getUtcTimestamp(2026, 8, 18), getUtcTimestamp(2026, 8, 19), 100 * MB, 0L)
        val wed = UsageBucket(getUtcTimestamp(2026, 8, 19), getUtcTimestamp(2026, 8, 20), 100 * MB, 0L)

        // Today is Thursday Aug 20 at 10:00 AM (partial day with only 5 MB so far)
        val todayThuPartial = UsageBucket(getUtcTimestamp(2026, 8, 20), getUtcTimestamp(2026, 8, 20, 10), 5 * MB, 0L)

        val currentTimeThuMorning = getUtcTimestamp(2026, 8, 20, 10)

        val result = useCase(
            dailyBuckets = listOf(prevSat, prevSun, mon, tue, wed, todayThuPartial),
            currentTime = currentTimeThuMorning,
            timeZone = timeZone
        )

        assertTrue(result is UsagePatternInsight.Pattern)
        val pattern = result as UsagePatternInsight.Pattern

        // Weekday average must be strictly (100 + 100 + 100) / 3 = 100 MB, NOT (100 + 100 + 100 + 5) / 4 = 76.25 MB
        assertEquals(100 * MB, pattern.weekdayAvgBytes)
        assertEquals(200 * MB, pattern.weekendAvgBytes)
        assertEquals(2.0, pattern.ratio, 0.01)
        assertEquals(PatternType.WEEKEND_HEAVY, pattern.patternType)
    }

    @Test
    fun `excludes in-progress today bucket when today is a weekend day`() {
        val useCase = GetUsagePatternInsightUseCase()

        // 3 completed weekdays (Mon, Tue, Wed) + 2 completed weekend days (Sat Aug 15, Sun Aug 16) + 1 partial weekend day (Sat Aug 22 9 AM)
        val mon = UsageBucket(getUtcTimestamp(2026, 8, 17), getUtcTimestamp(2026, 8, 18), 100 * MB, 0L)
        val tue = UsageBucket(getUtcTimestamp(2026, 8, 18), getUtcTimestamp(2026, 8, 19), 100 * MB, 0L)
        val wed = UsageBucket(getUtcTimestamp(2026, 8, 19), getUtcTimestamp(2026, 8, 20), 100 * MB, 0L)
        val prevSat = UsageBucket(getUtcTimestamp(2026, 8, 15), getUtcTimestamp(2026, 8, 16), 250 * MB, 0L)
        val prevSun = UsageBucket(getUtcTimestamp(2026, 8, 16), getUtcTimestamp(2026, 8, 17), 250 * MB, 0L)

        // Today is Sat Aug 22 at 9:00 AM (partial day with only 10 MB)
        val todaySatPartial = UsageBucket(getUtcTimestamp(2026, 8, 22), getUtcTimestamp(2026, 8, 22, 9), 10 * MB, 0L)

        val currentTimeSatMorning = getUtcTimestamp(2026, 8, 22, 9)

        val result = useCase(
            dailyBuckets = listOf(prevSat, prevSun, mon, tue, wed, todaySatPartial),
            currentTime = currentTimeSatMorning,
            timeZone = timeZone
        )

        assertTrue(result is UsagePatternInsight.Pattern)
        val pattern = result as UsagePatternInsight.Pattern

        // Weekend average must be strictly (250 + 250) / 2 = 250 MB, NOT (250 + 250 + 10) / 3 = 170 MB
        assertEquals(100 * MB, pattern.weekdayAvgBytes)
        assertEquals(250 * MB, pattern.weekendAvgBytes)
        assertEquals(2.5, pattern.ratio, 0.01)
        assertEquals(PatternType.WEEKEND_HEAVY, pattern.patternType)
    }

    @Test
    fun `weekend heavy pattern computes 2x ratio and expected summary`() {
        val useCase = GetUsagePatternInsightUseCase()

        val mon = UsageBucket(getUtcTimestamp(2026, 8, 17), getUtcTimestamp(2026, 8, 18), 100 * MB, 0L)
        val tue = UsageBucket(getUtcTimestamp(2026, 8, 18), getUtcTimestamp(2026, 8, 19), 100 * MB, 0L)
        val wed = UsageBucket(getUtcTimestamp(2026, 8, 19), getUtcTimestamp(2026, 8, 20), 100 * MB, 0L)
        val sat = UsageBucket(getUtcTimestamp(2026, 8, 22), getUtcTimestamp(2026, 8, 23), 200 * MB, 0L)
        val sun = UsageBucket(getUtcTimestamp(2026, 8, 23), getUtcTimestamp(2026, 8, 24), 200 * MB, 0L)

        val result = useCase(
            dailyBuckets = listOf(mon, tue, wed, sat, sun),
            currentTime = getUtcTimestamp(2026, 8, 25),
            timeZone = timeZone
        )

        assertTrue(result is UsagePatternInsight.Pattern)
        val pattern = result as UsagePatternInsight.Pattern

        assertEquals(100 * MB, pattern.weekdayAvgBytes)
        assertEquals(200 * MB, pattern.weekendAvgBytes)
        assertEquals(2.0, pattern.ratio, 0.01)
        assertEquals(PatternType.WEEKEND_HEAVY, pattern.patternType)
        assertEquals("You use ~2x more data on weekends (avg 200 MB/day vs 100 MB/day weekdays)", pattern.summary)
    }

    @Test
    fun `weekday heavy pattern computes expected ratio and summary`() {
        val useCase = GetUsagePatternInsightUseCase()

        val mon = UsageBucket(getUtcTimestamp(2026, 8, 17), getUtcTimestamp(2026, 8, 18), 300 * MB, 0L)
        val tue = UsageBucket(getUtcTimestamp(2026, 8, 18), getUtcTimestamp(2026, 8, 19), 300 * MB, 0L)
        val wed = UsageBucket(getUtcTimestamp(2026, 8, 19), getUtcTimestamp(2026, 8, 20), 300 * MB, 0L)
        val sat = UsageBucket(getUtcTimestamp(2026, 8, 22), getUtcTimestamp(2026, 8, 23), 150 * MB, 0L)
        val sun = UsageBucket(getUtcTimestamp(2026, 8, 23), getUtcTimestamp(2026, 8, 24), 150 * MB, 0L)

        val result = useCase(
            dailyBuckets = listOf(mon, tue, wed, sat, sun),
            currentTime = getUtcTimestamp(2026, 8, 25),
            timeZone = timeZone
        )

        assertTrue(result is UsagePatternInsight.Pattern)
        val pattern = result as UsagePatternInsight.Pattern

        assertEquals(300 * MB, pattern.weekdayAvgBytes)
        assertEquals(150 * MB, pattern.weekendAvgBytes)
        assertEquals(2.0, pattern.ratio, 0.01)
        assertEquals(PatternType.WEEKDAY_HEAVY, pattern.patternType)
        assertEquals("You use ~2x more data on weekdays (avg 300 MB/day vs 150 MB/day weekends)", pattern.summary)
    }

    @Test
    fun `balanced pattern when usage within 25 percent`() {
        val useCase = GetUsagePatternInsightUseCase()

        val mon = UsageBucket(getUtcTimestamp(2026, 8, 17), getUtcTimestamp(2026, 8, 18), 100 * MB, 0L)
        val tue = UsageBucket(getUtcTimestamp(2026, 8, 18), getUtcTimestamp(2026, 8, 19), 100 * MB, 0L)
        val wed = UsageBucket(getUtcTimestamp(2026, 8, 19), getUtcTimestamp(2026, 8, 20), 100 * MB, 0L)
        val sat = UsageBucket(getUtcTimestamp(2026, 8, 22), getUtcTimestamp(2026, 8, 23), 110 * MB, 0L)
        val sun = UsageBucket(getUtcTimestamp(2026, 8, 23), getUtcTimestamp(2026, 8, 24), 110 * MB, 0L)

        val result = useCase(
            dailyBuckets = listOf(mon, tue, wed, sat, sun),
            currentTime = getUtcTimestamp(2026, 8, 25),
            timeZone = timeZone
        )

        assertTrue(result is UsagePatternInsight.Pattern)
        val pattern = result as UsagePatternInsight.Pattern

        assertEquals(100 * MB, pattern.weekdayAvgBytes)
        assertEquals(110 * MB, pattern.weekendAvgBytes)
        assertEquals(PatternType.BALANCED, pattern.patternType)
        assertEquals("Your data usage is evenly balanced across weekdays and weekends (~105 MB/day)", pattern.summary)
    }
}
