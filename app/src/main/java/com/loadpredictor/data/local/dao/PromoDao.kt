package com.loadpredictor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.loadpredictor.data.local.entity.PromoEntity
import com.loadpredictor.domain.model.SimSlot
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PromoDao {

    @Query("SELECT * FROM promos WHERE is_active = 1 LIMIT 1")
    abstract fun getActivePromo(): Flow<PromoEntity?>

    @Query("SELECT * FROM promos ORDER BY start_timestamp DESC")
    abstract fun getAllPromos(): Flow<List<PromoEntity>>

    @Query("SELECT * FROM promos WHERE id = :id LIMIT 1")
    abstract fun getPromoById(id: Long): Flow<PromoEntity?>

    @Query("SELECT * FROM promos WHERE sim_slot = :simSlot AND is_active = 1 LIMIT 1")
    abstract fun getActivePromoForSim(simSlot: SimSlot): Flow<PromoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPromo(promo: PromoEntity): Long

    @Update
    abstract suspend fun updatePromo(promo: PromoEntity): Int

    @Delete
    abstract suspend fun deletePromo(promo: PromoEntity): Int

    @Query("UPDATE promos SET is_active = 0")
    abstract suspend fun deactivateAllPromos(): Int

    @Query("UPDATE promos SET is_active = 1 WHERE id = :id")
    abstract suspend fun activatePromoById(id: Long): Int

    /**
     * Atomically sets the single active promo context.
     */
    @Transaction
    open suspend fun setActivePromo(id: Long) {
        deactivateAllPromos()
        activatePromoById(id)
    }

    @Query("UPDATE promos SET last_active_burn_rate = :burnRate, last_sync_data_used_bytes = :dataUsedBytes, last_sync_timestamp = :syncTimestamp WHERE id = :promoId")
    abstract suspend fun updateSyncState(promoId: Long, burnRate: Double?, dataUsedBytes: Long, syncTimestamp: Long): Int
}
