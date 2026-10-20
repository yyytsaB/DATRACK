package com.loadpredictor.data.repository

import com.loadpredictor.data.local.dao.PromoDao
import com.loadpredictor.data.local.entity.toEntity
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.repository.PromoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of [PromoRepository] backed by Room database [PromoDao].
 */
class PromoRepositoryImpl(
    private val promoDao: PromoDao
) : PromoRepository {

    override fun getActivePromo(): Flow<Promo?> {
        return promoDao.getActivePromo().map { it?.toDomain() }
    }

    override fun getAllPromos(): Flow<List<Promo>> {
        return promoDao.getAllPromos().map { list -> list.map { it.toDomain() } }
    }

    override fun getPromoById(id: Long): Flow<Promo?> {
        return promoDao.getPromoById(id).map { it?.toDomain() }
    }

    override fun getActivePromoForSim(simSlot: SimSlot): Flow<Promo?> {
        return promoDao.getActivePromoForSim(simSlot).map { it?.toDomain() }
    }

    override suspend fun saveConfiguredPromo(promo: Promo): Long =
        com.loadpredictor.domain.sync.DataOperationGate.exclusive { promoDao.saveConfiguredPromo(promo.toEntity()) }

    override suspend fun updateObservedSyncState(observed:Promo,burnRate:Double?,bytes:Long,at:Long):Boolean =
        com.loadpredictor.domain.sync.DataOperationGate.exclusive {
            promoDao.updateObservedCheckpoint(observed.id,observed.startTimestamp,observed.initialUsageOffsetBytes,
                observed.totalAllowanceBytes,observed.lastSyncTimestamp,observed.lastSyncDataUsedBytes,burnRate,bytes,at)==1
        }

    override suspend fun isCurrentConfiguration(promo:Promo):Boolean {
        val current=promoDao.findById(promo.id)?.toDomain() ?: return false
        return current.isActive && current.copy(lastActiveBurnRate=promo.lastActiveBurnRate,
            lastSyncDataUsedBytes=promo.lastSyncDataUsedBytes,lastSyncTimestamp=promo.lastSyncTimestamp)==promo
    }

    override suspend fun insertPromo(promo: Promo): Long {
        return promoDao.insertPromo(promo.toEntity())
    }

    override suspend fun updatePromo(promo: Promo) {
        promoDao.updatePromo(promo.toEntity())
    }

    override suspend fun deletePromo(promo: Promo) {
        com.loadpredictor.domain.sync.DataOperationGate.exclusive { promoDao.deletePromo(promo.toEntity()) }
    }

    override suspend fun setActivePromo(id: Long) {
        com.loadpredictor.domain.sync.DataOperationGate.exclusive { promoDao.setActivePromo(id) }
    }

    override suspend fun updateSyncState(
        promoId: Long,
        burnRate: Double?,
        dataUsedBytes: Long,
        syncTimestamp: Long
    ) {
        promoDao.updateSyncState(promoId, burnRate, dataUsedBytes, syncTimestamp)
    }
}
