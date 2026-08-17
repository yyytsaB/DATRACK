package com.loadpredictor.data.local

import com.loadpredictor.data.local.entity.PromoEntity
import com.loadpredictor.data.local.entity.toEntity
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PromoEntityMappingTest {

    @Test
    fun `Expiring Promo toEntity and toDomain round trip preserves all properties`() {
        val domain = Promo(
            id = 42L,
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = 1700000000000L,
            expirationTimestamp = 1800000000000L,
            initialUsageOffsetBytes = 1024L * 1024L * 250L,
            simSlot = SimSlot.SIM_2,
            isActive = true
        )

        val entity = domain.toEntity()

        assertEquals(42L, entity.id)
        assertEquals("Smart GigaSurf 99", entity.name)
        assertEquals(2L * 1024L * 1024L * 1024L, entity.totalAllowanceBytes)
        assertEquals(1700000000000L, entity.startTimestamp)
        assertEquals(1800000000000L, entity.expirationTimestamp)
        assertEquals(1024L * 1024L * 250L, entity.initialUsageOffsetBytes)
        assertEquals(SimSlot.SIM_2, entity.simSlot)
        assertEquals(true, entity.isActive)

        val restoredDomain = entity.toDomain()
        assertEquals(domain, restoredDomain)
    }

    @Test
    fun `Non-expiring Promo toEntity and toDomain round trip preserves null expiration and offset`() {
        val domain = Promo(
            id = 99L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = 1700000000000L,
            expirationTimestamp = null,
            initialUsageOffsetBytes = 4L * 1024L * 1024L * 1024L,
            simSlot = SimSlot.SIM_1,
            isActive = true
        )

        val entity = domain.toEntity()

        assertEquals(99L, entity.id)
        assertEquals("Smart Magic Data 399", entity.name)
        assertNull(entity.expirationTimestamp)
        assertEquals(4L * 1024L * 1024L * 1024L, entity.initialUsageOffsetBytes)

        val restoredDomain = entity.toDomain()
        assertEquals(domain, restoredDomain)
        assertNull(restoredDomain.expirationTimestamp)
        assertEquals(true, restoredDomain.isNoExpiry)
    }

    @Test
    fun `Converters converts SimSlot to and from String`() {
        val converters = Converters()
        assertEquals("SIM_1", converters.fromSimSlot(SimSlot.SIM_1))
        assertEquals("SIM_2", converters.fromSimSlot(SimSlot.SIM_2))

        assertEquals(SimSlot.SIM_1, converters.toSimSlot("SIM_1"))
        assertEquals(SimSlot.SIM_2, converters.toSimSlot("SIM_2"))
        assertEquals(SimSlot.SIM_1, converters.toSimSlot("UNKNOWN"))
    }
}
