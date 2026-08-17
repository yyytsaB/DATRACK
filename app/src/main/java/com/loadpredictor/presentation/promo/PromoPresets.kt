package com.loadpredictor.presentation.promo

import com.loadpredictor.domain.model.SimSlot

/**
 * Illustrative preset model for rapid promo configuration and testing.
 *
 * NOTE: These presets represent common commercial configurations for Smart Communications
 * in the Philippines as illustrative examples. Actual allowances and validity terms can be
 * adjusted by the user in the promo entry form.
 *
 * Multi-Bucket Note: In accordance with SKILL.md, for split allowance promos (e.g. Magic Data with
 * bonus 5G pools), presets track only the primary/larger open-access general data pool.
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
            title = "Smart Magic Data 399",
            description = "24 GB Open Access • Non-Expiring / No Expiry",
            allowanceBytes = 24L * ONE_GB,
            durationDays = null
        ),
        PromoPreset(
            title = "Smart Magic Data 99",
            description = "2 GB Open Access • Non-Expiring / No Expiry",
            allowanceBytes = 2L * ONE_GB,
            durationDays = null
        ),
        PromoPreset(
            title = "Smart GigaSurf 99",
            description = "2 GB Open Access • 7 Days Validity",
            allowanceBytes = 2L * ONE_GB,
            durationDays = 7
        ),
        PromoPreset(
            title = "Smart Power All 99",
            description = "8 GB Open Access • 7 Days Validity",
            allowanceBytes = 8L * ONE_GB,
            durationDays = 7
        )
    )
}
