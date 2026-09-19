package com.planetfinder.app.data

import io.github.cosinekitty.astronomy.Body

data class CelestialBody(
    val id: String,
    val name: String,
    val englishName: String,
    val symbol: String,
    val body: Body,
    val description: String,
)

val celestialBodies = listOf(
    CelestialBody("sun", "太陽", "SUN", "☀", Body.Sun, "太陽系的中心恆星，也是地球光與熱的主要來源。"),
    CelestialBody("moon", "月亮", "MOON", "◐", Body.Moon, "地球唯一的天然衛星，月相隨日、地、月的位置持續變化。"),
    CelestialBody("mercury", "水星", "MERCURY", "☿", Body.Mercury, "最靠近太陽的行星，通常只能在晨昏短暫看見。"),
    CelestialBody("venus", "金星", "VENUS", "♀", Body.Venus, "夜空中最明亮的行星，常被稱為啟明星或長庚星。"),
    CelestialBody("mars", "火星", "MARS", "♂", Body.Mars, "富含氧化鐵的紅色行星，在夜空中呈現橘紅色。"),
    CelestialBody("jupiter", "木星", "JUPITER", "♃", Body.Jupiter, "太陽系最大的行星，明亮且容易以雙筒望遠鏡觀察衛星。"),
    CelestialBody("saturn", "土星", "SATURN", "♄", Body.Saturn, "以壯觀的冰粒環系聞名，是肉眼可見的遙遠行星。"),
    CelestialBody("uranus", "天王星", "URANUS", "⛢", Body.Uranus, "自轉軸近乎躺平的冰巨行星，觀察通常需要望遠鏡。"),
    CelestialBody("neptune", "海王星", "NEPTUNE", "♆", Body.Neptune, "太陽系最外側的主要行星，需要望遠鏡才能辨識。"),
)

data class SkyPosition(
    val body: CelestialBody,
    val azimuth: Double,
    val altitude: Double,
    val illumination: Double,
    val magnitude: Double?,
    val riseMillis: Long?,
    val setMillis: Long?,
    val currentRating: ObservabilityRating,
    val observationForecast: ObservationForecast,
) {
    val isAboveHorizon: Boolean get() = altitude >= 0.0
    val bestObservation: ObservationSample? get() = observationForecast.bestSample
}

data class ObservationSample(
    val timeMillis: Long,
    val azimuth: Double,
    val altitude: Double,
    val sunAltitude: Double,
    val moonAltitude: Double,
    val moonIllumination: Double,
    val sunSeparation: Double,
    val moonSeparation: Double,
    val rating: ObservabilityRating,
)

data class ObservationWindow(
    val startMillis: Long,
    val endMillis: Long,
)

data class ObservationForecast(
    val period: ObservationPeriod,
    val samples: List<ObservationSample>,
    val bestSample: ObservationSample?,
    val bestWindow: ObservationWindow?,
    val overallRating: ObservabilityRating,
)

enum class ObservationPeriod {
    DAYLIGHT,
    NIGHT,
}

enum class ObservabilityLevel(val displayName: String, val rank: Int) {
    UNAVAILABLE("不可見", 0),
    DIFFICULT("困難", 1),
    FAIR("普通", 2),
    RECOMMENDED("推薦", 3),
}

enum class TwilightPhase(val displayName: String) {
    DAYLIGHT("日光"),
    CIVIL("民用暮光"),
    NAUTICAL("航海暮光"),
    ASTRONOMICAL("天文暮光"),
    DARK("完全暗夜"),
}

data class ObservabilityRating(
    val level: ObservabilityLevel,
    val reasons: List<String>,
    val equipment: String,
    val score: Int,
)

enum class LocationSource(val displayName: String) {
    DEFAULT("預設位置"),
    DEVICE("裝置定位"),
    MANUAL("手動位置"),
}

data class ObserverLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double = 0.0,
    val label: String = "台北",
    val source: LocationSource = LocationSource.DEFAULT,
)
