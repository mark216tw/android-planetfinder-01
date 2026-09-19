package com.planetfinder.app.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import com.planetfinder.app.data.ObserverLocation
import com.planetfinder.app.util.normalizeDegrees
import com.planetfinder.app.util.pitchToAimAltitude
import kotlin.math.roundToInt

enum class SensorAccuracy(val label: String) {
    UNAVAILABLE("裝置不支援方向感測器"),
    INITIALIZING("正在初始化感測器"),
    UNRELIABLE("精度不可靠，請以 8 字形校準"),
    LOW("精度偏低，請遠離金屬或磁鐵"),
    MEDIUM("感測器精度中等"),
    HIGH("感測器精度良好"),
}

class HeadingSensor(
    context: Context,
    private val onOrientation: (heading: Float, altitude: Float) -> Unit,
    private val onAccuracy: (SensorAccuracy) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.display else null
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private var gravity: FloatArray? = null
    private var magnetic: FloatArray? = null
    private var location: ObserverLocation? = null
    private var filteredHeading: Float? = null
    private var filteredAltitude: Float? = null
    private var accuracy = SensorAccuracy.INITIALIZING

    fun updateLocation(value: ObserverLocation) { location = value }

    fun start() {
        if (rotationVector == null && (accelerometer == null || magnetometer == null)) {
            updateAccuracy(SensorAccuracy.UNAVAILABLE)
            return
        }
        updateAccuracy(SensorAccuracy.INITIALIZING)
        if (rotationVector != null) {
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stop() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val matrix = FloatArray(9)
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> SensorManager.getRotationMatrixFromVector(matrix, event.values)
            Sensor.TYPE_ACCELEROMETER -> {
                gravity = event.values.clone()
                return calculateFallback(matrix)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetic = event.values.clone()
                return calculateFallback(matrix)
            }
        }
        publish(matrix)
    }

    private fun calculateFallback(matrix: FloatArray) {
        val g = gravity ?: return
        val m = magnetic ?: return
        if (SensorManager.getRotationMatrix(matrix, null, g, m)) publish(matrix)
    }

    private fun publish(source: FloatArray) {
        if (accuracy == SensorAccuracy.INITIALIZING) updateAccuracy(SensorAccuracy.MEDIUM)
        val adjusted = FloatArray(9)
        val displayRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
        val (axisX, axisY) = when (displayRotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(source, axisX, axisY, adjusted)
        val orientation = SensorManager.getOrientation(adjusted, FloatArray(3))
        val magneticDegrees = Math.toDegrees(orientation[0].toDouble())
        val aimAltitude = pitchToAimAltitude(orientation[1].toDouble()).toFloat()
        val declination = location?.let {
            GeomagneticField(it.latitude.toFloat(), it.longitude.toFloat(), it.altitudeMeters.toFloat(), System.currentTimeMillis()).declination
        } ?: 0f
        val trueHeading = normalizeDegrees(magneticDegrees + declination).toFloat()
        val previous = filteredHeading
        val smoothed = if (previous == null) trueHeading else {
            val delta = (((trueHeading - previous + 540f) % 360f) - 180f)
            normalizeDegrees((previous + delta * 0.18f).toDouble()).toFloat()
        }
        val previousAltitude = filteredAltitude
        val smoothedAltitude = previousAltitude?.let { it + (aimAltitude - it) * 0.18f } ?: aimAltitude
        if (
            previous == null || previousAltitude == null ||
            previous.roundToInt() != smoothed.roundToInt() ||
            previousAltitude.roundToInt() != smoothedAltitude.roundToInt()
        ) {
            filteredHeading = smoothed
            filteredAltitude = smoothedAltitude
            onOrientation(smoothed, smoothedAltitude)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type != Sensor.TYPE_ROTATION_VECTOR && sensor?.type != Sensor.TYPE_MAGNETIC_FIELD) return
        updateAccuracy(
            when (accuracy) {
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> SensorAccuracy.HIGH
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SensorAccuracy.MEDIUM
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> SensorAccuracy.LOW
                else -> SensorAccuracy.UNRELIABLE
            }
        )
    }

    private fun updateAccuracy(value: SensorAccuracy) {
        accuracy = value
        onAccuracy(value)
    }
}
