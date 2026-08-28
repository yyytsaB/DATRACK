package com.loadpredictor.domain.model

/**
 * Pure Kotlin domain model representing a behavioral insight on weekday vs weekend data consumption.
 */
sealed interface UsagePatternInsight {

    /**
     * Insufficient completed weekday (< 3) or weekend (< 2) days to establish a reliable comparative pattern.
     */
    data object InsufficientData : UsagePatternInsight

    /**
     * Computed comparative usage pattern between completed weekdays and weekend days.
     *
     * @property weekdayAvgBytes Average daily consumption across completed weekdays (Mon-Fri) in bytes.
     * @property weekendAvgBytes Average daily consumption across completed weekend days (Sat-Sun) in bytes.
     * @property ratio Ratio of higher-use category to lower-use category.
     * @property patternType Classification of the pattern (WEEKEND_HEAVY, WEEKDAY_HEAVY, BALANCED).
     * @property summary Plain-language human-readable insight string.
     */
    data class Pattern(
        val weekdayAvgBytes: Long,
        val weekendAvgBytes: Long,
        val ratio: Double,
        val patternType: PatternType,
        val summary: String
    ) : UsagePatternInsight
}

/**
 * Pattern classification category.
 */
enum class PatternType {
    WEEKEND_HEAVY,
    WEEKDAY_HEAVY,
    BALANCED
}
