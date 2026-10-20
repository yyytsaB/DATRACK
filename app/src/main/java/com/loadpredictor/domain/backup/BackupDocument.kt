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
    val notified:Set<String> = emptySet()) {
    fun toPromo()=Promo(id,name,allowance,start,expiry,offset,SimSlot.valueOf(sim),active)
}
@Serializable data class BackupDocument(val kind:String="datrack.configuration",val formatVersion:Int=1,
    val createdAt:Long,val promos:List<BackupPromo>,val alerts:BackupAlerts=BackupAlerts()) {
    fun validate(now:Long) {
        require(kind=="datrack.configuration" && formatVersion==1) {"Unsupported backup format or version"}
        require(createdAt>=0 && promos.size<=1000) {"Invalid backup size or date"}
        require(promos.map {it.id}.toSet().size==promos.size && promos.count {it.active}<=1) {"Duplicate IDs or multiple active promos"}
        promos.forEach {
            require(it.id>0 && it.name.length<=512 && it.start in 0..now) {"Invalid promo ID, name or tracking date"}
            it.toPromo()
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
