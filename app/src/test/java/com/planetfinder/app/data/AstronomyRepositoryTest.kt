package com.planetfinder.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import io.github.cosinekitty.astronomy.Body
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class AstronomyRepositoryTest {
    private val utc = ZoneId.of("UTC")

    @Test fun nightWindowStartsAtSixPmAndEndsAtSixAm() {
        val window = nightWindow(Instant.parse("2026-09-19T10:00:00Z").toEpochMilli(), utc)

        assertEquals(Instant.parse("2026-09-19T18:00:00Z").toEpochMilli(), window.first)
        assertEquals(Instant.parse("2026-09-20T06:00:00Z").toEpochMilli(), window.last)
    }

    @Test fun earlyMorningBelongsToPreviousNight() {
        val window = nightWindow(Instant.parse("2026-09-19T02:00:00Z").toEpochMilli(), utc)

        assertEquals(Instant.parse("2026-09-18T18:00:00Z").toEpochMilli(), window.first)
        assertEquals(Instant.parse("2026-09-19T06:00:00Z").toEpochMilli(), window.last)
    }

    @Test fun positionsCalculatesEverySupportedBody() {
        val positions = AstronomyRepository.positions(
            TaipeiLocation,
            Instant.parse("2026-09-19T12:00:00Z").toEpochMilli(),
        )

        assertEquals(9, positions.size)
        assertTrue(positions.all { it.azimuth in 0.0..360.0 })
        assertTrue(positions.all { it.altitude in -90.0..90.0 })
        assertTrue(positions.filter { it.body.body != Body.Sun }.all { it.observationForecast.samples.size == 37 })
    }

    @Test fun sunUsesLocalSunriseToSunsetInsteadOfNightWindow() {
        val taipeiZone = ZoneId.of("Asia/Taipei")
        val window = daylightWindow(
            TaipeiLocation,
            Instant.parse("2026-09-19T12:00:00Z").toEpochMilli(),
            taipeiZone,
        )

        assertNotNull(window)
        val sunrise = ZonedDateTime.ofInstant(Instant.ofEpochMilli(window!!.first), taipeiZone)
        val sunset = ZonedDateTime.ofInstant(Instant.ofEpochMilli(window.last), taipeiZone)
        assertEquals(sunrise.toLocalDate(), sunset.toLocalDate())
        assertTrue(sunrise.hour in 4..7)
        assertTrue(sunset.hour in 17..19)
    }

    @Test fun sunForecastIsDaylightAndOtherBodiesRemainNighttime() {
        val positions = AstronomyRepository.positions(
            TaipeiLocation,
            Instant.parse("2026-09-19T12:00:00Z").toEpochMilli(),
        )
        val sun = positions.first { it.body.body == Body.Sun }

        assertEquals(ObservationPeriod.DAYLIGHT, sun.observationForecast.period)
        assertTrue(sun.observationForecast.samples.size in 30..50)
        assertNotNull(sun.observationForecast.bestSample)
        assertNotNull(sun.observationForecast.bestWindow)
        assertTrue(positions.filter { it.body.body != Body.Sun }.all { it.observationForecast.period == ObservationPeriod.NIGHT })
    }

    @Test fun twilightPhasesUseStandardSolarAltitudes() {
        assertEquals(TwilightPhase.DAYLIGHT, twilightPhase(1.0))
        assertEquals(TwilightPhase.CIVIL, twilightPhase(-3.0))
        assertEquals(TwilightPhase.NAUTICAL, twilightPhase(-8.0))
        assertEquals(TwilightPhase.ASTRONOMICAL, twilightPhase(-15.0))
        assertEquals(TwilightPhase.DARK, twilightPhase(-20.0))
    }

    @Test fun belowHorizonAndNearSunAreUnavailable() {
        assertEquals(ObservabilityLevel.UNAVAILABLE, rating(altitude = -1.0).level)
        assertEquals(ObservabilityLevel.UNAVAILABLE, rating(sunSeparation = 4.0).level)
    }

    @Test fun sunRequiresProtectionWhenAboveHorizon() {
        val rating = evaluateObservability(Body.Sun, 45.0, 45.0, -26.74, 0.0, -10.0, 0.0, 90.0)

        assertEquals(ObservabilityLevel.DIFFICULT, rating.level)
        assertEquals("僅可使用合格太陽濾鏡", rating.equipment)
    }

    @Test fun highDarkBrightTargetIsRecommended() {
        val rating = rating(altitude = 55.0, sunAltitude = -20.0, magnitude = -2.0, sunSeparation = 90.0)

        assertEquals(ObservabilityLevel.RECOMMENDED, rating.level)
        assertTrue("高仰角，較少大氣干擾" in rating.reasons)
    }

    @Test fun brightNearbyMoonReducesScore() {
        val clear = rating(moonAltitude = -5.0)
        val moonlit = rating(moonAltitude = 40.0, moonIllumination = 0.9, moonSeparation = 15.0)

        assertTrue(moonlit.score < clear.score)
        assertTrue("明亮月光干擾" in moonlit.reasons)
    }

    @Test fun bestWindowChoosesLongestRecommendedRun() {
        val samples = listOf(
            sample(0, ObservabilityLevel.DIFFICULT),
            sample(1, ObservabilityLevel.RECOMMENDED),
            sample(2, ObservabilityLevel.RECOMMENDED),
            sample(3, ObservabilityLevel.RECOMMENDED),
            sample(4, ObservabilityLevel.DIFFICULT),
        )

        val window = bestObservationWindow(samples)
        assertNotNull(window)
        assertEquals(1_200_000L, window!!.startMillis)
        assertEquals(4_800_000L, window.endMillis)
    }

    private fun rating(
        altitude: Double = 45.0,
        sunAltitude: Double = -20.0,
        magnitude: Double = 1.0,
        sunSeparation: Double = 90.0,
        moonAltitude: Double = -5.0,
        moonIllumination: Double = 0.0,
        moonSeparation: Double = 120.0,
    ) = evaluateObservability(
        Body.Jupiter, altitude, sunAltitude, magnitude, sunSeparation,
        moonAltitude, moonIllumination, moonSeparation,
    )

    private fun sample(index: Int, level: ObservabilityLevel): ObservationSample {
        val rating = ObservabilityRating(level, emptyList(), "肉眼", level.rank * 25)
        return ObservationSample(
            timeMillis = index * 1_200_000L,
            azimuth = 180.0,
            altitude = 30.0,
            sunAltitude = -20.0,
            moonAltitude = -5.0,
            moonIllumination = 0.0,
            sunSeparation = 90.0,
            moonSeparation = 90.0,
            rating = rating,
        )
    }
}
