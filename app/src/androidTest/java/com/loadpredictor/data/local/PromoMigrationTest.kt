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

        // Step 3: Open the database using Room at Version 4 with MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4
        val migratedDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
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

    @Test
    fun testMigration1To2To3To4_fullRealisticUpgradeChain_preservesDataAndInitializesSyncState() = runBlocking {
        // Step 1: Create a database with the exact Schema Version 1 (no offset, no sync state columns)
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

        // Step 3: Open the database with Room at Version 4 with MIGRATION_1_2, MIGRATION_2_3, and MIGRATION_3_4
        val migratedDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        val promoDao = migratedDb.promoDao()

        // Step 4: Verify existing rows survived and have all defaults properly initialized
        val allPromos = promoDao.getAllPromos().first()
        assertEquals(2, allPromos.size)

        val magicData = allPromos.find { it.id == 1L }
        assertNotNull("Magic Data promo must survive full migration chain", magicData)
        assertEquals("Smart Magic Data 399", magicData!!.name)
        assertEquals(25769803776L, magicData.totalAllowanceBytes)
        assertNull(magicData.expirationTimestamp)
        assertEquals(0L, magicData.initialUsageOffsetBytes)
        assertNull(magicData.lastActiveBurnRate)
        assertEquals(0L, magicData.lastSyncDataUsedBytes)
        assertEquals(0L, magicData.lastSyncTimestamp)

        val gigaSurf = allPromos.find { it.id == 2L }
        assertNotNull("GigaSurf promo must survive full migration chain", gigaSurf)
        assertEquals("Smart GigaSurf 99", gigaSurf!!.name)
        assertEquals(2147483648L, gigaSurf.totalAllowanceBytes)
        assertEquals(1700604800000L, gigaSurf.expirationTimestamp)
        assertEquals(0L, gigaSurf.initialUsageOffsetBytes)
        assertNull(gigaSurf.lastActiveBurnRate)
        assertEquals(0L, gigaSurf.lastSyncDataUsedBytes)
        assertEquals(0L, gigaSurf.lastSyncTimestamp)

        // Step 5: Verify updateSyncState works seamlessly on migrated rows
        promoDao.updateSyncState(
            promoId = 1L,
            burnRate = 3456789.0,
            dataUsedBytes = 54022984L,
            syncTimestamp = 1787408166808L
        )
        val updatedMagicData = promoDao.getPromoById(1L).first()
        assertNotNull(updatedMagicData)
        assertEquals(3456789.0, updatedMagicData!!.lastActiveBurnRate)
        assertEquals(54022984L, updatedMagicData.lastSyncDataUsedBytes)
        assertEquals(1787408166808L, updatedMagicData.lastSyncTimestamp)

        migratedDb.close()
    }

    @Test
    fun testMigration2To3To4_preservesExistingData_andInitializesSyncState() = runBlocking {
        // Step 1: Create a database with exact Schema Version 2 (has initial_usage_offset_bytes, but no sync state columns)
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `promos` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `total_allowance_bytes` INTEGER NOT NULL,
                            `start_timestamp` INTEGER NOT NULL,
                            `expiration_timestamp` INTEGER,
                            `initial_usage_offset_bytes` INTEGER NOT NULL DEFAULT 0,
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
        val v2Db = helper.writableDatabase

        // Step 2: Insert sample v2 data rows
        v2Db.execSQL(
            """
            INSERT INTO `promos` (`id`, `name`, `total_allowance_bytes`, `start_timestamp`, `expiration_timestamp`, `initial_usage_offset_bytes`, `sim_slot`, `is_active`)
            VALUES (1, 'Smart Magic Data 399 (Mid-cycle)', 25769803776, 1700000000000, NULL, 5368709120, 'SIM_1', 1)
            """.trimIndent()
        )
        v2Db.close()

        // Step 3: Open the database using Room at Version 4 with MIGRATION_2_3 and MIGRATION_3_4
        val migratedDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        ).addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        val promoDao = migratedDb.promoDao()

        // Step 4: Verify existing v2 row survived with intact offset and default sync state
        val promo = promoDao.getPromoById(1L).first()
        assertNotNull("v2 promo must survive migration to v4", promo)
        assertEquals("Smart Magic Data 399 (Mid-cycle)", promo!!.name)
        assertEquals(5368709120L, promo.initialUsageOffsetBytes)
        assertNull(promo.lastActiveBurnRate)
        assertEquals(0L, promo.lastSyncDataUsedBytes)
        assertEquals(0L, promo.lastSyncTimestamp)

        migratedDb.close()
    }

    @Test
    fun testMigration3To4_nullifiesLastActiveBurnRate_andPreservesAllOtherFields() = runBlocking {
        // Step 1: Create a database with exact Schema Version 3
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `promos` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `total_allowance_bytes` INTEGER NOT NULL,
                            `start_timestamp` INTEGER NOT NULL,
                            `expiration_timestamp` INTEGER,
                            `initial_usage_offset_bytes` INTEGER NOT NULL DEFAULT 0,
                            `sim_slot` TEXT NOT NULL,
                            `is_active` INTEGER NOT NULL,
                            `last_active_burn_rate` REAL DEFAULT NULL,
                            `last_sync_data_used_bytes` INTEGER NOT NULL DEFAULT 0,
                            `last_sync_timestamp` INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v3Db = helper.writableDatabase

        // Step 2: Insert sample v3 data rows with a legacy burn rate
        v3Db.execSQL(
            """
            INSERT INTO `promos` (`id`, `name`, `total_allowance_bytes`, `start_timestamp`, `expiration_timestamp`, `initial_usage_offset_bytes`, `sim_slot`, `is_active`, `last_active_burn_rate`, `last_sync_data_used_bytes`, `last_sync_timestamp`)
            VALUES (1, 'Smart GigaSurf 99', 2147483648, 1700000000000, 1700604800000, 0, 'SIM_1', 1, 987654.32, 50000000, 1700100000000)
            """.trimIndent()
        )
        v3Db.close()

        // Step 3: Open database with Room at Version 4 using MIGRATION_1_2, MIGRATION_2_3, and MIGRATION_3_4
        val migratedDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        val promoDao = migratedDb.promoDao()

        // Step 4: Verify that last_active_burn_rate was reset to null, while all other fields remained intact
        val promo = promoDao.getPromoById(1L).first()
        assertNotNull(promo)
        assertEquals("Smart GigaSurf 99", promo!!.name)
        assertEquals(2147483648L, promo.totalAllowanceBytes)
        assertEquals(1700000000000L, promo.startTimestamp)
        assertEquals(1700604800000L, promo.expirationTimestamp)
        assertEquals(0L, promo.initialUsageOffsetBytes)
        assertEquals(SimSlot.SIM_1, promo.simSlot)
        assertTrue(promo.isActive)
        assertNull("MIGRATION_3_4 must nullify legacy burn rate for clean recalibration", promo.lastActiveBurnRate)
        assertEquals(50000000L, promo.lastSyncDataUsedBytes)
        assertEquals(1700100000000L, promo.lastSyncTimestamp)

        migratedDb.close()
    }
}
