package com.loadpredictor.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Encapsulates paired formatted data amount strings to ensure uniform precision in paired UI layouts.
 */
data class FormattedDataPair(
    val remainingFormatted: String,
    val totalFormatted: String
)

/**
 * Shared utility for formatting data byte quantities and calendar dates with adaptive,
 * synchronized precision and consistent year-conditional rules across all app surfaces.
 */
object DataFormatter {

    /**
     * Formats remaining data alongside its total allowance, guaranteeing that both values
     * share matching decimal precision across all paired UI displays (e.g. "23.99 GB of 24.00 GB").
     *
     * @param remainingBytes The remaining balance in bytes.
     * @param totalAllowanceBytes The promo's total allowance in bytes.
     */
    fun formatDataPair(remainingBytes: Long, totalAllowanceBytes: Long): FormattedDataPair {
        val nonNegativeRemaining = remainingBytes.coerceAtLeast(0L)
        val nonNegativeTotal = totalAllowanceBytes.coerceAtLeast(0L)

        val remainingGb = nonNegativeRemaining.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val totalGb = nonNegativeTotal.toDouble() / (1024.0 * 1024.0 * 1024.0)

        if (totalGb >= 1.0) {
            val standardRemaining1Dec = String.format(Locale.US, "%.1f GB", remainingGb)
            val standardTotal1Dec = String.format(Locale.US, "%.1f GB", totalGb)

            // If remaining has nonzero usage but 1-decimal rounding causes it to look identical to total allowance:
            if (nonNegativeRemaining < nonNegativeTotal && standardRemaining1Dec == standardTotal1Dec) {
                return FormattedDataPair(
                    remainingFormatted = String.format(Locale.US, "%.2f GB", remainingGb),
                    totalFormatted = String.format(Locale.US, "%.2f GB", totalGb)
                )
            }

            if (remainingGb >= 1.0) {
                return FormattedDataPair(
                    remainingFormatted = standardRemaining1Dec,
                    totalFormatted = standardTotal1Dec
                )
            } else {
                val remainingMb = nonNegativeRemaining.toDouble() / (1024.0 * 1024.0)
                return FormattedDataPair(
                    remainingFormatted = String.format(Locale.US, "%.0f MB", remainingMb),
                    totalFormatted = standardTotal1Dec
                )
            }
        }

        // Sub-GB total allowance
        val remainingMb = nonNegativeRemaining.toDouble() / (1024.0 * 1024.0)
        val totalMb = nonNegativeTotal.toDouble() / (1024.0 * 1024.0)
        return FormattedDataPair(
            remainingFormatted = String.format(Locale.US, "%.0f MB", remainingMb),
            totalFormatted = String.format(Locale.US, "%.0f MB", totalMb)
        )
    }

    /**
     * Formats a single data amount (e.g. remaining balance or allowance).
     * When [totalAllowanceBytes] is provided, uses [formatDataPair] to resolve precision.
     */
    fun formatDataAmount(bytes: Long, totalAllowanceBytes: Long? = null): String {
        if (totalAllowanceBytes != null && totalAllowanceBytes > 0L) {
            return formatDataPair(bytes, totalAllowanceBytes).remainingFormatted
        }
        return formatBytes(bytes)
    }

