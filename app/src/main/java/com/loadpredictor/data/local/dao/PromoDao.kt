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
        require(findById(id) != null) { "Promo no longer exists" }
        deactivateAllPromos()
        check(activatePromoById(id) == 1)
    }

    @Query("UPDATE promos SET last_active_burn_rate = :burnRate, last_sync_data_used_bytes = :dataUsedBytes, last_sync_timestamp = :syncTimestamp WHERE id = :promoId AND last_sync_timestamp <= :syncTimestamp")
    abstract suspend fun updateSyncState(promoId: Long, burnRate: Double?, dataUsedBytes: Long, syncTimestamp: Long): Int

    @Query("SELECT * FROM promos WHERE id = :id LIMIT 1")
    abstract suspend fun findById(id: Long): PromoEntity?

    @Transaction
    open suspend fun saveConfiguredPromo(input: PromoEntity): Long {
        input.toDomain() // Validate before changing any selection.
        val old = if (input.id == 0L) null else requireNotNull(findById(input.id)) { "Promo no longer exists" }
        val changed = old != null && (old.startTimestamp != input.startTimestamp || old.initialUsageOffsetBytes != input.initialUsageOffsetBytes)
        val current = input.copy(
            correctionRemaining=if(changed) null else old?.correctionRemaining,
            correctionMeasured=if(changed) null else old?.correctionMeasured,
            correctionAt=if(changed) null else old?.correctionAt,
            correctionRevision=old?.correctionRevision ?: 0,
            isActive = false,
            lastActiveBurnRate = if (changed || old == null) null else old.lastActiveBurnRate,
            lastSyncDataUsedBytes = if (changed || old == null) 0 else old.lastSyncDataUsedBytes,
            lastSyncTimestamp = if (changed || old == null) 0 else old.lastSyncTimestamp)
        current.toDomain() // Reject allowance edits incompatible with an accepted carrier anchor.
        if(changed && old!=null) markAllCorrectionsUndone(old.id)
        val id = if (old == null) insertPromo(current) else { check(updatePromo(current) == 1); input.id }
        if (input.isActive) setActivePromo(id)
        return id
    }

    @Query("""UPDATE promos SET last_active_burn_rate=:rate, last_sync_data_used_bytes=:bytes,
        last_sync_timestamp=:at WHERE id=:id AND is_active=1 AND start_timestamp=:start
        AND initial_usage_offset_bytes=:offset AND total_allowance_bytes=:allowance
        AND last_sync_timestamp=:previousAt AND last_sync_data_used_bytes=:previousBytes
        AND :at >= last_sync_timestamp""")
    abstract suspend fun updateObservedCheckpoint(id:Long,start:Long,offset:Long,allowance:Long,
        previousAt:Long,previousBytes:Long,rate:Double?,bytes:Long,at:Long):Int
    @Query("SELECT * FROM balance_corrections WHERE promoId=:id ORDER BY id DESC")
    abstract suspend fun corrections(id:Long):List<com.loadpredictor.data.local.entity.BalanceCorrectionEntity>
    @Query("SELECT * FROM balance_corrections ORDER BY id")
    abstract suspend fun allCorrections():List<com.loadpredictor.data.local.entity.BalanceCorrectionEntity>
    @Insert(onConflict=OnConflictStrategy.ABORT)
    abstract suspend fun insertCorrection(row:com.loadpredictor.data.local.entity.BalanceCorrectionEntity):Long
    @Query("UPDATE balance_corrections SET undone=1 WHERE id=:id")
    abstract suspend fun markCorrectionUndone(id:Long)
    @Query("UPDATE balance_corrections SET undone=1 WHERE promoId=:id")
    abstract suspend fun markAllCorrectionsUndone(id:Long)
    /** Only used inside the journaled, user-confirmed restore transaction. */
    @Query("DELETE FROM promos")
    abstract suspend fun clearForRestore(): Int

}
