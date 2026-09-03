package com.loadpredictor.presentation.widget

import androidx.datastore.preferences.core.Preferences
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.domain.model.SimSlot
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStatePreferencesTest {

    @Test
    fun readState_returnsNoActivePromo_whenTypeIsMissingOrUnrecognized() {
        val prefs = mockk<Preferences>()
        every { prefs[WidgetStatePreferences.KEY_STATE_TYPE] } returns null

        val result = WidgetStatePreferences.readState(prefs)
        assertEquals(WidgetState.NoActivePromo, result)
    }

    @Test
    fun readState_returnsPermissionRequired_whenTypeIsPermissionRequired() {
        val prefs = mockk<Preferences>()
        every { prefs[WidgetStatePreferences.KEY_STATE_TYPE] } returns WidgetStatePreferences.TYPE_PERMISSION_REQUIRED

        val result = WidgetStatePreferences.readState(prefs)
        assertEquals(WidgetState.PermissionRequired, result)
    }

    @Test
    fun readState_returnsError_whenTypeIsError() {
        val prefs = mockk<Preferences>()
        every { prefs[WidgetStatePreferences.KEY_STATE_TYPE] } returns WidgetStatePreferences.TYPE_ERROR
        every { prefs[WidgetStatePreferences.KEY_ERROR_MESSAGE] } returns "Network stats unavailable"

        val result = WidgetStatePreferences.readState(prefs)
        assertTrue(result is WidgetState.Error)
        assertEquals("Network stats unavailable", (result as WidgetState.Error).message)
    }

    @Test
    fun readState_returnsSuccess_withEstimatedDepletionTimestamp() {
        val now = 1700000000000L
        val depletionTime = 1705000000000L
        val prefs = mockk<Preferences>()
        every { prefs[WidgetStatePreferences.KEY_STATE_TYPE] } returns WidgetStatePreferences.TYPE_SUCCESS
        every { prefs[WidgetStatePreferences.KEY_PROMO_NAME] } returns "Smart Magic Data 399"
        every { prefs[WidgetStatePreferences.KEY_SIM_SLOT] } returns "SIM_2"
        every { prefs[WidgetStatePreferences.KEY_REMAINING_BYTES] } returns 15_000_000_000L
        every { prefs[WidgetStatePreferences.KEY_TOTAL_ALLOWANCE_BYTES] } returns 24_000_000_000L
        every { prefs[WidgetStatePreferences.KEY_PACE] } returns BurnPace.ON_TRACK.name
        every { prefs[WidgetStatePreferences.KEY_SUMMARY] } returns "On track to last"
        every { prefs[WidgetStatePreferences.KEY_IS_NO_EXPIRY] } returns true
        every { prefs[WidgetStatePreferences.KEY_LAST_UPDATED] } returns now
        every { prefs[WidgetStatePreferences.KEY_ESTIMATED_DEPLETION_TIMESTAMP] } returns depletionTime

        val result = WidgetStatePreferences.readState(prefs)
        assertTrue(result is WidgetState.Success)
        val success = result as WidgetState.Success
        assertEquals("Smart Magic Data 399", success.promoName)
        assertEquals(SimSlot.SIM_2, success.simSlot)
        assertEquals(15_000_000_000L, success.remainingBytes)
        assertEquals(24_000_000_000L, success.totalAllowanceBytes)
        assertEquals(BurnPace.ON_TRACK, success.pace)
        assertEquals("On track to last", success.plainLanguageSummary)
        assertEquals(true, success.isNoExpiry)
        assertEquals(now, success.lastUpdatedMillis)
        assertEquals(depletionTime, success.estimatedDepletionTimestamp)
    }

    @Test
    fun readState_returnsSuccess_withNullEstimatedDepletionTimestamp_whenZeroOrNegative() {
        val prefs = mockk<Preferences>()
        every { prefs[WidgetStatePreferences.KEY_STATE_TYPE] } returns WidgetStatePreferences.TYPE_SUCCESS
        every { prefs[WidgetStatePreferences.KEY_PROMO_NAME] } returns "Smart GigaSurf 99"
        every { prefs[WidgetStatePreferences.KEY_SIM_SLOT] } returns "SIM_1"
        every { prefs[WidgetStatePreferences.KEY_REMAINING_BYTES] } returns 1_000_000L
        every { prefs[WidgetStatePreferences.KEY_TOTAL_ALLOWANCE_BYTES] } returns 2_000_000L
        every { prefs[WidgetStatePreferences.KEY_PACE] } returns BurnPace.INSUFFICIENT_DATA.name
        every { prefs[WidgetStatePreferences.KEY_SUMMARY] } returns "Calibrating"
        every { prefs[WidgetStatePreferences.KEY_IS_NO_EXPIRY] } returns false
        every { prefs[WidgetStatePreferences.KEY_LAST_UPDATED] } returns 1700000000000L
        every { prefs[WidgetStatePreferences.KEY_ESTIMATED_DEPLETION_TIMESTAMP] } returns -1L

        val result = WidgetStatePreferences.readState(prefs)
        assertTrue(result is WidgetState.Success)
        val success = result as WidgetState.Success
        assertNull(success.estimatedDepletionTimestamp)
    }
}
