package com.loadpredictor.domain.model
import com.loadpredictor.domain.backup.*
import org.junit.Assert.*
import org.junit.Test
class BackupCodecTest {
    private fun doc()=BackupDocument(createdAt=1000,promos=listOf(BackupPromo(8,"Saved",9007199254740993L,1,
        offset=6957847020L,active=true,notified=setOf("50"))),alerts=BackupAlerts(at80=false,premature=false))
    @Test fun bytePrecisionAndConfigurationRoundTrip() {
        assertEquals(doc(),BackupCodec.decode(BackupCodec.encode(doc(),2000),2000))
        assertTrue(doc().sameConfiguration(doc().copy(createdAt=2000)))
    }
    @Test fun invalidVersionsDuplicatesAndMultipleActivePromosAreRejected() {
        listOf(doc().copy(formatVersion=99),doc().copy(promos=doc().promos+doc().promos),
            doc().copy(promos=doc().promos+doc().promos.first().copy(id=9)),
            doc().copy(promos=listOf(doc().promos.first().copy(start=3000))),
            doc().copy(promos=listOf(doc().promos.first().copy(offset=Long.MAX_VALUE))))
            .forEach {d->assertThrows(IllegalArgumentException::class.java){BackupCodec.encode(d,2000)}}
    }
    @Test fun oversizedMalformedAndUnrecognizedFilesNeverBecomeDocuments() {
        assertThrows(IllegalArgumentException::class.java){BackupCodec.read(ByteArray(BackupCodec.MAX_BYTES+1).inputStream())}
        assertThrows(Exception::class.java){BackupCodec.decode(byteArrayOf(0xC3.toByte(),0x28))}
        assertThrows(IllegalArgumentException::class.java){BackupCodec.decode("{\"formatVersion\":99}".toByteArray())}
        assertThrows(IllegalArgumentException::class.java){BackupCodec.decode("{}".toByteArray())}
    }
}
