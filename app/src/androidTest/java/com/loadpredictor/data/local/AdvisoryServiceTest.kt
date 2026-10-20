package com.loadpredictor.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loadpredictor.data.backup.*
import com.loadpredictor.domain.model.*
import com.loadpredictor.testing.PreserveVerificationState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.*

class AdvisoryServiceTest {
    @get:Rule val preserve=PreserveVerificationState()
    @Test fun noRetroactiveAlertThenFutureDailyCrossingNotifiesOnceAndBlockedDeliveryRetries()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val prefs=AdvisoryPreferences(context)
        prefs.replace(AdvisoryState());var bytes=150L;var sent=0;var allowed=false
        val service=AdvisoryService(context,query={_,_->bytes},post={if(allowed){sent++;true}else false})
        val now=System.currentTimeMillis();service.save(AdvisorySettings(100,true),now)
        service.evaluate(null,now);assertEquals(0,sent)
        val tomorrow=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1).atTime(12,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        service.evaluate(null,tomorrow);assertEquals(0,sent)
        allowed=true;service.evaluate(null,tomorrow);service.evaluate(null,tomorrow+1000);assertEquals(1,sent)
        assertEquals(2,prefs.snapshot().consumed.size)
    }
    @Test fun settingsRoundTripAndRestorePrimesCurrentEvents()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val prefs=AdvisoryPreferences(context)
        var sent=0;val service=AdvisoryService(context,query={_,_->200},post={sent++;true})
        val settings=AdvisorySettings(100,true,24)
        service.restored(settings);service.evaluate(null,System.currentTimeMillis());assertEquals(0,sent)
        val backup=BackupService(context).export();assertEquals(settings,backup.advisory)
        prefs.replace(AdvisoryState())
        val folder=java.io.File(context.cacheDir,"advisory-restore-test").apply {mkdirs()}
        try {val restore=BackupService(context,directory=folder);restore.restore(backup,restore.export());assertEquals(settings,prefs.snapshot().settings)}
        finally{folder.deleteRecursively()}
    }
}
