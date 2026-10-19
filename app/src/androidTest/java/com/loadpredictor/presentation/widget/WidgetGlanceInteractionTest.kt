package com.loadpredictor.presentation.widget

import android.content.Context
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.SimSlot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetGlanceInteractionTest {
    @Test fun testWidgetStateSavingAndMultiPathTransitions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName.endsWith(".audit")) { "Widget tests require the isolated preview package" }
        val ids = GlanceAppWidgetManager(context).getGlanceIds(LoadPredictorWidget::class.java)
        assumeTrue("Place a DATRACK Preview widget before running this device interaction test", ids.isNotEmpty())
        val id = ids.first()
        val original = WidgetStatePreferences.readState(getAppWidgetState(context, PreferencesGlanceStateDefinition, id))
        val now = System.currentTimeMillis()
        val success = WidgetState.Success("Widget fixture", SimSlot.SIM_1, 20L * 1073741824,
            24L * 1073741824, BurnPace.ON_TRACK, "Steady usage", true, now)
        try {
            WidgetStatePreferences.saveState(context, id, success)
            assertEquals(success, WidgetStatePreferences.readState(getAppWidgetState(context, PreferencesGlanceStateDefinition, id)))
            WidgetStatePreferences.saveState(context, id, WidgetState.NoActivePromo)
            assertEquals(WidgetState.NoActivePromo, WidgetStatePreferences.readState(getAppWidgetState(context, PreferencesGlanceStateDefinition, id)))
            WidgetStatePreferences.saveState(context, id, WidgetState.PermissionRequired)
            assertEquals(WidgetState.PermissionRequired, WidgetStatePreferences.readState(getAppWidgetState(context, PreferencesGlanceStateDefinition, id)))
            WidgetStatePreferences.saveState(context, id, WidgetState.Error("Reading failed"))
            val failure = WidgetStatePreferences.readState(getAppWidgetState(context, PreferencesGlanceStateDefinition, id)) as WidgetState.Error
            assertEquals("Reading failed", failure.message)
            assertEquals(now, failure.lastSuccessfulAt)
            RefreshWidgetCallback().onAction(context, id, actionParametersOf())
            assertTrue(WorkManager.getInstance(context).getWorkInfosForUniqueWork("usage_sync_immediate").get().isNotEmpty())
        } finally {
            WidgetStatePreferences.saveStateAndNotify(context, original)
        }
    }
}
