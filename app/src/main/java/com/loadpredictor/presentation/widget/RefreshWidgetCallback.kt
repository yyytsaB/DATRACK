package com.loadpredictor.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.loadpredictor.worker.WorkManagerScheduler

/**
 * Glance action callback invoked when the user interacts with widget refresh triggers.
 * Enqueues an immediate background sync with WorkManager.
 */
class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WorkManagerScheduler.enqueueImmediateSync(context)
    }
}
