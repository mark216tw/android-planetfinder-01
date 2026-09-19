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
import java.time.Instant
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

private const val SAMPLE_INTERVAL_MILLIS = 20 * 60 * 1000L

object AstronomyRepository {
    private data class ObservationCacheKey(
        val body: Body,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val periodStart: Long,
        val periodEnd: Long,
    )

    private val observationCache = LinkedHashMap<ObservationCacheKey, ObservationForecast>()

    fun positions(location: ObserverLocation, millis: Long = System.currentTimeMillis()): List<SkyPosition> {
        val time = Time.fromMillisecondsSince1970(millis)
        val observer = Observer(location.latitude, location.longitude, location.altitudeMeters)
        val sunState = bodyState(Body.Sun, time, observer)
        val moonState = bodyState(Body.Moon, time, observer)
        val moonLight = illumination(Body.Moon, time)

        return celestialBodies.map { item ->
            val state = if (item.body == Body.Sun) sunState else if (item.body == Body.Moon) moonState else bodyState(item.body, time, observer)
            val light = if (item.body == Body.Sun) null else illumination(item.body, time)
            val magnitude = light?.mag ?: -26.74
            val rating = evaluateObservability(
                body = item.body,
                altitude = state.altitude,
                sunAltitude = sunState.altitude,
                magnitude = magnitude,
                sunSeparation = angularSeparation(state.ra, state.dec, sunState.ra, sunState.dec),
                moonAltitude = moonState.altitude,
                moonIllumination = moonLight.phaseFraction,
                moonSeparation = angularSeparation(state.ra, state.dec, moonState.ra, moonState.dec),
            )
            SkyPosition(
                body = item,
                azimuth = state.azimuth,
                altitude = state.altitude,
                illumination = light?.phaseFraction ?: 1.0,
                magnitude = magnitude,
                riseMillis = searchRiseSet(item.body, observer, Direction.Rise, time, 2.0)?.toMillisecondsSince1970(),
                setMillis = searchRiseSet(item.body, observer, Direction.Set, time, 2.0)?.toMillisecondsSince1970(),
                currentRating = rating,
                observationForecast = observationForecast(
                    item.body,
                    observer,
                    if (item.body == Body.Sun) daylightWindow(location, millis) else nightWindow(millis),
                ),
            )
        }
    }

    private fun observationForecast(body: Body, observer: Observer, window: LongRange?): ObservationForecast {
        val period = if (body == Body.Sun) ObservationPeriod.DAYLIGHT else ObservationPeriod.NIGHT
        if (window == null) {
            val reason = if (body == Body.Sun) "當日太陽不會升起" else "沒有可用的觀測時段"
            return ObservationForecast(period, emptyList(), null, null, unavailableRating(reason, equipmentFor(body)))
        }
        val key = ObservationCacheKey(body, observer.latitude, observer.longitude, observer.height, window.first, window.last)
        synchronized(observationCache) { observationCache[key]?.let { return it } }

        val samples = buildList {
            var sampleMillis = window.first
            while (sampleMillis < window.last) {
                add(observationSample(body, observer, sampleMillis))
                sampleMillis += SAMPLE_INTERVAL_MILLIS
            }
            if (isEmpty() || last().timeMillis != window.last) {
                add(observationSample(body, observer, window.last))
            }
        }
        val visible = samples.filter { it.rating.level != ObservabilityLevel.UNAVAILABLE }
        val best = visible.maxWithOrNull(compareBy<ObservationSample> { it.rating.level.rank }.thenBy { it.rating.score }.thenBy { it.altitude })
        val unavailableReason = if (body == Body.Sun) "當日太陽不在地平線上" else "今晚都在地平線下"
        val overall = best?.rating ?: unavailableRating(unavailableReason, equipmentFor(body))
        val result = ObservationForecast(
            period,
            samples,
            best,
            bestObservationWindow(samples, if (body == Body.Sun) ObservabilityLevel.DIFFICULT else null),
            overall,
        )

        synchronized(observationCache) {
            if (observationCache.size >= 36) observationCache.remove(observationCache.keys.first())
            observationCache[key] = result
        }
        return result
    }

