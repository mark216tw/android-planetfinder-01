package com.planetfinder.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AnglesTest {
    @Test fun normalizeWrapsBothDirections() {
        assertEquals(350.0, normalizeDegrees(-10.0), 0.0001)
        assertEquals(10.0, normalizeDegrees(370.0), 0.0001)
        assertEquals(0.0, normalizeDegrees(720.0), 0.0001)
    }

    @Test fun signedDeltaChoosesShortestTurn() {
        assertEquals(20.0, signedAngleDelta(10.0, 350.0), 0.0001)
        assertEquals(-20.0, signedAngleDelta(350.0, 10.0), 0.0001)
        assertEquals(-180.0, signedAngleDelta(180.0, 0.0), 0.0001)
    }

    @Test fun cardinalDirectionUsesEightSectors() {
        assertEquals("北", cardinalDirection(359.0))
        assertEquals("東北", cardinalDirection(45.0))
        assertEquals("南", cardinalDirection(181.0))
    }

    @Test fun pitchConvertsToPhoneTopAltitude() {
        assertEquals(0.0, pitchToAimAltitude(0.0), 0.0001)
        assertEquals(45.0, pitchToAimAltitude(-Math.PI / 4), 0.0001)
        assertEquals(90.0, pitchToAimAltitude(-Math.PI / 2), 0.0001)
    }

    @Test fun altitudeGuidanceUsesShortestVerticalMove() {
        assertEquals("仰角正確", altitudeAlignmentText(2.9))
        assertEquals("再抬高 12°", altitudeAlignmentText(12.8))
        assertEquals("再降低 7°", altitudeAlignmentText(-7.2))
    }
}
