package com.loadpredictor.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.local.entity.PromoEntity
import com.loadpredictor.domain.model.SimSlot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(context)
    }

    @Test
    fun testUsageSyncWorkerExecutesSuccessfullyOnRealDevice() = runBlocking {
        // Insert a test promo to ensure active promo path is exercised
        val now = System.currentTimeMillis()
        val promo = PromoEntity(
            name = "Test Magic Data 399",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = now - 3600000L,
            expirationTimestamp = null,
            initialUsageOffsetBytes = 0L,
            simSlot = SimSlot.SIM_1,
            isActive = true
        )
        database.promoDao().insertPromo(promo)

        val worker = TestListenableWorkerBuilder<UsageSyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }
}
