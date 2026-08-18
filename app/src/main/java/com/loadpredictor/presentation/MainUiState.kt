package com.loadpredictor.presentation

import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.UsageBucket

/**
 * Immutable UI State for MainActivity and dashboard container.
 */
data class MainUiState(
    val isUsagePermissionGranted: Boolean = false,
    val activePromo: Promo? = null,
    val forecastResult: BurnForecastResult = BurnForecastResult.NoActivePromo,
    val dailyUsageBreakdown: List<UsageBucket> = emptyList(),
    val isLoading: Boolean = true
)