    private fun observationSample(body: Body, observer: Observer, millis: Long): ObservationSample {
        val time = Time.fromMillisecondsSince1970(millis)
        val target = bodyState(body, time, observer)
        val sun = bodyState(Body.Sun, time, observer)
        val moon = bodyState(Body.Moon, time, observer)
        val moonLight = illumination(Body.Moon, time)
        val magnitude = if (body == Body.Sun) -26.74 else illumination(body, time).mag
        val sunSeparation = angularSeparation(target.ra, target.dec, sun.ra, sun.dec)
        val moonSeparation = angularSeparation(target.ra, target.dec, moon.ra, moon.dec)
        return ObservationSample(
            timeMillis = millis,
            azimuth = target.azimuth,
            altitude = target.altitude,
            sunAltitude = sun.altitude,
            moonAltitude = moon.altitude,
            moonIllumination = moonLight.phaseFraction,
            sunSeparation = sunSeparation,
            moonSeparation = moonSeparation,
            rating = evaluateObservability(
                body, target.altitude, sun.altitude, magnitude, sunSeparation,
                moon.altitude, moonLight.phaseFraction, moonSeparation,
            ),
        )
    }

    private data class BodyState(val ra: Double, val dec: Double, val azimuth: Double, val altitude: Double)

    private fun bodyState(body: Body, time: Time, observer: Observer): BodyState {
        val equatorial = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val horizontal = horizon(time, observer, equatorial.ra, equatorial.dec, Refraction.Normal)
        return BodyState(equatorial.ra, equatorial.dec, horizontal.azimuth, horizontal.altitude)
    }
}

fun twilightPhase(sunAltitude: Double): TwilightPhase = when {
    sunAltitude > 0.0 -> TwilightPhase.DAYLIGHT
    sunAltitude > -6.0 -> TwilightPhase.CIVIL
    sunAltitude > -12.0 -> TwilightPhase.NAUTICAL
    sunAltitude > -18.0 -> TwilightPhase.ASTRONOMICAL
    else -> TwilightPhase.DARK
}

fun evaluateObservability(
    body: Body,
    altitude: Double,
    sunAltitude: Double,
    magnitude: Double,
    sunSeparation: Double,
    moonAltitude: Double,
    moonIllumination: Double,
    moonSeparation: Double,
): ObservabilityRating {
    val equipment = equipmentFor(body)
    if (altitude < 0.0) return unavailableRating("位於地平線下", equipment)
    if (body != Body.Sun && sunSeparation < 5.0) return unavailableRating("距離太陽過近", equipment)
    if (body == Body.Sun) {
        return ObservabilityRating(
            ObservabilityLevel.DIFFICULT,
            listOf("太陽位於地平線上", "切勿直接觀看太陽"),
            equipment,
            30,
        )
    }

    var score = 40
    val positive = mutableListOf<Pair<Int, String>>()
    val negative = mutableListOf<Pair<Int, String>>()
    when {
        altitude >= 45.0 -> { score += 30; positive += 30 to "高仰角，較少大氣干擾" }
        altitude >= 25.0 -> { score += 20; positive += 20 to "仰角良好" }
        altitude >= 10.0 -> { score += 5; negative += 8 to "仰角偏低" }
        else -> { score -= 20; negative += 20 to "接近地平線" }
    }
    when (twilightPhase(sunAltitude)) {
        TwilightPhase.DARK -> { score += 20; positive += 20 to "天空已完全變暗" }
        TwilightPhase.ASTRONOMICAL -> { score += 12; positive += 12 to "天文暮光，天空接近全暗" }
        TwilightPhase.NAUTICAL -> { score -= 4; negative += 8 to "航海暮光干擾" }
        TwilightPhase.CIVIL -> { score -= 18; negative += 18 to "民用暮光較明亮" }
        TwilightPhase.DAYLIGHT -> { score -= 35; negative += 35 to "日光干擾" }
    }
    when {
        magnitude <= 0.0 -> { score += 15; positive += 15 to "星體非常明亮" }
        magnitude <= 4.0 -> { score += 8; positive += 8 to "亮度適合肉眼" }
        magnitude <= 6.0 -> { score -= 5; negative += 5 to "需要良好暗空" }
        else -> { score -= 15; negative += 15 to "需要望遠鏡" }
    }
    when {
        sunSeparation < 15.0 -> { score -= 35; negative += 35 to "非常接近太陽" }
        sunSeparation < 30.0 -> { score -= 16; negative += 16 to "晨昏才較容易觀測" }
        else -> positive += 4 to "遠離太陽眩光"
    }
    if (body != Body.Moon && moonAltitude > 0.0 && moonIllumination > 0.5) {
        when {
            moonSeparation < 30.0 -> { score -= 18; negative += 18 to "明亮月光干擾" }
            moonSeparation < 60.0 -> { score -= 8; negative += 8 to "有些月光干擾" }
        }
    }

    val clamped = score.coerceIn(0, 100)
    val level = when {
        clamped >= 75 -> ObservabilityLevel.RECOMMENDED
        clamped >= 50 -> ObservabilityLevel.FAIR
        clamped >= 15 -> ObservabilityLevel.DIFFICULT
        else -> ObservabilityLevel.UNAVAILABLE
    }
    val strongestPositive = positive.maxByOrNull { it.first }
    val strongestNegative = negative.maxByOrNull { it.first }
    val reasons = if (level.rank >= ObservabilityLevel.FAIR.rank) {
        listOfNotNull(strongestPositive, strongestNegative)
    } else {
        listOfNotNull(strongestNegative, strongestPositive)
    }.map { it.second }.distinct().take(2)
    return ObservabilityRating(level, reasons, equipment, clamped)
}

