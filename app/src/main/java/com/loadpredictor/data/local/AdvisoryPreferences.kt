package com.loadpredictor.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.*
import com.loadpredictor.domain.model.*
import com.loadpredictor.domain.sync.DataOperationGate
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

private val Context.advisoryStore by preferencesDataStore("advisory_settings")
@Serializable data class AdvisoryState(val settings:AdvisorySettings=AdvisorySettings(),val consumed:Set<String> = emptySet())
class AdvisoryPreferences(private val context:Context) {
    private val key=stringPreferencesKey("state")
    val flow=context.advisoryStore.data.map {p->p[key]?.let {Json.decodeFromString<AdvisoryState>(it)} ?: AdvisoryState()}
    suspend fun snapshot()=flow.first()
    suspend fun replace(state:AdvisoryState)=DataOperationGate.exclusive {
        state.settings.validate();context.advisoryStore.edit {it[key]=Json.encodeToString(state)};Unit
    }
}
