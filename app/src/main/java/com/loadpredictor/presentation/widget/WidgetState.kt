package com.loadpredictor.presentation.widget

import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.SimSlot

/**
 * Sealed hierarchy representing the visual state of the Glance home screen widget.
 */
sealed interface WidgetState {

    /**
     * Successfully calculated burn forecast state.
     */
    data class Success(
        val promoName: String,
        val simSlot: SimSlot,
        val remainingBytes: Long,
        val totalAllowanceBytes: Long,
        val pace: BurnPace,
        val plainLanguageSummary: String,
        val isNoExpiry: Boolean,
        val lastUpdatedMillis: Long,
        val estimatedDepletionTimestamp: Long? = null
    ) : WidgetState

    /**
     * Displayed when no active promo is registered in the database.
     */
    data object NoActivePromo : WidgetState

    /**
     * Displayed when PACKAGE_USAGE_STATS permission is missing.
     */
    data object PermissionRequired : WidgetState

    /**
     * Displayed when an unexpected forecasting or background error occurs.
     */
    data class Error(val message: String) : WidgetState
}
