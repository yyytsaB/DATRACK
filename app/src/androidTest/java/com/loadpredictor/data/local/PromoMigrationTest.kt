package com.loadpredictor.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loadpredictor.data.local.dao.PromoDao
import com.loadpredictor.domain.model.SimSlot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PromoMigrationTest {

    private val dbName = "migration_test_promo.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigration1To2_preservesExistingData_andPopulatesZeroOffset() = runBlocking {
        // Step 1: Create a database with the exact Schema Version 1 (no initial_usage_offset_bytes)
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `promos` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `total_allowance_bytes` INTEGER NOT NULL,
                            `start_timestamp` INTEGER NOT NULL,
                            `expiration_timestamp` INTEGER,
                            `sim_slot` TEXT NOT NULL,
                            `is_active` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v1Db = helper.writableDatabase

        // Step 2: Insert sample v1 data rows
        v1Db.execSQL(
            """
            INSERT INTO `promos` (`id`, `name`, `total_allowance_bytes`, `start_timestamp`, `expiration_timestamp`, `sim_slot`, `is_active`)
            VALUES (1, 'Smart Magic Data 399', 25769803776, 1700000000000, NULL, 'SIM_1', 1)
            """.trimIndent()
        )
        v1Db.execSQL(
            """
            INSERT INTO `promos` (`id`, `name`, `total_allowance_bytes`, `start_timestamp`, `expiration_timestamp`, `sim_slot`, `is_active`)
            VALUES (2, 'Smart GigaSurf 99', 2147483648, 1700000000000, 1700604800000, 'SIM_2', 0)
            """.trimIndent()
        )
        v1Db.close()

        // Step 3: Open the database using Room at Version 2 with MIGRATION_1_2
        val migratedDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        ).addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val promoDao = migratedDb.promoDao()

        // Step 4: Verify existing rows survived and have initial_usage_offset_bytes == 0
        val allPromos = promoDao.getAllPromos().first()
        assertEquals(2, allPromos.size)

        val magicData = allPromos.find { it.id == 1L }
        assertNotNull("Magic Data promo must survive migration", magicData)
        assertEquals("Smart Magic Data 399", magicData!!.name)
        assertEquals(25769803776L, magicData.totalAllowanceBytes)
        assertNull("Null expiration must remain null", magicData.expirationTimestamp)
        assertEquals(SimSlot.SIM_1, magicData.simSlot)
        assertTrue("Active status must be preserved", magicData.isActive)
        assertEquals(
            "Existing v1 promo must default to 0 offset in v2 schema",
            0L,
            magicData.initialUsageOffsetBytes
        )

        val gigaSurf = allPromos.find { it.id == 2L }
        assertNotNull("GigaSurf promo must survive migration", gigaSurf)
        assertEquals("Smart GigaSurf 99", gigaSurf!!.name)
        assertEquals(2147483648L, gigaSurf.totalAllowanceBytes)
        assertEquals(1700604800000L, gigaSurf.expirationTimestamp)
        assertEquals(SimSlot.SIM_2, gigaSurf.simSlot)
        assertEquals(0L, gigaSurf.initialUsageOffsetBytes)

        // Step 5: Verify new inserts with non-zero offsets work seamlessly on migrated DB
        val newPromo = com.loadpredictor.data.local.entity.PromoEntity(
            name = "Smart Magic Data 99 (Used)",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = 1710000000000L,
            expirationTimestamp = null,
            initialUsageOffsetBytes = 500L * 1024L * 1024L,
            simSlot = SimSlot.SIM_1,
            isActive = false
        )
        val newId = promoDao.insertPromo(newPromo)
        val fetchedNew = promoDao.getPromoById(newId).first()
        assertNotNull(fetchedNew)
        assertEquals(500L * 1024L * 1024L, fetchedNew?.initialUsageOffsetBytes)

        migratedDb.close()
    }
}
