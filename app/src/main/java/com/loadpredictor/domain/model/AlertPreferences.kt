package com.loadpredictor.domain.model

/**
 * User preferences for configurable data burn threshold notifications.
 *
 * @property is50Enabled Whether to notify when data consumption reaches 50%.
 * @property is80Enabled Whether to notify when data consumption reaches 80%.
 * @property is90Enabled Whether to notify when data consumption reaches 90%.
 * @property isPrematureEnabled Whether to notify when an expiring promo projects premature exhaustion.
 */
data class AlertPreferences(
    val is50Enabled: Boolean = true,
    val is80Enabled: Boolean = true,
    val is90Enabled: Boolean = true,
    val isPrematureEnabled: Boolean = true
)
