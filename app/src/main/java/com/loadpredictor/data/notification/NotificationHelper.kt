package com.loadpredictor.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.loadpredictor.MainActivity
import com.loadpredictor.R
import java.util.Locale

/**
 * System notification helper registering channels and posting data burn threshold alerts.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "data_burn_alerts"
        const val CHANNEL_NAME = "Data Burn & Threshold Alerts"
        const val NOTIFICATION_ID_THRESHOLD = 1001
        const val NOTIFICATION_ID_PREMATURE = 1002
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when prepaid mobile data reaches 50%, 80%, 90% or projects premature exhaustion."
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Posts an alert when data usage crosses 50%, 80%, or 90% of total allocation.
     */
    fun showThresholdAlert(promoName: String, thresholdPercent: Int, remainingBytes: Long) {
        if (!canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            thresholdPercent,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedRemaining = com.loadpredictor.util.DataFormatter.formatBytes(remainingBytes)
        val title = "⚠️ Data Alert: $thresholdPercent% Used"
        val message = "You have consumed $thresholdPercent% of your $promoName allowance. $formattedRemaining remaining."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_THRESHOLD + thresholdPercent, notification)
        } catch (e: SecurityException) {
            // Permission revoked
        }
    }

    /**
     * Posts an alert when an expiring promo is projected to deplete prematurely.
     */
    fun showPrematureDepletionAlert(promoName: String, hoursEarly: Long, remainingBytes: Long) {
        if (!canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_PREMATURE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedRemaining = com.loadpredictor.util.DataFormatter.formatBytes(remainingBytes)
        val earlyText = if (hoursEarly >= 24) "${hoursEarly / 24} days" else "$hoursEarly hours"
        val title = "🔥 Burning Fast: $promoName"
        val message = "At your current pace, your data will run out $earlyText before promo expiration. ($formattedRemaining remaining)"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PREMATURE, notification)
        } catch (e: SecurityException) {
            // Permission revoked
        }
    }
}

