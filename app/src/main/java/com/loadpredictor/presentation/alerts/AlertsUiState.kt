package com.loadpredictor.presentation.alerts

/**
 * UI State for the Alerts & Notifications screen.
 *
 * @property is50Enabled Whether 50% data milestone alert is toggled on.
 * @property is80Enabled Whether 80% data milestone alert is toggled on.
 * @property is90Enabled Whether 90% data milestone alert is toggled on.
 * @property isPrematureEnabled Whether premature depletion alert is toggled on.
 * @property hasNotificationPermission Whether POST_NOTIFICATIONS permission is currently granted.
 */
data class AlertsUiState(
    val is50Enabled: Boolean = true,
    val is80Enabled: Boolean = true,
    val is90Enabled: Boolean = true,
    val isPrematureEnabled: Boolean = true,
    val hasNotificationPermission: Boolean = true
)
