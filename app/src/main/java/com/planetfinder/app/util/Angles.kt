package com.planetfinder.app.util

import kotlin.math.abs

fun normalizeDegrees(value: Double): Double = ((value % 360.0) + 360.0) % 360.0

fun signedAngleDelta(target: Double, current: Double): Double =
    ((target - current + 540.0) % 360.0) - 180.0

fun relativeBearingDegrees(bearing: Double, heading: Double): Double =
    signedAngleDelta(bearing, heading)

fun pitchToAimAltitude(pitchRadians: Double): Double =
    Math.toDegrees(-pitchRadians).coerceIn(-90.0, 90.0)

fun cardinalDirection(degrees: Double): String {
    val labels = listOf("北", "東北", "東", "東南", "南", "西南", "西", "西北")
    return labels[((normalizeDegrees(degrees) + 22.5) / 45.0).toInt() % labels.size]
}

fun alignmentText(delta: Double): String = when {
    abs(delta) < 3.0 -> "方向正確"
    delta > 0.0 -> "向右轉 ${abs(delta).toInt()}°"
    else -> "向左轉 ${abs(delta).toInt()}°"
}

fun altitudeAlignmentText(delta: Double): String = when {
    abs(delta) < 3.0 -> "仰角正確"
    delta > 0.0 -> "再抬高 ${abs(delta).toInt()}°"
    else -> "再降低 ${abs(delta).toInt()}°"
}
