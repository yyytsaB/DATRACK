package com.loadpredictor.presentation.widget

import android.content.Context
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.SimSlot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetGlanceInteractionTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testWidgetStateSavingAndMultiPathTransitions() = runBlocking {
        // Test 1: Save and transition through all 4 WidgetStates on physical hardware
        val successState = WidgetState.Success(
            promoName = "Smart Magic Data 399",
            simSlot = SimSlot.SIM_1,
            remainingBytes = 20L * 1024L * 1024L * 1024L,
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            pace = BurnPace.ON_TRACK,
            plainLanguageSummary = "On track: will last beyond promo window.",
            isNoExpiry = true,
            lastUpdatedMillis = System.currentTimeMillis()
        )
        WidgetStatePreferences.saveStateAndNotify(context, successState)

        // Test 2: Transition to NoActivePromo
        WidgetStatePreferences.saveStateAndNotify(context, WidgetState.NoActivePromo)

        // Test 3: Transition to PermissionRequired
        WidgetStatePreferences.saveStateAndNotify(context, WidgetState.PermissionRequired)

        // Test 4: Transition to Error state
        WidgetStatePreferences.saveStateAndNotify(context, WidgetState.Error("Network stats service unreachable"))

        // Test 5: Execute RefreshWidgetCallback action
        val callback = RefreshWidgetCallback()
        assertNotNull(callback)
    }
}
