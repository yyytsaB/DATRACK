package com.loadpredictor.data.stats

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import com.loadpredictor.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Uses Usage Access and aggregate mobile interfaces; never IMSI, VPN or packet inspection. */
class AppMobileUsageDataSource(context: Context) {
    private val app = context.applicationContext
    suspend fun query(start: Long, end: Long): AppUsageReport = withContext(Dispatchers.IO) {
        check(UsageAccessHelper(app).hasUsageAccessPermission()) { "Allow Usage Access to see usage by app." }
        if (start >= end) return@withContext AppUsageReport(emptyList(), start, end)
        val manager = app.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: error("Android usage statistics are unavailable.")
        @Suppress("DEPRECATION")
        val stats = manager.querySummary(ConnectivityManager.TYPE_MOBILE, null, start, end)
            ?: error("Android could not return app usage. Try again.")
        val buckets = mutableListOf<Pair<Int, Long>>()
        stats.use {
            val bucket = NetworkStats.Bucket()
            while (it.hasNextBucket()) {
                it.getNextBucket(bucket)
                val rx = bucket.rxBytes.coerceAtLeast(0)
                val tx = bucket.txBytes.coerceAtLeast(0).coerceAtMost(Long.MAX_VALUE - rx)
                buckets += bucket.uid to rx + tx
            }
        }
        val pm = app.packageManager
        val rows = aggregateUidBytes(buckets).map { (uid, bytes) ->
            val packages = pm.getPackagesForUid(uid).orEmpty()
            val pkg = packages.sorted().firstOrNull()
            val label = if (uid < 10000) "Android system" else try {
                if (pkg == null) "Unidentified app" else {
                    @Suppress("DEPRECATION")
                    val info = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(info).toString()
                }
            } catch (_: android.content.pm.PackageManager.NameNotFoundException) { "Unidentified app" }
            AppMobileUsage(uid, if (packages.size > 1 && uid >= 10000) "$label + shared apps" else label,
                pkg, bytes, packages.size > 1)
        }.sortedWith(compareByDescending<AppMobileUsage> { it.bytes }.thenBy { it.uid })
        AppUsageReport(rows, start, end)
    }
}
