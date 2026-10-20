package com.loadpredictor.domain.backup

import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.nio.charset.CodingErrorAction

@Serializable data class BackupAlerts(val at50:Boolean=true,val at80:Boolean=true,val at90:Boolean=true,val premature:Boolean=true)
@Serializable data class BackupPromo(val id:Long,val name:String,val allowance:Long,val start:Long,
    val expiry:Long?=null,val offset:Long=0,val sim:String="SIM_1",val active:Boolean=false,
    val notified:Set<String> = emptySet(),
    val correctionRemaining:Long?=null,val correctionMeasured:Long?=null,val correctionAt:Long?=null,val correctionRevision:Long=0) {
    fun toPromo()=Promo(id,name,allowance,start,expiry,offset,SimSlot.valueOf(sim),active,
        correctionRemaining=correctionRemaining,correctionMeasured=correctionMeasured,correctionAt=correctionAt,correctionRevision=correctionRevision)
}
@Serializable data class BackupCorrection(val id:Long,val promoId:Long,val remaining:Long,val measured:Long,val observedAt:Long,val createdAt:Long,val undone:Boolean=false)
@Serializable data class BackupDocument(val kind:String="datrack.configuration",val formatVersion:Int=2,
    val createdAt:Long,val promos:List<BackupPromo>,val alerts:BackupAlerts=BackupAlerts(),val corrections:List<BackupCorrection> = emptyList()) {
    fun validate(now:Long) {
        require(kind=="datrack.configuration" && formatVersion in 1..2) {"Unsupported backup format or version"}
        require(createdAt>=0 && promos.size<=1000) {"Invalid backup size or date"}
        require(promos.map {it.id}.toSet().size==promos.size && promos.count {it.active}<=1) {"Duplicate IDs or multiple active promos"}
        require(corrections.size<=10000 && corrections.map {it.id}.toSet().size==corrections.size){"Invalid correction history"}
        corrections.forEach {c->
            val promo=promos.firstOrNull {it.id==c.promoId} ?: error("Orphan correction")
            require(c.id>0 && c.remaining>=0 && c.measured>=0 && c.observedAt>=0 && c.createdAt>=c.observedAt && c.createdAt<=now){"Invalid correction"}
            if(!c.undone) require(c.remaining<=promo.allowance && c.observedAt>=promo.start){"Correction does not match promo"}
        }
        promos.forEach {
            require(it.id>0 && it.name.length<=512 && it.start in 0..now) {"Invalid promo ID, name or tracking date"}
            it.toPromo()
            val anchor=corrections.filter {c->c.promoId==it.id && !c.undone}.maxByOrNull {c->c.id}
            require(it.correctionRemaining==anchor?.remaining && it.correctionMeasured==anchor?.measured && it.correctionAt==anchor?.observedAt){"Correction anchor does not match history"}
            require(it.correctionAt==null || it.correctionAt<=now){"Future correction"}
            require(it.notified.all {key->key in setOf("50","80","90","PREMATURE_DEPLETION")}) {"Invalid notification milestone"}
        }
    }
    fun sameConfiguration(other:BackupDocument)=copy(createdAt=0)==other.copy(createdAt=0)
}
object BackupCodec {
    const val MAX_BYTES=1048576
    private val json=Json {encodeDefaults=true;prettyPrint=true}
    fun encode(doc:BackupDocument,now:Long=System.currentTimeMillis()):ByteArray {
        doc.validate(now)
        return json.encodeToString(doc).toByteArray(Charsets.UTF_8).also {require(it.size<=MAX_BYTES){"Backup is too large"}}
    }
    fun decode(bytes:ByteArray,now:Long=System.currentTimeMillis()):BackupDocument {
        require(bytes.size<=MAX_BYTES){"Backup exceeds 1 MB"}
        val decoder=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
        return json.decodeFromString<BackupDocument>(decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()).also {it.validate(now)}
    }
    fun read(input:InputStream):ByteArray {
        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
        while(true) {val n=input.read(buffer);if(n<0)break;require(out.size()+n<=MAX_BYTES){"Backup exceeds 1 MB"};out.write(buffer,0,n)}
        return out.toByteArray()
    }
}
