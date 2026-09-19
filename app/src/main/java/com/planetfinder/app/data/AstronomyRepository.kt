package com.planetfinder.app.data

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.searchRiseSet

object AstronomyRepository {
    fun positions(location: ObserverLocation, millis: Long = System.currentTimeMillis()): List<SkyPosition> {
        val time = Time.fromMillisecondsSince1970(millis)
        val observer = Observer(location.latitude, location.longitude, location.altitudeMeters)
        return celestialBodies.map { item ->
            val equatorial = equator(item.body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
            val horizontal = horizon(time, observer, equatorial.ra, equatorial.dec, Refraction.Normal)
            val light = if (item.body == Body.Sun) null else illumination(item.body, time)
            SkyPosition(
                body = item,
                azimuth = horizontal.azimuth,
                altitude = horizontal.altitude,
                illumination = light?.phaseFraction ?: 1.0,
                magnitude = light?.mag ?: -26.74,
                riseMillis = searchRiseSet(item.body, observer, Direction.Rise, time, 2.0)?.toMillisecondsSince1970(),
                setMillis = searchRiseSet(item.body, observer, Direction.Set, time, 2.0)?.toMillisecondsSince1970(),
            )
        }
    }
}
