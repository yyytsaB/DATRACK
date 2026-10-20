package com.loadpredictor.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.loadpredictor.worker.WorkManagerScheduler

/**
 * BroadcastReceiver responsible for receiving app widget lifecycle events and binding [LoadPredictorWidget].
 */
class LoadPredictorWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LoadPredictorWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WorkManagerScheduler.enqueueImmediateSync(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkManagerScheduler.enqueueImmediateSync(context)
    }
}

