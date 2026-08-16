package com.loadpredictor.data.stats

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Utility helper to verify and request the PACKAGE_USAGE_STATS permission (Usage Access).
 *
 * Usage Access is a Special App Access permission managed via AppOpsManager rather than
 * standard runtime permission dialogues.
 */
class UsageAccessHelper(private val context: Context) {

    /**
     * Checks if the app has been granted Usage Access (PACKAGE_USAGE_STATS).
     */
    fun hasUsageAccessPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }

        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Creates an Intent to navigate directly to the system Usage Access settings screen.
     * Attempts to direct to the app's specific details if supported on the OS version,
     * with a fallback to the general Usage Access settings list.
     */
    fun createUsageAccessSettingsIntent(): Intent {
        val packageUri = Uri.parse("package:${context.packageName}")
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, packageUri)
        if (intent.resolveActivity(context.packageManager) != null) {
            return intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        }
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
