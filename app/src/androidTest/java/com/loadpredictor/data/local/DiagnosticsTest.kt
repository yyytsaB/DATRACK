package com.loadpredictor.data.local
import androidx.test.core.app.ApplicationProvider
import com.loadpredictor.data.diagnostics.LocalDiagnostics
import com.loadpredictor.domain.diagnostics.ObservationEvent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import org.junit.Assert.*
import org.junit.Test
class DiagnosticsTest {
    @Test fun retentionCountLimitAndPrivateSupportExport()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir=File(context.cacheDir,"diagnostic-fixture").apply {mkdirs()}
        try {
            val now=System.currentTimeMillis()
            val events=listOf(ObservationEvent(now-8*86400000L,"worker:periodic","completed"))+
                (0..1005).map {ObservationEvent(now-it,"forecast","success",1,now,123)}
            File(dir,"observations.json").writeText(Json.encodeToString(events))
            val log=LocalDiagnostics(context,dir);log.append(ObservationEvent(now,"worker:widget_refresh","retry",attempt=1))
            val saved=log.events(now)
            assertEquals(LocalDiagnostics.MAX_EVENTS,saved.size)
            assertTrue(saved.all {it.at>=now-LocalDiagnostics.RETENTION})
            assertEquals("retry",saved.last().result)
            assertTrue(log.supportReport().contains("Review before sharing",ignoreCase=true))
        } finally {dir.deleteRecursively()}
    }
}
