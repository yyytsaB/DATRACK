package com.loadpredictor.data.stats

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class NetworkStatsDataSourceTest {

    private val context: Context = mockk(relaxed = true)
    private val networkStatsManager: NetworkStatsManager = mockk()
    private val zoneId: ZoneId = ZoneId.of("Asia/Manila") // UTC+8

    private fun createBucket(rx: Long, tx: Long): NetworkStats.Bucket {
        val bucket = mockk<NetworkStats.Bucket>()
        every { bucket.rxBytes } returns rx
        every { bucket.txBytes } returns tx
        return bucket
    }

    @Test
    fun `queryDailyUsageBreakdown slices mid-afternoon registration into partial first day and midnight-aligned subsequent days`() {
        val dataSource = NetworkStatsDataSource(context, networkStatsManager)

        // Promo start: Aug 19, 2026 at 15:30:00 UTC+8
        val promoStart = ZonedDateTime.of(2026, 8, 19, 15, 30, 0, 0, zoneId).toInstant().toEpochMilli()
        // Query end: Aug 23, 2026 at 03:45:00 UTC+8
        val queryEnd = ZonedDateTime.of(2026, 8, 23, 3, 45, 0, 0, zoneId).toInstant().toEpochMilli()

        every {
            networkStatsManager.querySummaryForDevice(any(), any(), any(), any())
        } returns createBucket(100L, 50L)

        val buckets = dataSource.queryDailyUsageBreakdown(promoStart, queryEnd, zoneId)

        // Must produce 5 buckets:
        // Bucket 0: Aug 19 15:30 -> Aug 20 00:00 (Partial day 1)
        // Bucket 1: Aug 20 00:00 -> Aug 21 00:00 (Full day 2)
        // Bucket 2: Aug 21 00:00 -> Aug 22 00:00 (Full day 3)
        // Bucket 3: Aug 22 00:00 -> Aug 23 00:00 (Full day 4)
        // Bucket 4: Aug 23 00:00 -> Aug 23 03:45 (Today partial day 5)
        assertEquals(5, buckets.size)

        // Bucket 0: Aug 19 partial
        assertEquals(promoStart, buckets[0].startTimestamp)
        val aug20Midnight = ZonedDateTime.of(2026, 8, 20, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        assertEquals(aug20Midnight, buckets[0].endTimestamp)

        // Bucket 1: Aug 20 full day
        val aug21Midnight = ZonedDateTime.of(2026, 8, 21, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        assertEquals(aug20Midnight, buckets[1].startTimestamp)
        assertEquals(aug21Midnight, buckets[1].endTimestamp)

        // Bucket 2: Aug 21 full day
        val aug22Midnight = ZonedDateTime.of(2026, 8, 22, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        assertEquals(aug21Midnight, buckets[2].startTimestamp)
        assertEquals(aug22Midnight, buckets[2].endTimestamp)

        // Bucket 3: Aug 22 full day
        val aug23Midnight = ZonedDateTime.of(2026, 8, 23, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        assertEquals(aug22Midnight, buckets[3].startTimestamp)
        assertEquals(aug23Midnight, buckets[3].endTimestamp)

        // Bucket 4: Aug 23 today's active partial day
        assertEquals(aug23Midnight, buckets[4].startTimestamp)
        assertEquals(queryEnd, buckets[4].endTimestamp)
    }

    @Test
    fun `queryDailyUsageBreakdown starting exactly at midnight produces clean full day buckets`() {
        val dataSource = NetworkStatsDataSource(context, networkStatsManager)

        val aug20Midnight = ZonedDateTime.of(2026, 8, 20, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        val aug22Noon = ZonedDateTime.of(2026, 8, 22, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli()

        every {
            networkStatsManager.querySummaryForDevice(any(), any(), any(), any())
        } returns createBucket(200L, 100L)

        val buckets = dataSource.queryDailyUsageBreakdown(aug20Midnight, aug22Noon, zoneId)

        // Bucket 0: Aug 20 00:00 -> Aug 21 00:00 (Full day 1)
        // Bucket 1: Aug 21 00:00 -> Aug 22 00:00 (Full day 2)
        // Bucket 2: Aug 22 00:00 -> Aug 22 12:00 (Partial day 3)
        assertEquals(3, buckets.size)
        assertEquals(aug20Midnight, buckets[0].startTimestamp)
        val aug21Midnight = ZonedDateTime.of(2026, 8, 21, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        assertEquals(aug21Midnight, buckets[0].endTimestamp)
    }

    @Test
    fun `queryDailyUsageBreakdown when startTime is greater than or equal to endTime returns empty list`() {
        val dataSource = NetworkStatsDataSource(context, networkStatsManager)
        val now = 1000L
        assertTrue(dataSource.queryDailyUsageBreakdown(now, now).isEmpty())
        assertTrue(dataSource.queryDailyUsageBreakdown(now + 100, now).isEmpty())
    }
}
