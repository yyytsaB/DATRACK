package com.loadpredictor.domain.sync

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes evaluation, edits and restore. Nested sequential calls share the same operation.
 * Do not launch concurrent child operations while owning this gate. */
object DataOperationGate {
    private val mutex=Mutex()
    private object Key:CoroutineContext.Key<Owner>
    private class Owner:AbstractCoroutineContextElement(Key)
    suspend fun <T> exclusive(block:suspend ()->T):T {
        if(currentCoroutineContext()[Key]!=null) return block()
        return mutex.withLock { withContext(Owner()) { block() } }
    }
}
