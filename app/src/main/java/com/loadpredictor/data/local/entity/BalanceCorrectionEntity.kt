package com.loadpredictor.data.local.entity

import androidx.room.*
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName="balance_corrections",foreignKeys=[ForeignKey(entity=PromoEntity::class,
    parentColumns=["id"],childColumns=["promoId"],onDelete=ForeignKey.CASCADE)],indices=[Index("promoId")])
data class BalanceCorrectionEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,
    val promoId:Long,val remaining:Long,val measured:Long,val observedAt:Long,val createdAt:Long,
    val undone:Boolean=false)
