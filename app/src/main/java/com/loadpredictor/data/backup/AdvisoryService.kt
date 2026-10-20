package com.loadpredictor.data.backup

import android.content.Context
import com.loadpredictor.data.local.*
import com.loadpredictor.data.stats.*
import com.loadpredictor.data.notification.NotificationHelper
import com.loadpredictor.domain.model.*
import com.loadpredictor.domain.sync.DataOperationGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.time.ZoneId

class AdvisoryService(private val context:Context,
    private val query:((Long,ZoneId)->Long)?=null,
    private val post:((AdvisoryEvent)->Boolean)?=null) {
    private val prefs=AdvisoryPreferences(context)
    suspend fun today(now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()):Long=withContext(Dispatchers.IO) {
        query?.let {return@withContext it(now,zone)}
        if(!UsageAccessHelper(context).hasUsageAccessPermission())throw UsageAccessDeniedException("Grant Usage Access to read today's mobile usage.")
        NetworkStatsDataSource(context).queryMobileUsageBytes(AdvisoryPolicy.dayStart(now,zone),now)
    }
    suspend fun save(settings:AdvisorySettings,now:Long)=DataOperationGate.exclusive {
        settings.validate();val before=prefs.snapshot()
        if(before.settings==settings)return@exclusive
        val bytes=if(settings.dailyNotify)today(now) else 0
        val promo=AppDatabase.getInstance(context).promoDao().getActivePromo().first()?.toDomain()
        // Consume only events already eligible at setup, so enabling never replays old alerts.
        val prime=AdvisoryPolicy.eligible(settings,promo,bytes,now,ZoneId.systemDefault()).map {it.key}
        prefs.replace(AdvisoryState(settings,(before.consumed+prime).toList().takeLast(200).toSet()))
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(2101);manager.cancel(2102)
    }
    suspend fun evaluate(promo:Promo?,now:Long)=DataOperationGate.exclusive {
        val state=prefs.snapshot();val settings=state.settings
        if(!settings.dailyNotify && settings.expiryHours==null)return@exclusive
        val bytes=if(settings.dailyNotify)today(now) else 0
        val helper=NotificationHelper(context)
        val events=AdvisoryPolicy.eligible(settings,promo,bytes,now,ZoneId.systemDefault())
        if("restore_pending" in state.consumed) {
            prefs.replace(state.copy(consumed=events.map {it.key}.toSet()));return@exclusive
        }
        val delivered=events.filter {it.key !in state.consumed && (post?.invoke(it) ?: helper.showAdvisory(it))}.map {it.key}
        if(delivered.isNotEmpty())prefs.replace(state.copy(consumed=(state.consumed+delivered).toList().takeLast(200).toSet()))
    }
    suspend fun restored(settings:AdvisorySettings) {
        // Imported configurations deliberately start quiet; current events prime on first available reading.
        prefs.replace(AdvisoryState(settings,setOf("restore_pending")))
    }
}
