package com.loadpredictor.presentation.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.SimSlot

object WidgetStatePreferences {
    val KEY_STATE_TYPE = stringPreferencesKey("widget_state_type")
    val KEY_PROMO_NAME = stringPreferencesKey("widget_promo_name")
    val KEY_SIM_SLOT = stringPreferencesKey("widget_sim_slot")
    val KEY_REMAINING_BYTES = longPreferencesKey("widget_remaining_bytes")
    val KEY_TOTAL_ALLOWANCE_BYTES = longPreferencesKey("widget_total_allowance_bytes")
    val KEY_PACE = stringPreferencesKey("widget_pace")
    val KEY_SUMMARY = stringPreferencesKey("widget_summary")
    val KEY_IS_NO_EXPIRY = booleanPreferencesKey("widget_is_no_expiry")
    val KEY_LAST_UPDATED = longPreferencesKey("widget_last_updated")
    val KEY_ERROR_MESSAGE = stringPreferencesKey("widget_error_message")

    const val TYPE_SUCCESS = "SUCCESS"
    const val TYPE_NO_ACTIVE_PROMO = "NO_ACTIVE_PROMO"
    const val TYPE_PERMISSION_REQUIRED = "PERMISSION_REQUIRED"
    const val TYPE_ERROR = "ERROR"

    fun readState(preferences: Preferences): WidgetState {
        return when (preferences[KEY_STATE_TYPE]) {
            TYPE_SUCCESS -> {
                WidgetState.Success(
                    promoName = preferences[KEY_PROMO_NAME] ?: "Active Promo",
                    simSlot = if (preferences[KEY_SIM_SLOT] == "SIM_2") SimSlot.SIM_2 else SimSlot.SIM_1,
                    remainingBytes = preferences[KEY_REMAINING_BYTES] ?: 0L,
                    totalAllowanceBytes = preferences[KEY_TOTAL_ALLOWANCE_BYTES] ?: 0L,
                    pace = try {
                        BurnPace.valueOf(preferences[KEY_PACE] ?: BurnPace.INSUFFICIENT_DATA.name)
                    } catch (e: Exception) {
                        BurnPace.INSUFFICIENT_DATA
                    },
                    plainLanguageSummary = preferences[KEY_SUMMARY] ?: "",
                    isNoExpiry = preferences[KEY_IS_NO_EXPIRY] ?: true,
                    lastUpdatedMillis = preferences[KEY_LAST_UPDATED] ?: System.currentTimeMillis()
                )
            }
            TYPE_NO_ACTIVE_PROMO -> WidgetState.NoActivePromo
            TYPE_PERMISSION_REQUIRED -> WidgetState.PermissionRequired
            TYPE_ERROR -> WidgetState.Error(preferences[KEY_ERROR_MESSAGE] ?: "Forecast unavailable")
            else -> WidgetState.NoActivePromo
        }
    }

    suspend fun saveStateAndNotify(context: Context, state: WidgetState) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(LoadPredictorWidget::class.java)

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { preferences ->
                when (state) {
                    is WidgetState.Success -> {
                        preferences[KEY_STATE_TYPE] = TYPE_SUCCESS
                        preferences[KEY_PROMO_NAME] = state.promoName
                        preferences[KEY_SIM_SLOT] = state.simSlot.name
                        preferences[KEY_REMAINING_BYTES] = state.remainingBytes
                        preferences[KEY_TOTAL_ALLOWANCE_BYTES] = state.totalAllowanceBytes
                        preferences[KEY_PACE] = state.pace.name
                        preferences[KEY_SUMMARY] = state.plainLanguageSummary
                        preferences[KEY_IS_NO_EXPIRY] = state.isNoExpiry
                        preferences[KEY_LAST_UPDATED] = state.lastUpdatedMillis
                    }
                    is WidgetState.NoActivePromo -> {
                        preferences[KEY_STATE_TYPE] = TYPE_NO_ACTIVE_PROMO
                    }
                    is WidgetState.PermissionRequired -> {
                        preferences[KEY_STATE_TYPE] = TYPE_PERMISSION_REQUIRED
                    }
                    is WidgetState.Error -> {
                        preferences[KEY_STATE_TYPE] = TYPE_ERROR
                        preferences[KEY_ERROR_MESSAGE] = state.message
                    }
                }
            }
        }
        LoadPredictorWidget().updateAll(context)
    }
}
