package com.loadpredictor.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")

/**
 * DataStore Preferences data source managing anti-re-fire tracking for promo threshold notifications.
 * Stores sets of notified milestones (e.g. "50", "80", "90", "PREMATURE_DEPLETION") keyed by promoId.
 */
data class NotificationLedger(val milestones:Map<Long,Set<String>>,val pendingRestore:Set<Long> = emptySet())

class NotificationPreferencesDataSource(private val context: Context) {

    fun getNotifiedThresholds(promoId: Long): Flow<Set<String>> {
        val key = stringSetPreferencesKey("notified_thresholds_$promoId")
        return context.notificationDataStore.data.map { preferences ->
            preferences[key] ?: emptySet()
        }
    }

    suspend fun recordThresholdNotified(promoId: Long, threshold: String) = com.loadpredictor.domain.sync.DataOperationGate.exclusive {
        val key = stringSetPreferencesKey("notified_thresholds_$promoId")
        context.notificationDataStore.edit { preferences ->
            val current = preferences[key] ?: emptySet()
            preferences[key] = current + threshold
        }
    }

    suspend fun clearThresholdsForPromo(promoId: Long) = com.loadpredictor.domain.sync.DataOperationGate.exclusive {
        val key = stringSetPreferencesKey("notified_thresholds_$promoId")
        context.notificationDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
    private val restoreKey=stringSetPreferencesKey("restore_prime_promos")
    suspend fun snapshotLedger():NotificationLedger {
        val prefs=context.notificationDataStore.data.first()
        val rows=prefs.asMap().mapNotNull {(key,value)->
            val id=key.name.removePrefix("notified_thresholds_").toLongOrNull()
            @Suppress("UNCHECKED_CAST")
            if(id==null || !key.name.startsWith("notified_thresholds_")) null else id to (value as Set<String>)
        }.toMap()
        return NotificationLedger(rows,prefs[restoreKey].orEmpty().mapNotNull {it.toLongOrNull()}.toSet())
    }
    suspend fun replaceLedger(ledger:NotificationLedger) = com.loadpredictor.domain.sync.DataOperationGate.exclusive {
        context.notificationDataStore.edit {p->
            p.clear()
            ledger.milestones.forEach {(id,keys)->if(keys.isNotEmpty())p[stringSetPreferencesKey("notified_thresholds_$id")]=keys}
            if(ledger.pendingRestore.isNotEmpty())p[restoreKey]=ledger.pendingRestore.map {it.toString()}.toSet()
        };Unit
    }
    /** On the first fresh restored reading, consume already-crossed thresholds without replaying old alerts. */
    suspend fun primeRestoredPromo(id:Long,crossed:Set<String>) = com.loadpredictor.domain.sync.DataOperationGate.exclusive {
        context.notificationDataStore.edit {p->
            val pending=p[restoreKey].orEmpty()
            if(id.toString() in pending) {
                val key=stringSetPreferencesKey("notified_thresholds_$id")
                p[key]=p[key].orEmpty()+crossed
                val remaining=pending-id.toString()
                if(remaining.isEmpty())p.remove(restoreKey) else p[restoreKey]=remaining
            }
        };Unit
    }

}
