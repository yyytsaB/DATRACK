package com.loadpredictor.domain.model

/**
 * Supported time range filters for the Usage History analytics screen.
 *
 * @param label Short user-facing label (e.g. "7D", "30D", "Lifetime").
 * @param windowMs Duration window in milliseconds (Lifetime capped at 90 days app policy cap).
 */
enum class HistoryTimeRange(
    val label: String,
    val windowMs: Long
) {
    LAST_7_DAYS("7D", 7L * 24 * 60 * 60 * 1000L),
    LAST_30_DAYS("30D", 30L * 24 * 60 * 60 * 1000L),
    LIFETIME("Lifetime", 90L * 24 * 60 * 60 * 1000L) // 90-day deliberate app policy cap
}
