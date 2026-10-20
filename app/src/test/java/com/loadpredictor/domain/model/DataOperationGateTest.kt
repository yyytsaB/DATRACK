package com.loadpredictor.domain.model
import com.loadpredictor.domain.sync.DataOperationGate
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class DataOperationGateTest {
    @Test fun sequentialNestedCallsAndConcurrentEditsDoNotInterleave()=runTest {
        val order=mutableListOf<String>()
        val first=launch {DataOperationGate.exclusive {
            order+="measurement";delay(100)
            DataOperationGate.exclusive {order+="checkpoint"}
            order+="published"
        }}
        val second=launch {delay(1);DataOperationGate.exclusive {order+="edit"}}
        joinAll(first,second)
        assertEquals(listOf("measurement","checkpoint","published","edit"),order)
    }
    @Test fun cancellationReleasesTheGate()=runTest {
        val entered=CompletableDeferred<Unit>()
        val job=launch {DataOperationGate.exclusive {entered.complete(Unit);awaitCancellation()}}
        entered.await();job.cancelAndJoin()
        assertEquals("released",DataOperationGate.exclusive {"released"})
    }
}
