package com.loadpredictor.data.stats

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import com.loadpredictor.domain.model.UsageAccessDeniedException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkStatsDataSourceTest {

    private val context: Context = mockk(relaxed = true)
    private val networkStatsManager: NetworkStatsManager = mockk()

    @Test
    fun `queryMobileUsageBytes sums rxBytes and txBytes correctly`() {
        val startTime = 1000L
        val endTime = 5000L

        val mockBucket = mockk<NetworkStats.Bucket>()
        every { mockBucket.rxBytes } returns 3_000_000L
        every { mockBucket.txBytes } returns 2_000_000L

        @Suppress("DEPRECATION")
        every {
            networkStatsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                "",
                startTime,
                endTime
            )
        } returns mockBucket

        val dataSource = NetworkStatsDataSource(
            context = context,
            customNetworkStatsManager = networkStatsManager
        )

        val totalBytes = dataSource.queryMobileUsageBytes(startTime, endTime)

        assertEquals(5_000_000L, totalBytes)
        @Suppress("DEPRECATION")
        verify(exactly = 1) {
            networkStatsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                "",
                startTime,
                endTime
            )
        }
    }

    @Test
    fun `queryMobileUsageBytes throws UsageAccessDeniedException when SecurityException occurs`() {
        val startTime = 1000L
        val endTime = 5000L

        @Suppress("DEPRECATION")
        every {
            networkStatsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                "",
                startTime,
                endTime
            )
        } throws SecurityException("PACKAGE_USAGE_STATS revoked mid-query")

        val dataSource = NetworkStatsDataSource(
            context = context,
            customNetworkStatsManager = networkStatsManager
        )

        // Assert that a distinct UsageAccessDeniedException is thrown to distinguish from genuine 0L usage
        assertThrows(UsageAccessDeniedException::class.java) {
            dataSource.queryMobileUsageBytes(startTime, endTime)
        }
    }

    @Test
    fun `queryMobileUsageBytes returns 0L on invalid or empty time ranges without querying manager`() {
        val dataSource = NetworkStatsDataSource(
            context = context,
            customNetworkStatsManager = networkStatsManager
        )

        // Equal start and end
        assertEquals(0L, dataSource.queryMobileUsageBytes(1000L, 1000L))

        // Start after end
        assertEquals(0L, dataSource.queryMobileUsageBytes(2000L, 1000L))

        // Negative start
        assertEquals(0L, dataSource.queryMobileUsageBytes(-10L, 1000L))

        // Zero or negative end
        assertEquals(0L, dataSource.queryMobileUsageBytes(0L, 0L))

        // Verify manager was never queried for invalid ranges
        verify(exactly = 0) {
            networkStatsManager.querySummaryForDevice(any(), any(), any(), any())
        }
    }

    @Test
    fun `queryDailyUsageBreakdown returns empty list on invalid range`() {
        val dataSource = NetworkStatsDataSource(
            context = context,
            customNetworkStatsManager = networkStatsManager
        )

        val buckets = dataSource.queryDailyUsageBreakdown(5000L, 1000L)
        assertTrue(buckets.isEmpty())
    }

    @Test
    fun `queryDailyUsageBreakdown throws UsageAccessDeniedException on SecurityException`() {
        val startTime = 1000L
        val endTime = 5000L

        @Suppress("DEPRECATION")
        every {
            networkStatsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                "",
                any(),
                any()
            )
        } throws SecurityException("PACKAGE_USAGE_STATS revoked")

        val dataSource = NetworkStatsDataSource(
            context = context,
            customNetworkStatsManager = networkStatsManager
        )

        assertThrows(UsageAccessDeniedException::class.java) {
            dataSource.queryDailyUsageBreakdown(startTime, endTime)
        }
    }
}
