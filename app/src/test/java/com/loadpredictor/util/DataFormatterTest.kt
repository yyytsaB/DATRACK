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
}
