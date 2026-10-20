package com.loadpredictor.data.stats

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import com.loadpredictor.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MobileAppUsageSource(private val context:Context) {
    suspend fun query(start:Long,end:Long):List<MobileUidUsage> = withContext(Dispatchers.IO) {
        require(start>=0 && start<end)
        if(!UsageAccessHelper(context).hasUsageAccessPermission())throw UsageAccessDeniedException("Grant Usage Access to see mobile app usage.")
        val manager=context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val rows=mutableListOf<Pair<Int,Long>>()
        try {
            @Suppress("DEPRECATION")
            val stats=requireNotNull(manager.querySummary(ConnectivityManager.TYPE_MOBILE,null,start,end)){"Mobile attribution is unavailable"}
            stats.use {
                val bucket=NetworkStats.Bucket()
                while(it.hasNextBucket()) {
                    it.getNextBucket(bucket)
                    val rx=bucket.rxBytes.coerceAtLeast(0);val tx=bucket.txBytes.coerceAtLeast(0)
                    rows+=bucket.uid to if(Long.MAX_VALUE-rx<tx)Long.MAX_VALUE else rx+tx
                }
            }
        }catch(e:SecurityException){throw UsageAccessDeniedException("Usage Access is missing or was revoked.",e)}
        MobileAttribution.aggregate(rows).map {(uid,bytes)->
            val packages=try {context.packageManager.getPackagesForUid(uid).orEmpty()}catch(_:SecurityException){emptyArray<String>()}
            val labels=packages.map {name->try {
                @Suppress("DEPRECATION") val info=context.packageManager.getApplicationInfo(name,0)
                context.packageManager.getApplicationLabel(info).toString()
            }catch(_:android.content.pm.PackageManager.NameNotFoundException){name}catch(_:SecurityException){name}}.distinct().sorted()
            val label=when {
                packages.size>1 -> "Shared UID: "+labels.joinToString(", ")
                labels.size==1 -> labels.first()
                uid==-4 -> "Removed apps"
                uid==-5 -> "Tethering"
                else -> "System or unavailable app (UID $uid)"
            }
            MobileUidUsage(uid,label,bytes)
        }.filter {it.bytes>0}.sortedByDescending {it.bytes}
    }
}
