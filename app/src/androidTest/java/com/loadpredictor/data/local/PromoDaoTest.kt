package com.loadpredictor.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loadpredictor.data.local.dao.PromoDao
import com.loadpredictor.data.local.entity.PromoEntity
import com.loadpredictor.domain.model.SimSlot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PromoDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var promoDao: PromoDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        promoDao = database.promoDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    private suspend fun insertSamplePromos() {
        val promo1 = PromoEntity(
            id = 1L,
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = 1000L,
            expirationTimestamp = 5000L,
            simSlot = SimSlot.SIM_1,
            isActive = false
        )
        val promo2 = PromoEntity(
            id = 2L,
            name = "Smart Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = 2000L,
            expirationTimestamp = 8000L,
            simSlot = SimSlot.SIM_1,
            isActive = false
        )
        val promo3 = PromoEntity(
            id = 3L,
            name = "Smart Power All 99",
            totalAllowanceBytes = 8L * 1024L * 1024L * 1024L,
            startTimestamp = 3000L,
            expirationTimestamp = 9000L,
            simSlot = SimSlot.SIM_2,
            isActive = false
        )

        promoDao.insertPromo(promo1)
        promoDao.insertPromo(promo2)
        promoDao.insertPromo(promo3)
    }

    @Test
    fun test1_setActivePromo_leavesExactlyOneActivePromo_whenInitialPromoActivated() = runBlocking {
        insertSamplePromos()

        promoDao.setActivePromo(1L)

        val allPromos = promoDao.getAllPromos().first()
        val activeCount = allPromos.count { it.isActive }
        val activePromo = promoDao.getActivePromo().first()

        assertEquals("Exactly one promo must be active after setting promo 1", 1, activeCount)
        assertNotNull(activePromo)
        assertEquals(1L, activePromo?.id)
        assertTrue(activePromo?.isActive == true)
    }

    @Test
    fun test2_setActivePromo_deactivatesPreviousPromo_whenSwitchingBetweenPromos() = runBlocking {
        insertSamplePromos()

        promoDao.setActivePromo(1L)
        promoDao.setActivePromo(2L)

        val allPromos = promoDao.getAllPromos().first()
        val activeCount = allPromos.count { it.isActive }
        val activePromo = promoDao.getActivePromo().first()

        assertEquals("Exactly one promo must be active after switching to promo 2", 1, activeCount)
        assertNotNull(activePromo)
        assertEquals(2L, activePromo?.id)
        assertEquals("Smart Magic Data 399", activePromo?.name)
    }

    @Test
    fun test3_setActivePromo_isIdempotent_whenRepeatedlyActivated() = runBlocking {
        insertSamplePromos()

        promoDao.setActivePromo(2L)
        promoDao.setActivePromo(2L)
        promoDao.setActivePromo(2L)

        val allPromos = promoDao.getAllPromos().first()
        val activeCount = allPromos.count { it.isActive }
        val activePromo = promoDao.getActivePromo().first()

        assertEquals("Repeated activations must maintain exactly one active promo", 1, activeCount)
        assertEquals(2L, activePromo?.id)
    }

    @Test
    fun test4_setActivePromo_maintainsSingleActiveInvariant_acrossDifferentSimSlots() = runBlocking {
        insertSamplePromos()

        // Promo 1 is SIM_1, Promo 3 is SIM_2
        promoDao.setActivePromo(1L)
        promoDao.setActivePromo(3L)

        val allPromos = promoDao.getAllPromos().first()
        val activeCount = allPromos.count { it.isActive }
        val activePromo = promoDao.getActivePromo().first()

        assertEquals("Exactly one promo must be active across all SIM slots", 1, activeCount)
        assertEquals(3L, activePromo?.id)
        assertEquals(SimSlot.SIM_2, activePromo?.simSlot)
    }
}
