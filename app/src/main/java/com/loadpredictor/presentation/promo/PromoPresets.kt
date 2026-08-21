package com.loadpredictor.presentation.promo

import com.loadpredictor.domain.model.SimSlot

/**
 * Illustrative preset model for rapid promo configuration and testing.
 */
data class PromoPreset(
    val title: String,
    val description: String,
    val allowanceBytes: Long,
    val durationDays: Int?, // null for non-expiring promos (Magic Data)
    val defaultSimSlot: SimSlot = SimSlot.SIM_1
)

object PromoPresets {
    private const val ONE_GB = 1024L * 1024L * 1024L

    val ALL_PRESETS = listOf(
        PromoPreset(
            title = "GigaSurf 99",
            description = "2 GB • 7 Days",
            allowanceBytes = 2L * ONE_GB,
            durationDays = 7
        ),
        PromoPreset(
            title = "GigaSurf 149",
            description = "4 GB • 7 Days",
            allowanceBytes = 4L * ONE_GB,
            durationDays = 7
        ),
        PromoPreset(
            title = "Giga Video 50",
            description = "1 GB • 3 Days",
            allowanceBytes = 1L * ONE_GB,
            durationDays = 3
        ),
        PromoPreset(
            title = "Magic Data",
            description = "24 GB • No Expiry",
            allowanceBytes = 24L * ONE_GB,
            durationDays = null
        ),
        PromoPreset(
            title = "GoSURF 50",
            description = "2 GB • 3 Days",
            allowanceBytes = 2L * ONE_GB,
            durationDays = 3
        )
    )
}

