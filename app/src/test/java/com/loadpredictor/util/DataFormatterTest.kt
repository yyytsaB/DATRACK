package com.loadpredictor.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DataFormatterTest {

    private val oneGb = 1024L * 1024L * 1024L
    private val oneMb = 1024L * 1024L

    @Test
    fun `normal usage displays clean 1-decimal value`() {
        val totalAllowance = 24L * oneGb
        val remaining = 18L * oneGb // 18.0 GB remaining
        val formatted = DataFormatter.formatDataAmount(remaining, totalAllowance)
        assertEquals("18.0 GB", formatted)

        val pair = DataFormatter.formatDataPair(remaining, totalAllowance)
        assertEquals("18.0 GB", pair.remainingFormatted)
        assertEquals("24.0 GB", pair.totalFormatted)
    }

    @Test
    fun `near-total edge case synchronizes both values to 2 decimals`() {
        val totalAllowance = 24L * oneGb
        val used = 7L * oneMb // 7 MB used of 24 GB
        val remaining = totalAllowance - used
        val pair = DataFormatter.formatDataPair(remaining, totalAllowance)

        // Both values must use matching 2-decimal precision (never mixed "23.99 GB of 24.0 GB")
        assertEquals("23.99 GB", pair.remainingFormatted)
        assertEquals("24.00 GB", pair.totalFormatted)
    }

    @Test
    fun `near-total edge case with small 2MB usage synchronizes both values to 2 decimals`() {
        val totalAllowance = 24L * oneGb
        val used = 2L * oneMb // 2 MB used of 24 GB
        val remaining = totalAllowance - used
        val pair = DataFormatter.formatDataPair(remaining, totalAllowance)

        assertEquals("24.00 GB", pair.remainingFormatted)
        assertEquals("24.00 GB", pair.totalFormatted)
    }

    @Test
    fun `exactly zero usage correctly displays 1 decimal for both values`() {
        val totalAllowance = 24L * oneGb
        val remaining = totalAllowance // Exactly 0 bytes used
        val pair = DataFormatter.formatDataPair(remaining, totalAllowance)

        assertEquals("24.0 GB", pair.remainingFormatted)
        assertEquals("24.0 GB", pair.totalFormatted)
    }

    @Test
    fun `depleted and near-zero remaining cases format in MB or 0 MB`() {
        val totalAllowance = 24L * oneGb

        // Exactly depleted
        val depletedPair = DataFormatter.formatDataPair(0L, totalAllowance)
        assertEquals("0 MB", depletedPair.remainingFormatted)
        assertEquals("24.0 GB", depletedPair.totalFormatted)

        // 500 MB remaining
        val subGbPair = DataFormatter.formatDataPair(500L * oneMb, totalAllowance)
        assertEquals("500 MB", subGbPair.remainingFormatted)
        assertEquals("24.0 GB", subGbPair.totalFormatted)
    }

    @Test
    fun `sub-GB total allowance formats both values in MB`() {
        val totalAllowance = 800L * oneMb
        val remaining = 400L * oneMb
        val pair = DataFormatter.formatDataPair(remaining, totalAllowance)

        assertEquals("400 MB", pair.remainingFormatted)
        assertEquals("800 MB", pair.totalFormatted)
    }

    @Test
    fun `formatBytes standalone formats GB and MB correctly`() {
        assertEquals("24.0 GB", DataFormatter.formatBytes(24L * oneGb))
        assertEquals("1.5 GB", DataFormatter.formatBytes((1.5 * oneGb).toLong()))
        assertEquals("100 MB", DataFormatter.formatBytes(100L * oneMb))
        assertEquals("0 MB", DataFormatter.formatBytes(0L))
    }

    @Test
    fun `depletion date within current calendar year omits year`() {
        val timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
        // Reference: Aug 25, 2026 15:00:00 UTC+8 (epoch ms: 1787641200000)
        val calNow = java.util.Calendar.getInstance(timeZone).apply {
            set(2026, java.util.Calendar.AUGUST, 25, 15, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = calNow.timeInMillis

        // Future date in same year: Oct 1, 2026 15:00:00 UTC+8 (~37 days later)
        val calSameYear = java.util.Calendar.getInstance(timeZone).apply {
            set(2026, java.util.Calendar.OCTOBER, 1, 15, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val oct1 = calSameYear.timeInMillis

        val formatted = DataFormatter.formatDepletionDateTime(oct1, now, timeZone)
        assertEquals("Oct 1 at 3:00 PM", formatted)
    }

    @Test
    fun `depletion date across calendar year includes year`() {
        val timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
        // Reference: Aug 25, 2026 15:00:00 UTC+8
        val calNow = java.util.Calendar.getInstance(timeZone).apply {
            set(2026, java.util.Calendar.AUGUST, 25, 15, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = calNow.timeInMillis

        // Future date in next year: Mar 3, 2027 15:16:00 UTC+8 (~190 days / 6+ months later)
        val calNextYear = java.util.Calendar.getInstance(timeZone).apply {
            set(2027, java.util.Calendar.MARCH, 3, 15, 16, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val mar3NextYear = calNextYear.timeInMillis

        val formatted = DataFormatter.formatDepletionDateTime(mar3NextYear, now, timeZone)
        assertEquals("Mar 3, 2027 at 3:16 PM", formatted)
    }

    @Test
    fun `depletion date within 7 days displays day of week and time`() {
        val timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
        // Reference: Tuesday Aug 25, 2026 10:00:00
        val calNow = java.util.Calendar.getInstance(timeZone).apply {
            set(2026, java.util.Calendar.AUGUST, 25, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = calNow.timeInMillis

        // 2 days later: Thursday Aug 27, 2026 14:30:00
        val calNear = java.util.Calendar.getInstance(timeZone).apply {
            set(2026, java.util.Calendar.AUGUST, 27, 14, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val nearTime = calNear.timeInMillis

        val formatted = DataFormatter.formatDepletionDateTime(nearTime, now, timeZone)
        assertEquals("Thursday at 2:30 PM", formatted)
    }

    @Test
    fun `formatDate and formatDateRange handle current year and multi-year spans accurately`() {
        val timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
        val cal2026Start = java.util.Calendar.getInstance(timeZone).apply {
            set(2026, java.util.Calendar.AUGUST, 19, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val cal2026End = java.util.Calendar.getInstance(timeZone).apply {
            set(2026, java.util.Calendar.SEPTEMBER, 1, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val cal2027End = java.util.Calendar.getInstance(timeZone).apply {
            set(2027, java.util.Calendar.JANUARY, 15, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = cal2026Start.timeInMillis

        // Single date in same year
        assertEquals("Aug 19", DataFormatter.formatDate(cal2026Start.timeInMillis, now, timeZone))
        // Single date in next year
        assertEquals("Jan 15, 2027", DataFormatter.formatDate(cal2027End.timeInMillis, now, timeZone))

        // Same year range
        assertEquals("Aug 19 – Sep 1", DataFormatter.formatDateRange(cal2026Start.timeInMillis, cal2026End.timeInMillis, now, timeZone))

        // Across year range
        assertEquals("Aug 19, 2026 – Jan 15, 2027", DataFormatter.formatDateRange(cal2026Start.timeInMillis, cal2027End.timeInMillis, now, timeZone))

        // No expiry range
        assertEquals("Aug 19 • No Expiry", DataFormatter.formatDateRange(cal2026Start.timeInMillis, null, now, timeZone))
    }
}
