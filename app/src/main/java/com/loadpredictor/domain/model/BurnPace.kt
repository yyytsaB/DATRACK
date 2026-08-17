package com.loadpredictor.domain.model

/**
 * Pacing status indicating how fast data is burning relative to promo validity.
 */
enum class BurnPace {
    /**
     * Consuming data >= 25% faster than linear pace (Burn Status Index > 1.25).
     * Projected to run out before promo expiration.
     */
    BURNING_FAST,

    /**
     * Consuming data within ±25% of linear pace (0.75 <= Burn Status Index <= 1.25).
     */
    ON_TRACK,

    /**
     * Consuming data >= 25% slower than linear pace (Burn Status Index < 0.75).
     * Projected to have surplus data remaining at promo expiration.
     */
    CONSERVATIVE,

    /**
     * Total allowance is completely consumed (0 bytes remaining).
     */
    DEPLETED,

    /**
     * Promo is in the first hour of usage (T_elapsed < 1 hr) or no usage detected yet (R_burn == 0).
     * Pacing is calibrating to prevent burst noise distortion.
     */
    INSUFFICIENT_DATA
}
