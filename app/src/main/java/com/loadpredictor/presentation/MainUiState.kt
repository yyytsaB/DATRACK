package com.loadpredictor.presentation

import com.loadpredictor.domain.model.Promo

/**
 * Immutable UI State for MainActivity and dashboard container.
 */
data class MainUiState(
    val isUsagePermissionGranted: Boolean = false,
    val activePromo: Promo? = null,
    val isLoading: Boolean = true
)
