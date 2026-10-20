package com.loadpredictor.data.backup

import android.content.Context
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.stats.*
import com.loadpredictor.domain.model.*
import com.loadpredictor.domain.sync.DataOperationGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*

data class DiarySnapshot(val promo:Promo,val window:DiaryWindow,val buckets:List<UsageBucket>,val observedAt:Long) {
    fun csv()=DiaryCsv.encode(promo,window,buckets,observedAt)
}
class DiaryService(private val context:Context) {
    suspend fun load(id:Long,from:LocalDate,through:LocalDate,now:Long,zone:ZoneId):DiarySnapshot=withContext(Dispatchers.IO) {DataOperationGate.exclusive {
        if(!UsageAccessHelper(context).hasUsageAccessPermission())throw UsageAccessDeniedException("Grant Usage Access to read the Data Diary.")
        val promo=requireNotNull(AppDatabase.getInstance(context).promoDao().findById(id)){"Promo no longer exists"}.toDomain()
        val window=DiaryWindow.create(from,through,promo,now,zone)
        val buckets=NetworkStatsDataSource(context).queryDailyUsageBreakdown(window.start,window.end,zone)
        DiarySnapshot(promo,window,buckets,now)
    }}
}
