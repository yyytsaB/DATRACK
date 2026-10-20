package com.loadpredictor.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.local.entity.BalanceCorrectionEntity
import com.loadpredictor.data.stats.NetworkStatsDataSource
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.sync.DataOperationGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CarrierComparison(val promo:Promo,val reported:Long,val observedAt:Long,val measured:Long,val appRemaining:Long)

class CarrierBalanceService(context:Context,
    private val query:(Long,Long)->Long = {start,end->NetworkStatsDataSource(context).queryMobileUsageBytes(start,end)}) {
    private val app=context.applicationContext
    private val db=AppDatabase.getInstance(app)
    private val dao=db.promoDao()
    suspend fun compare(id:Long,remaining:Long,at:Long,now:Long):CarrierComparison=withContext(Dispatchers.IO) {
        DataOperationGate.exclusive {
            val promo=requireNotNull(dao.findById(id)).toDomain()
            require(promo.isActive && !promo.isExpired(now)){"Select an active, unexpired promo."}
            require(at in promo.startTimestamp..now && now-at<=15*60_000){"Use a carrier reading from the last 15 minutes."}
            require(promo.correctionAt==null || at>promo.correctionAt){"Use a newer carrier reading."}
            require(remaining in 0..promo.totalAllowanceBytes){"Balance must be between zero and the allowance."}
            val measured=query(promo.startTimestamp,at)
            CarrierComparison(promo,remaining,at,measured,promo.remainingBytes(measured))
        }
    }
    suspend fun apply(comparison:CarrierComparison,now:Long)=withContext(Dispatchers.IO) {DataOperationGate.exclusive {
        require(now-comparison.observedAt in 0..15*60_000){"Comparison expired. Review a fresh reading."}
        val p=comparison.promo
        db.withTransaction {
            val current=requireNotNull(dao.findById(p.id)).toDomain()
            require(current.copy(lastActiveBurnRate=p.lastActiveBurnRate,lastSyncDataUsedBytes=p.lastSyncDataUsedBytes,lastSyncTimestamp=p.lastSyncTimestamp)==p){"Promo changed. Compare again."}
            val row=BalanceCorrectionEntity(promoId=p.id,remaining=comparison.reported,measured=comparison.measured,observedAt=comparison.observedAt,createdAt=now)
            dao.insertCorrection(row)
            dao.updatePromo(requireNotNull(dao.findById(p.id)).copy(correctionRemaining=row.remaining,correctionMeasured=row.measured,correctionAt=row.observedAt,correctionRevision=p.correctionRevision+1))
        }

    }}
    suspend fun undo(id:Long,expectedRevision:Long)=withContext(Dispatchers.IO) {DataOperationGate.exclusive {
        db.withTransaction {
            val p=requireNotNull(dao.findById(id))
            require(p.correctionRevision==expectedRevision){"Correction changed. Reopen before undoing."}
            val latest=dao.corrections(id).firstOrNull {!it.undone} ?: error("No correction to undo")
            dao.markCorrectionUndone(latest.id)
            val prior=dao.corrections(id).firstOrNull {!it.undone}
            dao.updatePromo(p.copy(correctionRemaining=prior?.remaining,correctionMeasured=prior?.measured,correctionAt=prior?.observedAt,correctionRevision=p.correctionRevision+1))
        }

    }}
    suspend fun history(id:Long)=withContext(Dispatchers.IO){dao.corrections(id)}
}
