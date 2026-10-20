package com.loadpredictor.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

@Serializable data class AdvisorySettings(val dailyCap:Long?=null,val dailyNotify:Boolean=false,val expiryHours:Int?=null) {
    fun validate(){require(dailyCap==null || dailyCap>0);require(expiryHours==null || expiryHours in listOf(6,24,48));require(!dailyNotify || dailyCap!=null)}
}
data class AdvisoryEvent(val key:String,val title:String,val message:String,val notificationId:Int)
object AdvisoryPolicy {
    fun dayStart(now:Long,zone:ZoneId)=Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
    fun eligible(settings:AdvisorySettings,promo:Promo?,todayBytes:Long,now:Long,zone:ZoneId):List<AdvisoryEvent> {
        settings.validate();val events=mutableListOf<AdvisoryEvent>()
        val date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        if(settings.dailyNotify && settings.dailyCap!=null && todayBytes>=settings.dailyCap)
            events+=AdvisoryEvent("budget:$date","Daily advisory budget reached","Today's measured mobile usage has reached your daily budget. Android readings and background checks can be delayed.",2101)
        val expiry=promo?.expirationTimestamp
        if(promo!=null && promo.isActive && expiry!=null && settings.expiryHours!=null && expiry>now && expiry-now<=settings.expiryHours*3600000L)
            events+=AdvisoryEvent("expiry:${promo.id}:$expiry:${settings.expiryHours}","Promo expiry reminder","${promo.name} expires within ${settings.expiryHours} hours. This reminder uses its actual saved expiry, not a forecast.",2102)
        return events
    }
}
