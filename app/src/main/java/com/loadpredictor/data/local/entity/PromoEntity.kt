package com.loadpredictor.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot

/**
 * Room entity representation of a prepaid mobile data promo.
 *
 * Store allowances in raw bytes ([totalAllowanceBytes]) for mathematical precision.
 */
@Entity(tableName = "promos")
data class PromoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "total_allowance_bytes")
    val totalAllowanceBytes: Long,

    @ColumnInfo(name = "start_timestamp")
    val startTimestamp: Long,

    @ColumnInfo(name = "expiration_timestamp")
    val expirationTimestamp: Long,

    @ColumnInfo(name = "sim_slot")
    val simSlot: SimSlot = SimSlot.SIM_1,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false
) {
    fun toDomain(): Promo {
        return Promo(
            id = id,
            name = name,
            totalAllowanceBytes = totalAllowanceBytes,
            startTimestamp = startTimestamp,
            expirationTimestamp = expirationTimestamp,
            simSlot = simSlot,
            isActive = isActive
        )
    }
}

/**
 * Domain to Entity mapper extension function.
 */
fun Promo.toEntity(): PromoEntity {
    return PromoEntity(
        id = id,
        name = name,
        totalAllowanceBytes = totalAllowanceBytes,
        startTimestamp = startTimestamp,
        expirationTimestamp = expirationTimestamp,
        simSlot = simSlot,
        isActive = isActive
    )
}
