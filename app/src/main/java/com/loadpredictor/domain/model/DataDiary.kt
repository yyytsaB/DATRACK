package com.loadpredictor.domain.model

import java.time.*

data class DiaryWindow(val start:Long,val end:Long,val zone:ZoneId) {
    companion object {
        fun create(from:LocalDate,through:LocalDate,promo:Promo,now:Long,zone:ZoneId):DiaryWindow {
            val today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            require(!through.isBefore(from) && !through.isAfter(today) && !from.isBefore(today.minusDays(89))){"Choose up to 90 recent calendar days, ending no later than today."}
            val start=maxOf(from.atStartOfDay(zone).toInstant().toEpochMilli(),promo.startTimestamp)
            val end=minOf(through.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),now,promo.expirationTimestamp ?: now)
            require(start<end){"No tracked time exists in this range."}
            return DiaryWindow(start,end,zone)
        }
    }
}
object DiaryCsv {
    private fun cell(text:String):String {
        // Spreadsheet formula injection is possible even in quoted CSV cells.
        val safe=if(text.trimStart().firstOrNull() in listOf('=','+','-','@','\t','\r','\n')) "'"+text else text
        return "\""+safe.replace("\"","\"\"")+"\""
    }
    fun encode(promo:Promo,window:DiaryWindow,buckets:List<UsageBucket>,observedAt:Long):String {
        val lines=mutableListOf("source,scope,timezone,promo_id,promo_name,observed_at_utc,local_date,period_start_utc,period_end_utc,rx_bytes,tx_bytes,total_bytes,period_status")
        buckets.forEach {b->
            require(b.startTimestamp>=window.start && b.endTimestamp<=window.end && b.endTimestamp>b.startTimestamp && b.rxBytes>=0 && b.txBytes>=0)
            val date=Instant.ofEpochMilli(b.startTimestamp).atZone(window.zone).toLocalDate()
            val whole=b.startTimestamp==date.atStartOfDay(window.zone).toInstant().toEpochMilli() && b.endTimestamp==date.plusDays(1).atStartOfDay(window.zone).toInstant().toEpochMilli() && b.endTimestamp<=AdvisoryPolicy.dayStart(observedAt,window.zone)
            val cells=listOf("Android NetworkStatsManager","device-total mobile; carrier balance and offsets excluded",window.zone.id,promo.id.toString(),promo.name,Instant.ofEpochMilli(observedAt).toString(),date.toString(),Instant.ofEpochMilli(b.startTimestamp).toString(),Instant.ofEpochMilli(b.endTimestamp).toString(),b.rxBytes.toString(),b.txBytes.toString(),b.totalBytes.toString(),if(whole)"calendar_complete; Android may revise or delay statistics" else "partial; Android may revise or delay statistics")
            lines+=cells.joinToString(",",transform=::cell)
        }
        return lines.joinToString("\r\n",postfix="\r\n")
    }
}
data class MobileUidUsage(val uid:Int,val label:String,val bytes:Long)
object MobileAttribution {
    fun aggregate(rows:List<Pair<Int,Long>>):Map<Int,Long> {
        val totals=linkedMapOf<Int,Long>()
        rows.filter {it.first != -1 && it.second>=0}.forEach {(uid,bytes)->
            val old=totals[uid] ?: 0L;totals[uid]=if(Long.MAX_VALUE-old<bytes)Long.MAX_VALUE else old+bytes
        }
        return totals
    }
}
