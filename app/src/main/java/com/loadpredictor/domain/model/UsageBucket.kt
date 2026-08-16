package com.loadpredictor.domain.model

/**
 * Pure Kotlin domain model representing mobile data consumption in a discrete time bucket.
 *
 * @property startTimestamp Start of the bucket interval in epoch milliseconds.
 * @property endTimestamp End of the bucket interval in epoch milliseconds.
 * @property rxBytes Received (download) mobile data in bytes.
 * @property txBytes Transmitted (upload) mobile data in bytes.
 */
data class UsageBucket(
    val startTimestamp: Long,
    val endTimestamp: Long,
    val rxBytes: Long,
    val txBytes: Long
) {
    val totalBytes: Long
        get() = rxBytes + txBytes
}