    /**
     * Shorthand formatter for standalone byte quantities (e.g. daily breakdown, allowance presets).
     */
    fun formatBytes(bytes: Long): String {
        val nonNegativeBytes = bytes.coerceAtLeast(0L)
        val gb = nonNegativeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1.0) {
            return String.format(Locale.US, "%.1f GB", gb)
        }
        val mb = nonNegativeBytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.0f MB", mb)
    }

    /**
     * Checks if two epoch timestamps fall within the same calendar year.
     */
    fun isSameCalendarYear(
        timestamp1: Long,
        timestamp2: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val cal1 = Calendar.getInstance(timeZone).apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance(timeZone).apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    /**
     * Formats a projected depletion timestamp with contextual precision:
     * - Within next 7 days: "Tuesday at 4:15 PM"
     * - Beyond 7 days in the current calendar year: "Oct 1 at 3:00 PM"
     * - Beyond current calendar year: "Mar 3, 2027 at 3:16 PM"
     */
    fun formatDepletionDateTime(
        timestamp: Long,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val diffMs = timestamp - now
        val daysDiff = (diffMs / (1000 * 60 * 60 * 24)).toInt()

        return if (diffMs >= 0 && daysDiff < 7) {
            val sdf = SimpleDateFormat("EEEE 'at' h:mm a", Locale.US).apply { this.timeZone = timeZone }
            sdf.format(Date(timestamp))
        } else if (isSameCalendarYear(timestamp, now, timeZone)) {
            val sdf = SimpleDateFormat("MMM d 'at' h:mm a", Locale.US).apply { this.timeZone = timeZone }
            sdf.format(Date(timestamp))
        } else {
            val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US).apply { this.timeZone = timeZone }
            sdf.format(Date(timestamp))
        }
    }

    /**
     * Formats a date timestamp (e.g. promo expiration or snapshot):
     * - Current calendar year: "Aug 19"
     * - Other calendar year: "Mar 15, 2027"
     */
    fun formatDate(
        timestamp: Long,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val pattern = if (isSameCalendarYear(timestamp, now, timeZone)) "MMM d" else "MMM d, yyyy"
        val sdf = SimpleDateFormat(pattern, Locale.US).apply { this.timeZone = timeZone }
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats a date range for promo validity or usage period with consistent year rules:
     * - Both in current calendar year: "Aug 19 – Sep 1"
     * - Both in same non-current year: "Jan 1 – Jan 15, 2027"
     * - Across different years: "Dec 20, 2026 – Jan 15, 2027"
     * - Non-expiring promo: "Aug 19 • No Expiry" (or "Aug 19, 2025 • No Expiry")
     */
    fun formatDateRange(
        startTimestamp: Long,
        endTimestamp: Long?,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val startFormatted = formatDate(startTimestamp, now, timeZone)
        if (endTimestamp == null || endTimestamp <= startTimestamp) {
            return "$startFormatted • No Expiry"
        }

        val startSameAsNow = isSameCalendarYear(startTimestamp, now, timeZone)
        val endSameAsNow = isSameCalendarYear(endTimestamp, now, timeZone)
        val startSameAsEnd = isSameCalendarYear(startTimestamp, endTimestamp, timeZone)

        return when {
            startSameAsNow && endSameAsNow -> {
                val sdf = SimpleDateFormat("MMM d", Locale.US).apply { this.timeZone = timeZone }
                "${sdf.format(Date(startTimestamp))} – ${sdf.format(Date(endTimestamp))}"
            }
            startSameAsEnd -> {
                val sdfShort = SimpleDateFormat("MMM d", Locale.US).apply { this.timeZone = timeZone }
                val sdfYear = SimpleDateFormat("MMM d, yyyy", Locale.US).apply { this.timeZone = timeZone }
                "${sdfShort.format(Date(startTimestamp))} – ${sdfYear.format(Date(endTimestamp))}"
            }
            else -> {
                val sdfYear = SimpleDateFormat("MMM d, yyyy", Locale.US).apply { this.timeZone = timeZone }
                "${sdfYear.format(Date(startTimestamp))} – ${sdfYear.format(Date(endTimestamp))}"
            }
        }
    }

    /**
     * Formats a day label for chart tooltips / selected day indicators:
     * - Current calendar year: "Tue, Aug 25"
     * - Other calendar year: "Tue, Aug 25, 2025"
     */
    fun formatDayLabel(
        timestamp: Long,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val pattern = if (isSameCalendarYear(timestamp, now, timeZone)) "EEE, MMM d" else "EEE, MMM d, yyyy"
        val sdf = SimpleDateFormat(pattern, Locale.US).apply { this.timeZone = timeZone }
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats full day of week (e.g. "Tuesday").
     */
    fun formatDayOfWeek(
        timestamp: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("EEEE", Locale.US).apply { this.timeZone = timeZone }
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats 2-letter day abbreviation for chart X-axis labels (e.g. "Tu").
     */
    fun formatShortDay(
        timestamp: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("EEE", Locale.US).apply { this.timeZone = timeZone }
        return sdf.format(Date(timestamp)).take(2)
    }
}
