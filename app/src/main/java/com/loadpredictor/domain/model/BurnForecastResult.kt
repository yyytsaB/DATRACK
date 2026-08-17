package com.loadpredictor.domain.model

/**
 * Sealed interface representing the state of an active burn forecast.
 */
sealed interface BurnForecastResult {
    /**
     * Successfully calculated forecast for the active promo.
     */
    data class Success(val forecast: BurnForecast) : BurnForecastResult

    /**
     * No promo is currently active or configured.
     */
    data object NoActivePromo : BurnForecastResult

    /**
     * Usage Access permission (PACKAGE_USAGE_STATS) has not been granted by the user.
     */
    data object PermissionRequired : BurnForecastResult

    /**
     * An unexpected repository or query failure occurred.
     */
    data class Error(val message: String) : BurnForecastResult
}
