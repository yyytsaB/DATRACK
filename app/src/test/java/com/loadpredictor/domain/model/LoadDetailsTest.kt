package com.loadpredictor.domain.model
import org.junit.Assert.*
import org.junit.Test
import com.loadpredictor.data.local.entity.toEntity
class LoadDetailsTest {
    @Test fun loadMetadataSurvivesMapping() {
        val promo=Promo(name="My promo",totalAllowanceBytes=1024,startTimestamp=1,carrier="Smart",priceCentavos=9900)
        assertEquals(promo,promo.toEntity().toDomain())
        assertNull(promo.copy(carrier=null,priceCentavos=null).toEntity().toDomain().priceCentavos)
    }
    @Test fun priceUsesExactCentavos() {
        assertEquals(9999L, parseLoadPrice("99.99"))
        assertEquals(9900L, parseLoadPrice("99"))
        assertEquals(0L, parseLoadPrice("0"))
        listOf("", "-1", "1.001", "NaN", "Infinity", "92233720368547758.08").forEach { assertNull(parseLoadPrice(it)) }
    }
    @Test fun uidBucketsAreMergedAndSpecialTotalsExcluded() {
        assertEquals(mapOf(10001 to 70L, 10002 to 30L), aggregateUidBytes(listOf(10001 to 50L, 10001 to 20L, 10002 to 30L, -1 to 100L, 10003 to -1L)))
        val a=AppMobileUsage(10001,"One",null,70);val b=AppMobileUsage(10002,"Two",null,30)
        val report=AppUsageReport(listOf(a,b),0,100)
        assertEquals(100L,report.totalBytes);assertEquals(.7f,report.share(a),.001f)
        assertEquals(0f,AppUsageReport(emptyList(),0,1).share(a),0f)
    }
}