fun bestObservationWindow(
    samples: List<ObservationSample>,
    minimumLevel: ObservabilityLevel? = null,
): ObservationWindow? {
    val threshold = minimumLevel?.rank ?: if (samples.any { it.rating.level == ObservabilityLevel.RECOMMENDED }) {
        ObservabilityLevel.RECOMMENDED.rank
    } else ObservabilityLevel.FAIR.rank
    var bestStart = -1
    var bestEnd = -1
    var currentStart = -1
    samples.forEachIndexed { index, sample ->
        if (sample.rating.level.rank >= threshold) {
            if (currentStart == -1) currentStart = index
            if (bestStart == -1 || index - currentStart > bestEnd - bestStart) {
                bestStart = currentStart
                bestEnd = index
            }
        } else {
            currentStart = -1
        }
    }
    if (bestStart == -1) return null
    return ObservationWindow(
        samples[bestStart].timeMillis,
        (samples[bestEnd].timeMillis + SAMPLE_INTERVAL_MILLIS).coerceAtMost(samples.last().timeMillis),
    )
}

private fun equipmentFor(body: Body): String = when (body) {
    Body.Sun -> "僅可使用合格太陽濾鏡"
    Body.Moon -> "肉眼或雙筒望遠鏡"
    Body.Uranus, Body.Neptune -> "建議使用天文望遠鏡"
    else -> "肉眼可見，望遠鏡可觀察更多細節"
}

private fun unavailableRating(reason: String, equipment: String) =
    ObservabilityRating(ObservabilityLevel.UNAVAILABLE, listOf(reason), equipment, 0)

private fun angularSeparation(ra1Hours: Double, dec1Degrees: Double, ra2Hours: Double, dec2Degrees: Double): Double {
    val ra1 = Math.toRadians(ra1Hours * 15.0)
    val ra2 = Math.toRadians(ra2Hours * 15.0)
    val dec1 = Math.toRadians(dec1Degrees)
    val dec2 = Math.toRadians(dec2Degrees)
    val cosine = (sin(dec1) * sin(dec2) + cos(dec1) * cos(dec2) * cos(ra1 - ra2)).coerceIn(-1.0, 1.0)
    return Math.toDegrees(acos(cosine))
}

fun nightWindow(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LongRange {
    val current = Instant.ofEpochMilli(millis).atZone(zoneId)
    val nightDate = if (current.hour < 6) current.toLocalDate().minusDays(1) else current.toLocalDate()
    val start = nightDate.atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli()
    val end = nightDate.plusDays(1).atTime(6, 0).atZone(zoneId).toInstant().toEpochMilli()
    return start..end
}

fun daylightWindow(
    location: ObserverLocation,
    millis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): LongRange? {
    val date = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
    val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val startOfNextDay = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val observer = Observer(location.latitude, location.longitude, location.altitudeMeters)
    val searchStart = Time.fromMillisecondsSince1970(startOfDay)
    val rise = searchRiseSet(Body.Sun, observer, Direction.Rise, searchStart, 1.0)?.toMillisecondsSince1970()
    val set = searchRiseSet(Body.Sun, observer, Direction.Set, searchStart, 1.0)?.toMillisecondsSince1970()
    if (rise != null && set != null && rise < set && rise in startOfDay until startOfNextDay) {
        return rise..set.coerceAtMost(startOfNextDay)
    }

    fun sunAltitude(atMillis: Long): Double {
        val time = Time.fromMillisecondsSince1970(atMillis)
        val equatorial = equator(Body.Sun, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        return horizon(time, observer, equatorial.ra, equatorial.dec, Refraction.Normal).altitude
    }
    val noon = startOfDay + (startOfNextDay - startOfDay) / 2
    return if (sunAltitude(noon) >= 0.0) startOfDay..startOfNextDay else null
}
