package com.loadpredictor.domain.model

/**
 * Pure Kotlin domain model representing a tracked Philippine prepaid mobile data promo.
 *
 * All data allowances are stored internally in raw [totalAllowanceBytes] for exact precision,
 * per the engineering rules in SKILL.md.
 *
 * Non-expiring promos (such as Smart Magic Data) have a `null` [expirationTimestamp].
 *
 * Structural invariants (name, positive allowance, valid date order) are validated in [init].
 * Temporal validation (e.g., ensuring start timestamp is not in the future) is enforced
 * in the use-case layer via injected TimeProvider, keeping this model pure and deterministic.
 *
 * @property id Unique identifier (auto-generated in persistence).
 * @property name Commercial name of the promo (e.g., "Smart GigaSurf 99", "Smart Magic Data 399").
 * @property totalAllowanceBytes Total data allowance in bytes (must be > 0).
 * @property startTimestamp Epoch timestamp in milliseconds when promo began.
 * @property expirationTimestamp Epoch timestamp in milliseconds when promo expires, or null for no-expiry promos.
 * @property simSlot The SIM slot associated with this promo (SIM_1 or SIM_2).
 * @property isActive Whether this promo is currently the active forecasting context.
 */
data class Promo(
    val id: Long = 0L,
    val name: String,
    val totalAllowanceBytes: Long,
    val startTimestamp: Long,
    val expirationTimestamp: Long? = null,
    val simSlot: SimSlot = SimSlot.SIM_1,
    val isActive: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "Promo name must not be blank" }
        require(totalAllowanceBytes > 0) { "Total allowance must be greater than 0 bytes" }
        if (expirationTimestamp != null) {
            require(expirationTimestamp > startTimestamp) {
                "Expiration timestamp must be after start timestamp"
            }
        }
    }

    /**
     * Total validity window of the promo in milliseconds, or null for non-expiring promos.
     */
    val totalDurationMillis: Long?
        get() = expirationTimestamp?.let { it - startTimestamp }

    /**
     * Indicates whether this is a non-expiring / data-cap-only promo (e.g., Smart Magic Data).
     */
    val isNoExpiry: Boolean
        get() = expirationTimestamp == null

    /**
     * Checks if the promo is expired relative to [currentTimeMillis].
     * Non-expiring promos always return false.
     */
    fun isExpired(currentTimeMillis: Long): Boolean =
        expirationTimestamp?.let { currentTimeMillis >= it } ?: false
}
