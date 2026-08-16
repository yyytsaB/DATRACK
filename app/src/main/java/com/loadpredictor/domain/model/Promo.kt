package com.loadpredictor.domain.model

/**
 * Pure Kotlin domain model representing a tracked Philippine prepaid mobile data promo.
 *
 * All data allowances are stored internally in raw [totalAllowanceBytes] for exact precision,
 * per the engineering rules in SKILL.md.
 *
 * @property id Unique identifier (auto-generated in persistence).
 * @property name Commercial name of the promo (e.g., "Smart GigaSurf 99", "Magic Data 399").
 * @property totalAllowanceBytes Total data allowance in bytes (must be > 0).
 * @property startTimestamp Epoch timestamp in milliseconds when promo began.
 * @property expirationTimestamp Epoch timestamp in milliseconds when promo expires.
 * @property simSlot The SIM slot associated with this promo (SIM_1 or SIM_2).
 * @property isActive Whether this promo is currently the active forecasting context.
 */
data class Promo(
    val id: Long = 0L,
    val name: String,
    val totalAllowanceBytes: Long,
    val startTimestamp: Long,
    val expirationTimestamp: Long,
    val simSlot: SimSlot = SimSlot.SIM_1,
    val isActive: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "Promo name must not be blank" }
        require(totalAllowanceBytes > 0) { "Total allowance must be greater than 0 bytes" }
        require(expirationTimestamp > startTimestamp) { "Expiration timestamp must be after start timestamp" }
    }

    /**
     * Total validity window of the promo in milliseconds.
     */
    val totalDurationMillis: Long
        get() = expirationTimestamp - startTimestamp

    /**
     * Checks if the promo is expired relative to [currentTimeMillis].
     */
    fun isExpired(currentTimeMillis: Long): Boolean = currentTimeMillis >= expirationTimestamp
}
