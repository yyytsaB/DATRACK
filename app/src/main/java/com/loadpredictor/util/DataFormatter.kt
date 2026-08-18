package com.loadpredictor.util

import java.util.Locale

/**
 * Encapsulates paired formatted data amount strings to ensure uniform precision in paired UI layouts.
 */
data class FormattedDataPair(
    val remainingFormatted: String,
    val totalFormatted: String
)

/**
 * Shared utility for formatting data byte quantities with adaptive, synchronized precision.
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
}
