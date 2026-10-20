package com.loadpredictor.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.*
class DataDiaryTest {
    private val zone=ZoneId.of("Asia/Manila")
    private val today=LocalDate.of(2026,10,20)
    private val now=today.atTime(12,0).atZone(zone).toInstant().toEpochMilli()
    private val start=today.minusDays(6).atTime(15,0).atZone(zone).toInstant().toEpochMilli()
    private val promo=Promo(id=2,name="=SUM(1,2)\"",totalAllowanceBytes=1000,startTimestamp=start,initialUsageOffsetBytes=500)
    @Test fun rangeClipsTrackingAndExpiryAndRejectsFutureAndTooOldDates() {
        val expiry=today.atTime(8,0).atZone(zone).toInstant().toEpochMilli()
        val w=DiaryWindow.create(today.minusDays(6),today,promo.copy(expirationTimestamp=expiry),now,zone)
        assertEquals(start,w.start);assertEquals(expiry,w.end)
        assertThrows(IllegalArgumentException::class.java){DiaryWindow.create(today,today.plusDays(1),promo,now,zone)}
        assertThrows(IllegalArgumentException::class.java){DiaryWindow.create(today.minusDays(90),today,promo,now,zone)}
        assertThrows(IllegalArgumentException::class.java){DiaryWindow.create(today,today.minusDays(1),promo,now,zone)}
    }
    @Test fun csvRetainsMeasuredBytesEscapesNamesAndLabelsPartialAndCalendarCompleteDays() {
        val w=DiaryWindow.create(today.minusDays(6),today,promo,now,zone)
        val midnight=today.minusDays(5).atStartOfDay(zone).toInstant().toEpochMilli()
        val next=today.minusDays(4).atStartOfDay(zone).toInstant().toEpochMilli()
        val csv=DiaryCsv.encode(promo,w,listOf(UsageBucket(start,midnight,123,456),UsageBucket(midnight,next,100,200)),now)
        assertTrue(csv.contains("\"'=SUM(1,2)\"\"\""))
        assertTrue(csv.contains("\"579\""));assertTrue(csv.contains("partial; Android"));assertTrue(csv.contains("calendar_complete; Android"))
        assertFalse(csv.contains("\"1079\"")) // No historical offset counted as traffic.
        assertTrue(csv.contains("Asia/Manila"));assertEquals(3,csv.trim().lines().size)
    }
    @Test fun outOfWindowOrNegativeMeasurementsCannotBeExported() {
        val w=DiaryWindow.create(today.minusDays(6),today,promo,now,zone)
        assertThrows(IllegalArgumentException::class.java){DiaryCsv.encode(promo,w,listOf(UsageBucket(start-1,now,1,1)),now)}
        assertThrows(IllegalArgumentException::class.java){DiaryCsv.encode(promo,w,listOf(UsageBucket(start,now,-1,1)),now)}
    }
    @Test fun dstWindowUsesCalendarDaysRatherThanFixed24HourSlices() {
        val ny=ZoneId.of("America/New_York");val date=LocalDate.of(2026,3,8)
        val end=date.plusDays(1).atStartOfDay(ny).toInstant().toEpochMilli()
        val w=DiaryWindow.create(date,date,promo.copy(startTimestamp=1),end,ny)
        assertEquals(23*3600000L,w.end-w.start)
    }
    @Test fun uidBucketsAggregateSharedAppsWithoutDoubleCountingSummaryUidAndGuardOverflow() {
        val grouped=MobileAttribution.aggregate(listOf(1000 to 20L,1000 to 30L,2000 to 70L,-1 to 120L,-4 to 5L,1000 to -1L))
        assertEquals(mapOf(1000 to 50L,2000 to 70L,-4 to 5L),grouped)
        assertEquals(Long.MAX_VALUE,MobileAttribution.aggregate(listOf(1000 to Long.MAX_VALUE,1000 to 1L))[1000])
    }
}
