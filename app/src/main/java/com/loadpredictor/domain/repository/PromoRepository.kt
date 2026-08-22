package com.loadpredictor.domain.repository

import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing mobile data promos.
 */
interface PromoRepository {
    /**
     * Observes the currently active promo context.
     */
    fun getActivePromo(): Flow<Promo?>

    /**
     * Observes all configured promos.
     */
    fun getAllPromos(): Flow<List<Promo>>

    /**
     * Observes a specific promo by its unique identifier.
     */
    fun getPromoById(id: Long): Flow<Promo?>

    /**
     * Observes the active promo for a given SIM slot.
     */
    fun getActivePromoForSim(simSlot: SimSlot): Flow<Promo?>

    /**
     * Inserts a new promo and returns its generated ID.
     */
    suspend fun insertPromo(promo: Promo): Long

    /**
     * Updates an existing promo.
     */
    suspend fun updatePromo(promo: Promo)

    /**
     * Deletes a promo.
     */
    suspend fun deletePromo(promo: Promo)

    /**
     * Sets the active promo context, deactivating any previously active promo.
     */
    suspend fun setActivePromo(id: Long)

    /**
     * Atomically updates the persisted active burn rate and sync metadata for a promo.
     */
    suspend fun updateSyncState(promoId: Long, burnRate: Double?, dataUsedBytes: Long, syncTimestamp: Long)
}
