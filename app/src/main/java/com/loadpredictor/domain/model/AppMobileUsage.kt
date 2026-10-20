package com.loadpredictor.domain.model

/** UID is the attribution boundary: shared-UID packages cannot be split reliably. */
data class AppMobileUsage(val uid: Int, val label: String, val packageName: String?, val bytes: Long,
    val sharedUid: Boolean = false)

data class AppUsageReport(val apps: List<AppMobileUsage>, val start: Long, val end: Long) {
    val totalBytes: Long get() = apps.sumOf { it.bytes }
    fun share(app: AppMobileUsage): Float = if (totalBytes > 0) (app.bytes.toDouble() / totalBytes).toFloat().coerceIn(0f,1f) else 0f
}

/** Fold all foreground/background, metered and roaming buckets by UID exactly once. */
fun aggregateUidBytes(buckets: List<Pair<Int, Long>>): Map<Int, Long> = buckets
    .filter { it.first >= 0 && it.second > 0 }
    .groupBy({ it.first }, { it.second })
    .mapValues { (_, bytes) -> bytes.fold(0L) { sum, next -> sum + next.coerceAtMost(Long.MAX_VALUE - sum) } }
