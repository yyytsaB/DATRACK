package com.loadpredictor.presentation.widget
import com.loadpredictor.presentation.common.ForecastCopy
import com.loadpredictor.domain.model.BurnPace
import org.junit.Assert.*
import org.junit.Test
class ForecastCopyTest {
    @Test fun `long runway avoids a timestamp that truncates on a widget`() {
        assertEquals("About 3 months left", ForecastCopy.runway(BurnPace.ON_TRACK, 90L * 86400000, 0))
    }
    @Test fun `expired and calibrating do not claim a future runway`() {
        assertEquals("Validity ended", ForecastCopy.runway(BurnPace.EXPIRED, 90L * 86400000, 0))
        assertEquals("Estimate not ready", ForecastCopy.runway(BurnPace.INSUFFICIENT_DATA, 86400000, 0))
    }
    @Test fun `missing timestamp never claims a fresh reading`() {
        assertEquals("No reading yet", ForecastCopy.updated(0, 1000))
    }
}
