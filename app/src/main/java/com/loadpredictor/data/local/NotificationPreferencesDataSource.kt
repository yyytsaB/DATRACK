package com.loadpredictor.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")

/**
 * DataStore Preferences data source managing anti-re-fire tracking for promo threshold notifications.
 * Stores sets of notified milestones (e.g. "50", "80", "90", "PREMATURE_DEPLETION") keyed by promoId.
 */
class NotificationPreferencesDataSource(private val context: Context) {

    fun getNotifiedThresholds(promoId: Long): Flow<Set<String>> {
        val key = stringSetPreferencesKey("notified_thresholds_$promoId")
        return context.notificationDataStore.data.map { preferences ->
            preferences[key] ?: emptySet()
        }
    }

    suspend fun recordThresholdNotified(promoId: Long, threshold: String) {
        val key = stringSetPreferencesKey("notified_thresholds_$promoId")
        context.notificationDataStore.edit { preferences ->
            val current = preferences[key] ?: emptySet()
            preferences[key] = current + threshold
        }
    }

    suspend fun clearThresholdsForPromo(promoId: Long) {
        val key = stringSetPreferencesKey("notified_thresholds_$promoId")
        context.notificationDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}
