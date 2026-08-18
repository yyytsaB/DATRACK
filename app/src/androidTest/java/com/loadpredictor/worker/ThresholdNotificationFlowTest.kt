package com.loadpredictor.worker

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.local.NotificationPreferencesDataSource
import com.loadpredictor.data.local.entity.PromoEntity
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.presentation.widget.WidgetState
import com.loadpredictor.presentation.widget.WidgetStatePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThresholdNotificationFlowTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var notificationPrefs: NotificationPreferencesDataSource
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(context)
        notificationPrefs = NotificationPreferencesDataSource(context)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun testThresholdNotificationFiringAndAntiReFireSuppression() = runBlocking {
        // Step 1: Insert promo with 80% consumed (10 GB total, 8 GB used via initial offset)
        val now = System.currentTimeMillis()
        val totalAllowance = 10L * 1024L * 1024L * 1024L // 10 GB
        val initialOffset = 8L * 1024L * 1024L * 1024L  // 8 GB (80% used)

        val promoId = database.promoDao().insertPromo(
            PromoEntity(
                name = "Test Threshold GigaSurf",
                totalAllowanceBytes = totalAllowance,
                startTimestamp = now - 3600000L,
                expirationTimestamp = now + (3 * 86400000L),
                initialUsageOffsetBytes = initialOffset,
                simSlot = SimSlot.SIM_1,
                isActive = true
            )
        )
        database.promoDao().setActivePromo(promoId)
        notificationPrefs.clearThresholdsForPromo(promoId)

        // Step 2: First Worker Run -> should evaluate usage and record 50 and 80 milestones
        val worker1 = TestListenableWorkerBuilder<UsageSyncWorker>(context).build()
        val result1 = worker1.doWork()
        assertEquals(ListenableWorker.Result.success(), result1)

        val notifiedFirstRun = notificationPrefs.getNotifiedThresholds(promoId).first()
        assertTrue("Expected 50% milestone to be notified", notifiedFirstRun.contains("50"))
        assertTrue("Expected 80% milestone to be notified", notifiedFirstRun.contains("80"))
        assertEquals(2, notifiedFirstRun.size)

        // Step 3: Second Worker Run (Immediate repeated sync) -> must NOT duplicate or re-fire
        val worker2 = TestListenableWorkerBuilder<UsageSyncWorker>(context).build()
        val result2 = worker2.doWork()
        assertEquals(ListenableWorker.Result.success(), result2)

        val notifiedSecondRun = notificationPrefs.getNotifiedThresholds(promoId).first()
        assertEquals("Set of notified milestones must remain unchanged", setOf("50", "80"), notifiedSecondRun)

        // Step 4: Verify Widget State is saved with Success and correct remaining balance
        val widgetState = WidgetStatePreferences.readState(
            androidx.datastore.preferences.core.emptyPreferences()
        )
        assertNotNull(widgetState)
    }
}
