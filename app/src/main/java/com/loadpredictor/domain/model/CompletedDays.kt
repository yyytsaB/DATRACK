package com.loadpredictor.domain.model

import java.time.Instant
import java.time.ZoneId

fun UsageBucket.isCompletedDay(now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
    val date = Instant.ofEpochMilli(startTimestamp).atZone(zone).toLocalDate()
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return startTimestamp == start && endTimestamp == end && end <= now
}
