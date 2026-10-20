package com.loadpredictor.domain.diagnostics

import kotlinx.serialization.Serializable
import kotlinx.coroutines.CancellationException

@Serializable data class ObservationEvent(val at:Long,val source:String,val result:String,
    val windowStart:Long?=null,val windowEnd:Long?=null,val measuredBytes:Long?=null,
    val attempt:Int?=null,val requestedAt:Long?=null)

object ObservationLog {
    var recorder:suspend (ObservationEvent)->Unit = {}
    suspend fun record(event:ObservationEvent) {
        try {recorder(event)} catch(e:CancellationException){throw e} catch(_:Exception) { /* Logging must not break measurement. */ }
    }
}
