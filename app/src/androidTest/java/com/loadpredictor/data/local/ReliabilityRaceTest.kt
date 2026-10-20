package com.loadpredictor.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loadpredictor.data.local.entity.PromoEntity
import com.loadpredictor.data.repository.PromoRepositoryImpl
import com.loadpredictor.domain.usecase.SavePromoUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReliabilityRaceTest {
    private fun fixture() = PromoEntity(id=1, name="Saved",totalAllowanceBytes=10000000,
        startTimestamp=1000,initialUsageOffsetBytes=100,isActive=true,
        lastActiveBurnRate=10.0,lastSyncDataUsedBytes=1000,lastSyncTimestamp=2000)

    @Test fun missingActivationTargetPreservesExistingSelection() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).build()
        try {
            val dao=db.promoDao();dao.insertPromo(fixture())
            try {dao.setActivePromo(999);fail("Missing target must be rejected")} catch (_:IllegalArgumentException) {}
            assertEquals(1L,dao.getActivePromo().first()?.id)
        } finally {db.close()}
    }
    @Test fun editorSnapshotCannotOverwriteNewerBackgroundCheckpoint() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).build()
        try {
            val dao=db.promoDao();val old=fixture();dao.insertPromo(old)
            dao.updateSyncState(1,20.0,2000,3000)
            SavePromoUseCase(PromoRepositoryImpl(dao))(old.toDomain().copy(name="Renamed"))
            val saved=dao.getPromoById(1).first()!!
            assertEquals("Renamed",saved.name);assertEquals(3000L,saved.lastSyncTimestamp)
            assertEquals(2000L,saved.lastSyncDataUsedBytes);assertEquals(20.0,saved.lastActiveBurnRate!!,0.0)
        } finally {db.close()}
    }
    @Test fun olderObservationCannotReplaceNewerCheckpoint() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).build()
        try {
            val dao=db.promoDao();dao.insertPromo(fixture())
            dao.updateSyncState(1,20.0,2000,4000)
            dao.updateSyncState(1,15.0,1500,3000)
            assertEquals(4000L,dao.getPromoById(1).first()!!.lastSyncTimestamp)
        } finally {db.close()}
    }
    @Test fun baselineChangeRejectsOldObservationAndResetsVelocity() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).build()
        try {
            val dao=db.promoDao();val old=fixture();dao.insertPromo(old)
            dao.saveConfiguredPromo(old.copy(initialUsageOffsetBytes=200))
            assertEquals(0,dao.updateObservedCheckpoint(1,old.startTimestamp,old.initialUsageOffsetBytes,
                old.totalAllowanceBytes,old.lastSyncTimestamp,old.lastSyncDataUsedBytes,30.0,3000,5000))
            val current=dao.findById(1)!!;assertNull(current.lastActiveBurnRate)
            assertEquals(0L,current.lastSyncTimestamp);assertEquals(200L,current.initialUsageOffsetBytes)
        } finally {db.close()}
    }
    @Test fun failedConfiguredSaveDoesNotDeactivateAnotherPromo() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).build()
        try {
            val dao=db.promoDao();dao.insertPromo(fixture())
            try {dao.saveConfiguredPromo(fixture().copy(id=99));fail("Deleted editor target must fail")}
            catch (_:IllegalArgumentException) {}
            assertEquals(1L,dao.getActivePromo().first()?.id)
        } finally {db.close()}
    }

}
