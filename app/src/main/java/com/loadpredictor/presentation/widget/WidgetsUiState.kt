package com.loadpredictor.presentation.widget

import com.loadpredictor.domain.model.BurnForecast

/**
 * UI State for the Widgets Gallery & Pinning screen.
 *
 * @property activeForecast Current active promo burn forecast (null if no active promo configured).
 * @property isPinSupported Whether the user's current launcher supports programmatic widget pinning.
 * @property isLoading Whether the forecast data is currently calculating.
 */
data class WidgetsUiState(
    val activeForecast: BurnForecast? = null,
    val isPinSupported: Boolean = true,
    val isLoading: Boolean = false
)
