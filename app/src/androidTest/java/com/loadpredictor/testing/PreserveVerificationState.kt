package com.loadpredictor.testing

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.loadpredictor.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

/** Restore rows, active selection and preference values even when a mutating test fails. */
class PreserveVerificationState : ExternalResource() {
    private lateinit var restore: suspend () -> Unit
    override fun before() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName == "com.loadpredictor.releasecheck.audit")
        val work = WorkManager.getInstance(context)
        work.cancelAllWork().result.get()
        val dao = AppDatabase.getInstance(context).promoDao()
        val original = dao.getAllPromos().first()
        val corrections=dao.allCorrections()
        val alerts = AlertPreferencesDataSource(context)
        val flags = alerts.alertPreferencesFlow.first()
        val notifications = NotificationPreferencesDataSource(context)
        val ledger = notifications.snapshotLedger()
        restore = {
            work.cancelAllWork().result.get()
            dao.getAllPromos().first().filter { row -> original.none { it.id == row.id } }.forEach {
                notifications.clearThresholdsForPromo(it.id)
                dao.deletePromo(it)
            }
            original.forEach { dao.insertPromo(it) }
            corrections.forEach {dao.insertCorrection(it)}
            alerts.set50AlertEnabled(flags.is50Enabled)
            alerts.set80AlertEnabled(flags.is80Enabled)
            alerts.set90AlertEnabled(flags.is90Enabled)
            alerts.setPrematureAlertEnabled(flags.isPrematureEnabled)
            notifications.replaceLedger(ledger)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        }
    }
    override fun after() = runBlocking { if (::restore.isInitialized) restore() }
}
