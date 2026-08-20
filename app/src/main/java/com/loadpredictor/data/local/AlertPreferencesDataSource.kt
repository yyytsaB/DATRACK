package com.loadpredictor.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.loadpredictor.domain.model.AlertPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.alertDataStore: DataStore<Preferences> by preferencesDataStore(name = "alert_preferences")

/**
 * DataStore Preferences data source managing user configuration for data burn threshold alerts.
 */
class AlertPreferencesDataSource(private val context: Context) {

    companion object {
        val KEY_ALERT_50 = booleanPreferencesKey("alert_50_enabled")
        val KEY_ALERT_80 = booleanPreferencesKey("alert_80_enabled")
        val KEY_ALERT_90 = booleanPreferencesKey("alert_90_enabled")
        val KEY_ALERT_PREMATURE = booleanPreferencesKey("alert_premature_enabled")
    }

    val alertPreferencesFlow: Flow<AlertPreferences> = context.alertDataStore.data.map { preferences ->
        AlertPreferences(
            is50Enabled = preferences[KEY_ALERT_50] ?: true,
            is80Enabled = preferences[KEY_ALERT_80] ?: true,
            is90Enabled = preferences[KEY_ALERT_90] ?: true,
            isPrematureEnabled = preferences[KEY_ALERT_PREMATURE] ?: true
        )
    }

    suspend fun set50AlertEnabled(enabled: Boolean) {
        context.alertDataStore.edit { preferences ->
            preferences[KEY_ALERT_50] = enabled
        }
    }

    suspend fun set80AlertEnabled(enabled: Boolean) {
        context.alertDataStore.edit { preferences ->
            preferences[KEY_ALERT_80] = enabled
        }
    }

    suspend fun set90AlertEnabled(enabled: Boolean) {
        context.alertDataStore.edit { preferences ->
            preferences[KEY_ALERT_90] = enabled
        }
    }

    suspend fun setPrematureAlertEnabled(enabled: Boolean) {
        context.alertDataStore.edit { preferences ->
            preferences[KEY_ALERT_PREMATURE] = enabled
        }
    }
}
