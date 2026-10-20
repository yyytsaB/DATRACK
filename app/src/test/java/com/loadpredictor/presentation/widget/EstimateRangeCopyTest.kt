package com.loadpredictor.presentation.widget
import com.loadpredictor.presentation.common.EstimateRangeCopy
import org.junit.Assert.*
import org.junit.Test
class EstimateRangeCopyTest {
    @Test fun asymmetricBoundsAreShownWithoutSymmetricConfidenceClaim() {
        assertEquals("Estimated range: 2–5 days",EstimateRangeCopy.label(2*86400000L,4*86400000L+1,0))
        assertNull(EstimateRangeCopy.label(10,9,0))
        assertNull(EstimateRangeCopy.label(null,100,0))
        assertNull(EstimateRangeCopy.label(10,100,20))
    }
}
