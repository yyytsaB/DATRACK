package com.loadpredictor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loadpredictor.data.local.dao.PromoDao
import com.loadpredictor.data.local.entity.PromoEntity

/**
 * Room database instance for local data persistence.
 */
@Database(
    entities = [PromoEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun promoDao(): PromoDao

    companion object {
        private const val DATABASE_NAME = "load_predictor.db"

        /**
         * Migration from version 1 to 2: adds `initial_usage_offset_bytes` column with default 0.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE promos ADD COLUMN initial_usage_offset_bytes INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from version 2 to 3: adds `last_active_burn_rate`, `last_sync_data_used_bytes`,
         * and `last_sync_timestamp` columns to persist active velocity across app restarts and idle periods.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE promos ADD COLUMN last_active_burn_rate REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE promos ADD COLUMN last_sync_data_used_bytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE promos ADD COLUMN last_sync_timestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
